package com.kurostream.data.repository

import com.kurostream.data.model.WatchHistoryEntry
import com.kurostream.data.source.local.HistoryDao
import com.kurostream.data.source.local.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun getRecentHistory(): Flow<List<WatchHistoryEntry>> =
        historyDao.getRecentHistory().map { it.map { e -> e.toModel() } }

    suspend fun recordWatch(
        contentId: String,
        contentTitle: String,
        poster: String?
    ) {
        val existing = historyDao.getByContentId(contentId)
        if (existing != null) {
            historyDao.updateProgress(contentId, existing.progressMs, System.currentTimeMillis())
        } else {
            historyDao.insert(
                HistoryEntity(
                    id = UUID.randomUUID().toString(),
                    contentId = contentId,
                    contentTitle = contentTitle,
                    poster = poster,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateProgress(contentId: String, progressMs: Long) {
        historyDao.updateProgress(contentId, progressMs, System.currentTimeMillis())
    }

    private fun HistoryEntity.toModel() = WatchHistoryEntry(
        id = id,
        contentId = contentId,
        contentTitle = contentTitle,
        poster = poster,
        watchedAt = watchedAt,
        progressMs = progressMs,
        durationMs = durationMs
    )
}
