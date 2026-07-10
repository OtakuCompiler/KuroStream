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

package com.kurostream.data.anistream.sync

import android.content.Intent

interface SyncProvider {
    val providerId: String
    val providerName: String
    val iconRes: Int

    suspend fun isAuthenticated(): Boolean
    fun getAuthRequest(): Intent
    suspend fun handleAuthResponse(intent: android.content.Intent): Result<SyncToken>
    suspend fun syncWatchHistory(historyItems: List<WatchHistoryItem>): SyncResult
    suspend fun fetchRemoteHistory(): List<WatchHistoryItem>
    suspend fun logout()
}

data class SyncToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAt
}

sealed class SyncResult {
    data class Success(val synced: Int, val failed: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

data class WatchHistoryItem(
    val animeId: String,
    val malId: Int? = null,
    val anilistId: Int? = null,
    val title: String,
    val episodesWatched: Int = 0,
    val status: WatchStatus = WatchStatus.WATCHING,
    val userRating: Int? = null,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

enum class WatchStatus {
    WATCHING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_WATCH
}
