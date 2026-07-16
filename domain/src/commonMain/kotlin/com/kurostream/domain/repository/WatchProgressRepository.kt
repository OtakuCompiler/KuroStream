package com.kurostream.domain.repository

interface WatchProgressRepository {
    suspend fun getProgress(mediaId: String): Float?
    suspend fun saveProgress(mediaId: String, position: Long, duration: Long)
}
