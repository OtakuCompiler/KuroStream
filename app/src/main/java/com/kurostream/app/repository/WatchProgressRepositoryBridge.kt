package com.kurostream.app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepositoryBridge @Inject constructor(
    private val domainRepo: com.kurostream.domain.repository.WatchProgressRepository,
) : TvRepositories.WatchProgressRepository {

    private val _watchProgress = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun getWatchProgress(): Flow<Map<String, Long>> {
        return domainRepo.observeAllProgress()
    }

    override suspend fun updateProgress(itemId: String, progress: Long) {
        try {
            domainRepo.saveProgress(itemId, progress, duration = 0L)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update progress for $itemId")
        }
    }

    override suspend fun saveProgress(mediaId: String, episodeId: String?, positionMs: Long, durationMs: Long) {
        try {
            domainRepo.saveProgress(mediaId, positionMs, durationMs)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save progress for $mediaId")
        }
    }
}
