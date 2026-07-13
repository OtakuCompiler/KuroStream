package com.kurostream.domain.usecase

import com.kurostream.core.platform.platformCurrentTimeMillis
import com.kurostream.domain.model.MediaItem
import com.kurostream.domain.model.MediaSource
import com.kurostream.domain.model.SourceLock
import com.kurostream.domain.model.SourceLockFallback
import com.kurostream.domain.model.SourceLockResult
import com.kurostream.domain.model.SourceLockSettings
import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.repository.SourceLockRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class SourceLockUseCases(
    private val sourceLockRepository: SourceLockRepository,
    private val mediaRepository: MediaRepository,
) {
    suspend fun resolveSource(
        mediaItem: MediaItem,
        availableSources: List<MediaSource>
    ): SourceLockResult {
        val settings = sourceLockRepository.getSettings()

        if (!settings.enabled) {
            return SourceLockResult.Fallback(mediaItem, "Source lock disabled")
        }

        val seriesId = extractSeriesId(mediaItem)
        val existingLock = sourceLockRepository.getLock(seriesId)

        if (existingLock != null && existingLock.isActive) {
            val matchingSource = availableSources.find { it.providerId == existingLock.providerId }
            if (matchingSource != null) {
                val updatedLock = existingLock.copy(
                    lastUsedAt = currentTimeMillis(),
                    episodeCount = existingLock.episodeCount + 1
                )
                sourceLockRepository.updateLock(updatedLock)
                return SourceLockResult.Locked(mediaItem, updatedLock)
            } else {
                return handleFallback(
                    mediaItem,
                    seriesId,
                    availableSources,
                    settings,
                    "Locked source ${existingLock.providerId} not available"
                )
            }
        }

        if (availableSources.isNotEmpty() && settings.enabled) {
            val bestSource = selectBestSource(availableSources)
            val newLock = SourceLock(
                seriesId = seriesId,
                providerId = bestSource.providerId,
                sourceQuality = bestSource.quality,
                sourceUrl = bestSource.url,
                lockedAt = currentTimeMillis(),
                lastUsedAt = currentTimeMillis(),
                episodeCount = 1,
                isActive = true
            )
            sourceLockRepository.setLock(newLock)
            return SourceLockResult.Locked(mediaItem, newLock)
        }

        return SourceLockResult.Fallback(mediaItem, "No sources available")
    }

    private fun extractSeriesId(mediaItem: MediaItem): String {
        return mediaItem.id.split("_").firstOrNull() ?: mediaItem.id
    }

    private fun selectBestSource(sources: List<MediaSource>): MediaSource {
        val qualityPriority = mapOf(
            "4K" to 4,
            "2160p" to 4,
            "1080p" to 3,
            "720p" to 2,
            "480p" to 1
        )
        return sources.maxByOrNull { qualityPriority[it.quality] ?: 0 } ?: sources.first()
    }

    private suspend fun handleFallback(
        mediaItem: MediaItem,
        seriesId: String,
        availableSources: List<MediaSource>,
        settings: SourceLockSettings,
        reason: String
    ): SourceLockResult {
        var retries = 0
        while (retries < settings.maxRetries && availableSources.isNotEmpty()) {
            delay(settings.retryDelayMs)
            retries++

            val existingLock = sourceLockRepository.getLock(seriesId)
            if (existingLock != null) {
                val matchingSource = availableSources.find { it.providerId == existingLock.providerId }
                if (matchingSource != null) {
                    val updatedLock = existingLock.copy(
                        lastUsedAt = currentTimeMillis(),
                        episodeCount = existingLock.episodeCount + 1
                    )
                    sourceLockRepository.updateLock(updatedLock)
                    return SourceLockResult.Locked(mediaItem, updatedLock)
                }
            }
        }

        return when (settings.fallbackMode) {
            SourceLockFallback.AUTOMATIC -> {
                if (availableSources.isNotEmpty()) {
                    val fallbackSource = selectBestSource(availableSources)
                    val fallbackLock = SourceLock(
                        seriesId = seriesId,
                        providerId = fallbackSource.providerId,
                        sourceQuality = fallbackSource.quality,
                        sourceUrl = fallbackSource.url,
                        lockedAt = currentTimeMillis(),
                        lastUsedAt = currentTimeMillis(),
                        episodeCount = 1,
                        isActive = true
                    )
                    sourceLockRepository.setLock(fallbackLock)
                    if (settings.notifyOnFallback) {
                        SourceLockResult.Fallback(mediaItem, "Auto-fallback to ${fallbackSource.providerName}")
                    } else {
                        SourceLockResult.Locked(mediaItem, fallbackLock)
                    }
                } else {
                    SourceLockResult.Error("No sources available after retries")
                }
            }
            SourceLockFallback.MANUAL -> {
                SourceLockResult.ManualPickerRequired(seriesId, availableSources)
            }
        }
    }

    suspend fun clearLockForSeries(seriesId: String) {
        sourceLockRepository.clearLock(seriesId)
    }

    suspend fun clearAllLocks() {
        sourceLockRepository.clearAllLocks()
    }

    fun observeSettings(): Flow<SourceLockSettings> = sourceLockRepository.observeSettings()

    suspend fun updateSettings(settings: SourceLockSettings) {
        sourceLockRepository.updateSettings(settings)
    }
}

private var _timeOverride: (() -> Long)? = null

internal fun setTimeOverride(override: () -> Long) {
    _timeOverride = override
}

internal fun resetTimeOverride() {
    _timeOverride = null
}

internal fun currentTimeMillis(): Long {
    return _timeOverride?.invoke() ?: platformCurrentTimeMillis()
}