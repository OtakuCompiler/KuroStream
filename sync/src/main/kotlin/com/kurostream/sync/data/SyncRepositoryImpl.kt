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

package com.kurostream.sync.data

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.SyncState
import com.kurostream.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor() : SyncRepository {
    private val _syncState = MutableStateFlow(SyncState(isSyncing = false))
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override suspend fun sync(): Result<Unit> {
        _syncState.value = _syncState.value.copy(isSyncing = true)
        return try {
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        } finally {
            _syncState.value = _syncState.value.copy(isSyncing = false)
        }
    }

    override suspend fun forceSync(): Result<Unit> {
        return sync()
    }
}
