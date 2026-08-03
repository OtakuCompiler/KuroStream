package com.kurostream.data.repository

import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
) : WatchProgressRepository {
    // Default profile ID for local-only progress tracking
    private val defaultProfileId = "local"

    override suspend fun getProgress(mediaId: String): Float? {
        val history = watchHistoryDao.getByMediaAndProfile(mediaId, defaultProfileId)
        return history?.let { it.position.toFloat() / it.duration.coerceAtLeast(1) }
    }

    override suspend fun saveProgress(mediaId: String, position: Long, duration: Long) {
        val existing = watchHistoryDao.getByMediaAndProfile(mediaId, defaultProfileId)
        val entity = existing?.copy(position = position, duration = duration, watchedAt = System.currentTimeMillis())
            ?: com.kurostream.data.local.entity.WatchHistoryEntity(
                id = "${defaultProfileId}_${mediaId}",
                mediaItemId = mediaId,
                profileId = defaultProfileId,
                position = position,
                duration = duration,
                watchedAt = System.currentTimeMillis(),
            )
        watchHistoryDao.insert(entity)
    }

    override fun observeAllProgress(): Flow<Map<String, Long>> {
        return watchHistoryDao.observeByProfile(defaultProfileId).map { list ->
            list.associate { it.mediaItemId to it.position }
        }
    }

    override suspend fun syncPending() {
        Timber.d("WatchProgressRepositoryImpl: syncPending (no-op)")
    }
}
