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

import com.kurostream.domain.model.SourceLockSettings
import kotlinx.coroutines.flow.Flow

interface SourceLockRepository {
    data class Settings(
        val enabled: Boolean = true,
        val persistAcrossSessions: Boolean = true,
        val fallbackMode: com.kurostream.domain.model.SourceLockFallback = com.kurostream.domain.model.SourceLockFallback.AUTOMATIC,
        val maxRetries: Int = 2,
        val retryDelayMs: Long = 3000,
        val notifyOnFallback: Boolean = true,
    )

    suspend fun getSettings(): Settings
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setPersistAcrossSessions(enabled: Boolean)
    suspend fun setFallbackMode(mode: com.kurostream.domain.model.SourceLockFallback)
    fun observeSettings(): Flow<Settings>
    suspend fun clearAllLocks()
}