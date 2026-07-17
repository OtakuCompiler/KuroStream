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

package com.kurostream.domain.sync

import com.kurostream.core.common.result.Result
import com.kurostream.core.platform.platformCurrentTimeMillis
import com.kurostream.domain.model.*

interface SyncProvider {
    val providerName: String
    val isAuthenticated: Boolean
    suspend fun authenticate(credentials: Map<String, String>): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun push(data: SyncPayload): Result<SyncTimestamp>
    suspend fun pull(lastSyncTimestamp: Long?): Result<SyncPayload?>
    suspend fun resolveConflicts(local: SyncPayload, remote: SyncPayload): SyncPayload
    suspend fun deleteCloudData(): Result<Unit>
}

data class SyncPayload(
    val profiles: List<Profile> = emptyList(),
    val watchHistory: List<WatchHistory> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val downloads: List<DownloadItem> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val timestamp: Long = platformCurrentTimeMillis(),
    val deviceId: String = "",
    val version: Int = 1
)

data class SyncTimestamp(val timestamp: Long, val serverTime: Long)
