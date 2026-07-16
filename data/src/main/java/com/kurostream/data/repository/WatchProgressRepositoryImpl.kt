package com.kurostream.data.repository

import com.kurostream.domain.repository.WatchProgressRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepositoryImpl @Inject constructor() : WatchProgressRepository {
    override suspend fun getProgress(mediaId: String): Float? = null
    override suspend fun saveProgress(mediaId: String, position: Long, duration: Long) {}
}
