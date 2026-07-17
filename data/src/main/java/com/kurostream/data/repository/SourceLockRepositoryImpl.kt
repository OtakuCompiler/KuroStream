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

package com.kurostream.data.repository

import com.kurostream.core.common.result.Result
import com.kurostream.data.local.dao.SourceLockDao
import com.kurostream.data.local.entity.SourceLockEntity
import com.kurostream.data.local.entity.SourceLockSettingsEntity
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.domain.model.SourceLock
import com.kurostream.domain.model.SourceLockFallback
import com.kurostream.domain.model.SourceLockSettings
import com.kurostream.domain.repository.SourceLockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceLockRepositoryImpl @Inject constructor(
    private val sourceLockDao: SourceLockDao,
    private val settingsDataStore: SettingsDataStore,
) : SourceLockRepository {

    private val _settings = MutableStateFlow<SourceLockSettings?>(null)
    val settings: Flow<SourceLockSettings> = _settings
        .asStateFlow()
        .filterNotNull()

    override suspend fun getLock(seriesId: String): SourceLock? {
        return withContext(Dispatchers.IO) {
            sourceLockDao.getBySeriesId(seriesId)?.toDomain()
        }
    }

    override fun observeLock(seriesId: String): Flow<SourceLock?> {
        return sourceLockDao.observeBySeriesId(seriesId)
            .map { it?.toDomain() }
            .distinctUntilChanged()
    }

    override suspend fun setLock(lock: SourceLock) {
        withContext(Dispatchers.IO) {
            val entity = SourceLockEntity(
                seriesId = lock.seriesId,
                providerId = lock.providerId,
                sourceQuality = lock.sourceQuality,
                sourceUrl = lock.sourceUrl,
                createdAt = lock.lockedAt,
                lastUsedAt = lock.lastUsedAt,
                episodeCount = lock.episodeCount,
                fallbackCount = 0,
                isActive = lock.isActive,
            )
            sourceLockDao.insert(entity)
        }
    }

    override suspend fun updateLock(lock: SourceLock) {
        withContext(Dispatchers.IO) {
            val entity = SourceLockEntity(
                seriesId = lock.seriesId,
                providerId = lock.providerId,
                sourceQuality = lock.sourceQuality,
                sourceUrl = lock.sourceUrl,
                createdAt = lock.lockedAt,
                lastUsedAt = lock.lastUsedAt,
                episodeCount = lock.episodeCount,
                fallbackCount = 0,
                isActive = lock.isActive,
            )
            sourceLockDao.update(entity)
        }
    }

    override suspend fun clearLock(seriesId: String) {
        withContext(Dispatchers.IO) {
            sourceLockDao.deleteBySeriesId(seriesId)
        }
    }

    override suspend fun clearAllLocks() {
        withContext(Dispatchers.IO) {
            sourceLockDao.deleteAll()
        }
    }

    override fun observeAllActive(): Flow<List<SourceLock>> {
        return sourceLockDao.observeAllActive()
            .map { list -> list.map { it.toDomain() } }
            .distinctUntilChanged()
    }

    // Settings
    override suspend fun getSettings(): SourceLockSettings {
        return withContext(Dispatchers.IO) {
            val entity = sourceLockDao.getSettings() ?: SourceLockSettingsEntity()
            _settings.value = entity.toDomain()
            entity.toDomain()
        }
    }

    override suspend fun updateSettings(settings: SourceLockSettings) {
        withContext(Dispatchers.IO) {
            val entity = SourceLockSettingsEntity(
                enabled = settings.enabled,
                fallbackModeOrdinal = settings.fallbackMode.ordinal,
                maxRetries = settings.maxRetries,
                retryDelayMs = settings.retryDelayMs,
                persistAcrossSessions = settings.persistAcrossSessions,
                notifyOnFallback = settings.notifyOnFallback,
            )
            sourceLockDao.insertSettings(entity)
            _settings.value = settings
        }
    }

    override fun observeSettings(): Flow<SourceLockSettings> {
        return settingsDataStore.data
            .map { prefs ->
                SourceLockSettings(
                    enabled = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_ENABLED] ?: true,
                    fallbackMode = SourceLockFallback.values()[prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_FALLBACK_MODE] ?: 0],
                    maxRetries = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_MAX_RETRIES] ?: 2,
                    retryDelayMs = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_RETRY_DELAY_MS] ?: 3000,
                    persistAcrossSessions = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_PERSIST] ?: true,
                    notifyOnFallback = prefs[com.kurostream.data.local.preferences.SettingsDataStore.Keys.SOURCE_LOCK_NOTIFY_FALLBACK] ?: true,
                )
            }
    }
}

fun SourceLockEntity.toDomain(): SourceLock = SourceLock(
    seriesId = seriesId,
    providerId = providerId,
    sourceQuality = sourceQuality,
    sourceUrl = sourceUrl,
    lockedAt = createdAt,
    lastUsedAt = lastUsedAt,
    episodeCount = episodeCount,
    isActive = isActive,
)

fun SourceLockSettingsEntity.toDomain(): SourceLockSettings = SourceLockSettings(
    enabled = enabled,
    fallbackMode = SourceLockFallback.values()[fallbackModeOrdinal],
    maxRetries = maxRetries,
    retryDelayMs = retryDelayMs,
    persistAcrossSessions = persistAcrossSessions,
    notifyOnFallback = notifyOnFallback,
)