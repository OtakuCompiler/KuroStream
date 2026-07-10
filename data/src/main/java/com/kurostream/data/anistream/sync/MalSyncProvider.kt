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

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kurostream.data.anistream.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.openid.appauth.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MalSyncProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val malApi: MalApiService
) : SyncProvider {

    companion object {
        const val PROVIDER_ID = "myanimelist"
        const val AUTH_ENDPOINT = "https://myanimelist.net/v1/oauth2/authorize"
        const val TOKEN_ENDPOINT = "https://myanimelist.net/v1/oauth2/token"
        const val CLIENT_ID = "your_mal_client_id"
        const val REDIRECT_URI = "anistream://mal/callback"
    }

    private var authService: AuthorizationService? = null
    private val json = Json { ignoreUnknownKeys = true }

    override val providerId: String = PROVIDER_ID
    override val providerName: String = "MyAnimeList"
    override val iconRes: Int = com.kurostream.legacyui.anistream.R.drawable.ic_mal

    override suspend fun isAuthenticated(): Boolean {
        val token = getStoredToken()
        return token != null && !token.isExpired
    }

    override fun getAuthRequest(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScope("write:users")
            .build()

        authService = AuthorizationService(context)
        return authService!!.getAuthorizationRequestIntent(authRequest)
    }

    override suspend fun handleAuthResponse(intent: Intent): Result<SyncToken> {
        val authorizationResponse = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        if (error != null) {
            return Result.failure(Exception("Auth error: ${error.errorDescription}"))
        }

        authorizationResponse?.let { response ->
            return exchangeCodeForToken(response)
        }

        return Result.failure(Exception("No authorization response"))
    }

    private suspend fun exchangeCodeForToken(response: AuthorizationResponse): Result<SyncToken> {
        return withContext(Dispatchers.IO) {
            try {
                val tokenRequest = response.createTokenExchangeRequest()
                val tokenResponse = authService?.performTokenRequest(tokenRequest)

                tokenResponse?.let { token ->
                    val syncToken = SyncToken(
                        accessToken = token.accessToken ?: "",
                        refreshToken = token.refreshToken,
                        expiresAt = token.accessTokenExpirationTime ?: System.currentTimeMillis() + 3600000
                    )
                    storeToken(syncToken)
                    Result.success(syncToken)
                } ?: Result.failure(Exception("Token exchange failed"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncWatchHistory(historyItems: List<WatchHistoryItem>): SyncResult {
        if (!isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return withContext(Dispatchers.IO) {
            try {
                var synced = 0
                var failed = 0

                historyItems.forEach { item ->
                    try {
                        val malId = item.malId ?: resolveMalId(item.animeId)
                        if (malId != null) {
                            malApi.updateList(
                                accessToken = getStoredToken()?.accessToken ?: "",
                                malId = malId,
                                status = mapStatus(item.status),
                                numWatchedEpisodes = item.episodesWatched,
                                score = item.userRating
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
        return malApi.getUserAnimeList(token).data.map { entry ->
            WatchHistoryItem(
                animeId = entry.node.id.toString(),
                malId = entry.node.id,
                title = entry.node.title,
                episodesWatched = entry.listStatus?.numEpisodesWatched ?: 0,
                status = mapMalStatus(entry.listStatus?.status),
                userRating = entry.listStatus?.score
            )
        }
    }

    private suspend fun resolveMalId(animeId: String): Int? {
        // Map internal anime ID to MAL ID via database or API
        return null
    }

    private fun mapStatus(status: WatchStatus): String = when (status) {
        WatchStatus.WATCHING -> "watching"
        WatchStatus.COMPLETED -> "completed"
        WatchStatus.ON_HOLD -> "on_hold"
        WatchStatus.DROPPED -> "dropped"
        WatchStatus.PLAN_TO_WATCH -> "plan_to_watch"
    }

    private fun mapMalStatus(status: String?): WatchStatus = when (status) {
        "watching" -> WatchStatus.WATCHING
        "completed" -> WatchStatus.COMPLETED
        "on_hold" -> WatchStatus.ON_HOLD
        "dropped" -> WatchStatus.DROPPED
        else -> WatchStatus.PLAN_TO_WATCH
    }

    private suspend fun getStoredToken(): SyncToken? {
        val tokenJson = settingsRepository.getSetting("mal_token") ?: return null
        return json.decodeFromString(SyncToken.serializer(), tokenJson)
    }

    private suspend fun storeToken(token: SyncToken) {
        val tokenJson = json.encodeToString(SyncToken.serializer(), token)
        settingsRepository.setSetting("mal_token", tokenJson)
    }

    override suspend fun logout() {
        settingsRepository.setSetting("mal_token", "")
        authService?.dispose()
        authService = null
    }
}

// MAL API Service interface
interface MalApiService {
    suspend fun updateList(
        accessToken: String,
        malId: Int,
        status: String,
        numWatchedEpisodes: Int,
        score: Int?
    )

    suspend fun getUserAnimeList(accessToken: String): MalAnimeListResponse
}

data class MalAnimeListResponse(
    val data: List<MalListEntry>
)

data class MalListEntry(
    val node: MalNode,
    val listStatus: MalListStatus? = null
)

data class MalNode(
    val id: Int,
    val title: String
)

data class MalListStatus(
    val status: String? = null,
    val score: Int? = null,
    val numEpisodesWatched: Int? = null
)
