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

    // Settings
    suspend fun getSettings(): SourceLockSettings
    suspend fun updateSettings(settings: SourceLockSettings)
    fun observeSettings(): Flow<SourceLockSettings>
}