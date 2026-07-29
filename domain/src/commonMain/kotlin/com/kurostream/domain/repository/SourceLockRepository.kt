package com.kurostream.domain.repository

import com.kurostream.domain.model.SourceLock
import com.kurostream.domain.model.SourceLockSettings
import kotlinx.coroutines.flow.Flow

interface SourceLockRepository {
    suspend fun getLock(seriesId: String): SourceLock?
    fun observeLock(seriesId: String): Flow<SourceLock?>
    suspend fun setLock(lock: SourceLock)
    suspend fun updateLock(lock: SourceLock)
    suspend fun clearLock(seriesId: String)
    suspend fun clearAllLocks()
    fun observeAllActive(): Flow<List<SourceLock>>
    suspend fun getSettings(): SourceLockSettings
    suspend fun updateSettings(settings: SourceLockSettings)
    fun observeSettings(): Flow<SourceLockSettings>
}
