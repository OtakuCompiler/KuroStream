// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.domain.usecase

import com.kurostream.domain.model.MediaItem
import com.kurostream.domain.model.MediaSource
import com.kurostream.domain.model.SourceLock
import com.kurostream.domain.model.SourceLockFallback
import com.kurostream.domain.model.SourceLockResult
import com.kurostream.domain.model.SourceLockSettings
import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.repository.SourceLockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceLockUseCases @Inject constructor(
    private val sourceLockRepository: SourceLockRepository,
    private val mediaRepository: MediaRepository,
) {

    /**
     * Resolve the source for a given media item based on source lock settings
     */
    suspend fun resolveSource(
        mediaItem: MediaItem,
        availableSources: List<MediaSource>
    ): SourceLockResult = withContext(Dispatchers.IO) {
        val settings = sourceLockRepository.getSettings()
        
        if (!settings.enabled) {
            return@withContext SourceLockResult.Fallback(mediaItem, "Source lock disabled")
        }

        val seriesId = extractSeriesId(mediaItem)
        val existingLock = sourceLockRepository.getLock(seriesId)
        
        if (existingLock != null && existingLock.isActive) {
            // Find matching source
            val matchingSource = availableSources.find { it.providerId == existingLock.providerId }
            if (matchingSource != null) {
                // Update lock usage stats
                val updatedLock = existingLock.copy(
                    lastUsedAt = System.currentTimeMillis(),
                    episodeCount = existingLock.episodeCount + 1
                )
                sourceLockRepository.updateLock(updatedLock)
                return@withContext SourceLockResult.Locked(mediaItem, updatedLock)
            } else {
                // Locked source not available - handle fallback
                return@withContext handleFallback(
                    mediaItem, 
                    seriesId, 
                    availableSources, 
                    settings,
                    "Locked source ${existingLock.providerId} not available"
                )
            }
        }

        // No existing lock - if we have sources and source lock is enabled, create a lock for the best source
        if (availableSources.isNotEmpty() && settings.enabled) {
            val bestSource = selectBestSource(availableSources)
            val newLock = SourceLock(
                seriesId = seriesId,
                providerId = bestSource.providerId,
                sourceQuality = bestSource.quality,
                sourceUrl = bestSource.url,
                lockedAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                episodeCount = 1,
                isActive = true
            )
            sourceLockRepository.setLock(newLock)
            return@withContext SourceLockResult.Locked(mediaItem, newLock)
        }

        SourceLockResult.Fallback(mediaItem, "No sources available")
    }

    private fun extractSeriesId(mediaItem: MediaItem): String {
        // Extract series ID from media item - could be show ID or season ID
        return mediaItem.id.split("_").firstOrNull() ?: mediaItem.id
    }

    private fun selectBestSource(sources: List<MediaSource>): MediaSource {
        // Priority: 4K > 1080p > 720p > other
        val qualityPriority = mapOf(
            "4K" to 4,
            "2160p" to 4,
            "1080p" to 3,
            "720p" to 2,
            "480p" to 1
        )
        return sources.maxByOrNull { qualityPriority[it.quality] ?: 0 } ?: sources.first()
    }

    private fun handleFallback(
        mediaItem: MediaItem,
        seriesId: String,
        availableSources: List<MediaSource>,
        settings: SourceLockSettings,
        reason: String
    ): SourceLockResult {
        var retries = 0
        while (retries < settings.maxRetries && availableSources.isNotEmpty()) {
            // Wait before retry
            Thread.sleep(settings.retryDelayMs)
            retries++
            
            // Re-check if locked source becomes available
            val existingLock = sourceLockRepository.getLock(seriesId)
            if (existingLock != null) {
                val matchingSource = availableSources.find { it.providerId == existingLock.providerId }
                if (matchingSource != null) {
                    val updatedLock = existingLock.copy(
                        lastUsedAt = System.currentTimeMillis(),
                        episodeCount = existingLock.episodeCount + 1
                    )
                    sourceLockRepository.updateLock(updatedLock)
                    return SourceLockResult.Locked(mediaItem, updatedLock)
                }
            }
        }

        // All retries exhausted - apply fallback strategy
        return when (settings.fallbackMode) {
            SourceLockFallback.AUTOMATIC -> {
                if (availableSources.isNotEmpty()) {
                    val fallbackSource = selectBestSource(availableSources)
                    val fallbackLock = SourceLock(
                        seriesId = seriesId,
                        providerId = fallbackSource.providerId,
                        sourceQuality = fallbackSource.quality,
                        sourceUrl = fallbackSource.url,
                        lockedAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
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