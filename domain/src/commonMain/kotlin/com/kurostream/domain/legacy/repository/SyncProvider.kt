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

package com.kurostream.domain.legacy.repository

import com.kurostream.common.result.Result
import com.kurostream.domain.entity.PlaybackState
import com.kurostream.domain.entity.SyncState
import kotlinx.coroutines.flow.Flow

interface SyncProvider {
    val isAuthenticated: Flow<Boolean>
    suspend fun authenticate(credentials: SyncCredentials): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun pushPlaybackStates(states: List<PlaybackState>): Result<SyncState>
    suspend fun pullPlaybackStates(profileId: String): Result<List<PlaybackState>>
    fun observeSyncState(profileId: String): Flow<SyncState>
    suspend fun syncNow(profileId: String): Result<SyncState>
}

sealed class SyncCredentials {
    data class Token(val token: String) : SyncCredentials()
    data class Password(val username: String, val password: String) : SyncCredentials()
    data class OAuth(val code: String, val redirectUri: String) : SyncCredentials()
}
