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
import android.net.Uri
import com.kurostream.data.anistream.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistSyncProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val anilistApi: AnilistApiService
) : SyncProvider {

    companion object {
        const val PROVIDER_ID = "anilist"
        const val AUTH_URL = "https://anilist.co/api/v2/oauth/authorize"
        const val TOKEN_URL = "https://anilist.co/api/v2/oauth/token"
        const val CLIENT_ID = "your_anilist_client_id"
        const val REDIRECT_URI = "anistream://anilist/callback"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override val providerId: String = PROVIDER_ID
    override val providerName: String = "AniList"
    override val iconRes: Int = com.kurostream.legacyui.anistream.R.drawable.ic_anilist

    override suspend fun isAuthenticated(): Boolean {
        return getStoredToken() != null
    }

    override fun getAuthRequest(): Intent {
        val authUri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .build()

        return Intent(Intent.ACTION_VIEW, authUri)
    }

    override suspend fun handleAuthResponse(intent: Intent): Result<SyncToken> {
        val uri = intent.data ?: return Result.failure(Exception("No auth data"))
        val code = uri.getQueryParameter("code")
            ?: return Result.failure(Exception("No authorization code"))

        return exchangeCodeForToken(code)
    }

    private suspend fun exchangeCodeForToken(code: String): Result<SyncToken> {
        return withContext(Dispatchers.IO) {
            try {
                val token = anilistApi.exchangeToken(code, CLIENT_ID, REDIRECT_URI)
                val syncToken = SyncToken(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000)
                )
                storeToken(syncToken)
                Result.success(syncToken)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncWatchHistory(historyItems: List<WatchHistoryItem>): SyncResult {
        if (!isAuthenticated()) return SyncResult.Error("Not authenticated")

        return withContext(Dispatchers.IO) {
            try {
                val token = getStoredToken()?.accessToken ?: return@withContext SyncResult.Error("No token")
                var synced = 0
                var failed = 0

                historyItems.forEach { item ->
                    try {
                        val anilistId = item.anilistId ?: resolveAnilistId(item.animeId)
                        if (anilistId != null) {
                            anilistApi.saveMediaListEntry(
                                token = token,
                                mediaId = anilistId,
                                status = mapStatus(item.status),
                                progress = item.episodesWatched,
                                score = item.userRating?.toDouble() ?: 0.0
                            )
                            synced++
                        } else {
                            failed++
                        }
                    } catch (e: Exception) {
                        failed++
                    }
                }

                SyncResult.Success(synced = synced, failed = failed)
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Sync failed")
            }
        }
    }

    override suspend fun fetchRemoteHistory(): List<WatchHistoryItem> {
        val token = getStoredToken()?.accessToken ?: return emptyList()
        return anilistApi.getUserAnimeList(token).map { entry ->
            WatchHistoryItem(
                animeId = entry.mediaId.toString(),
                anilistId = entry.mediaId,
                title = entry.title,
                episodesWatched = entry.progress ?: 0,
                status = mapAnilistStatus(entry.status),
                userRating = entry.score?.toInt()
            )
        }
    }

    private fun mapStatus(status: WatchStatus): String = when (status) {
        WatchStatus.WATCHING -> "CURRENT"
        WatchStatus.COMPLETED -> "COMPLETED"
        WatchStatus.ON_HOLD -> "PAUSED"
        WatchStatus.DROPPED -> "DROPPED"
        WatchStatus.PLAN_TO_WATCH -> "PLANNING"
    }

    private fun mapAnilistStatus(status: String?): WatchStatus = when (status) {
        "CURRENT" -> WatchStatus.WATCHING
        "COMPLETED" -> WatchStatus.COMPLETED
        "PAUSED" -> WatchStatus.ON_HOLD
        "DROPPED" -> WatchStatus.DROPPED
        else -> WatchStatus.PLAN_TO_WATCH
    }

    private suspend fun resolveAnilistId(animeId: String): Int? {
        // Map internal ID to AniList ID
        return null
    }

    private suspend fun getStoredToken(): SyncToken? {
        val tokenJson = settingsRepository.getSetting("anilist_token") ?: return null
        return json.decodeFromString(SyncToken.serializer(), tokenJson)
    }

    private suspend fun storeToken(token: SyncToken) {
        val tokenJson = json.encodeToString(SyncToken.serializer(), token)
        settingsRepository.setSetting("anilist_token", tokenJson)
    }

    override suspend fun logout() {
        settingsRepository.setSetting("anilist_token", "")
    }
}

// AniList API Service interface
interface AnilistApiService {
    suspend fun exchangeToken(code: String, clientId: String, redirectUri: String): AnilistTokenResponse
    suspend fun saveMediaListEntry(token: String, mediaId: Int, status: String, progress: Int, score: Double)
    suspend fun getUserAnimeList(token: String): List<AnilistListEntry>
}

data class AnilistTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val refreshToken: String? = null
)

data class AnilistListEntry(
    val mediaId: Int,
    val title: String,
    val status: String? = null,
    val progress: Int? = null,
    val score: Double? = null
)
