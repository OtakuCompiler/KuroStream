package com.kurostream.domain.repository

import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    suspend fun getProgress(mediaId: String): Float?
    suspend fun saveProgress(mediaId: String, position: Long, duration: Long = 0L)
    suspend fun syncPending()
    fun observeAllProgress(): Flow<Map<String, Long>>
}
