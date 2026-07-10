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

package com.kurostream.app.repository

import com.kurostream.common.result.Result
import com.kurostream.domain.model.SourceLockFallback
import com.kurostream.domain.model.SourceLockSettings
import com.kurostream.domain.repository.SourceLockRepository as CanonicalSourceLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceLockRepositoryImpl @Inject constructor(
    private val canonicalRepository: CanonicalSourceLockRepository
) : SourceLockRepository {

    private val _settings = mutableStateFlow<Settings>(Settings())
    val settings: kotlinx.coroutines.flow.Flow<Settings> = _settings.asStateFlow()

    override suspend fun getSettings(): Settings {
        return canonicalRepository.getSettings().fold(
            onSuccess = { settings ->
                Settings(
                    enabled = settings.enabled,
                    persistAcrossSessions = settings.persistAcrossSessions,
                    fallbackMode = settings.fallbackMode,
                    maxRetries = settings.maxRetries,
                    retryDelayMs = settings.retryDelayMs,
                    notifyOnFallback = settings.notifyOnFallback,
                )
            },
            onFailure = { Settings() }
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        canonicalRepository.getSettings().first().fold(
            onSuccess = { current ->
                val updated = current.copy(enabled = enabled)
                canonicalRepository.updateSettings(updated)
                refresh()
            },
            onFailure = { }
        )
    }

    override suspend fun setPersistAcrossSessions(enabled: Boolean) {
        canonicalRepository.getSettings().first().fold(
            onSuccess = { current ->
                val updated = current.copy(persistAcrossSessions = enabled)
                canonicalRepository.updateSettings(updated)
                refresh()
            },
            onFailure = { }
        )
    }

    override suspend fun setFallbackMode(mode: com.kurostream.domain.model.SourceLockFallback) {
        canonicalRepository.getSettings().first().fold(
            onSuccess = { current ->
                val updated = current.copy(fallbackMode = mode)
                canonicalRepository.updateSettings(updated)
                refresh()
            },
            onFailure = { }
        )
    }

    override fun observeSettings(): Flow<Settings> = _settings

    override suspend fun clearAllLocks() {
        canonicalRepository.clearAllLocks()
    }

    private fun refresh() {
        getSettings().also { _settings.value = it }
    }
}