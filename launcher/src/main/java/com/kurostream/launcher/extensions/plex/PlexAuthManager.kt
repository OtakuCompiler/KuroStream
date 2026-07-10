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

package com.kurostream.launcher.extensions.plex

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlexAuthManager @Inject constructor(
    private val prefs: SharedPreferences,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val PREF_PLEX_TOKEN = "plex_access_token"
        private const val PREF_PLEX_SERVER_URL = "plex_server_url"
        private const val PLEX_TV_URL = "https://plex.tv/api/v2/"
        private const val CLIENT_ID = "streambox-android"
        private const val CLIENT_NAME = "StreamBox"
    }

    private var apiService: PlexApiService? = null
    private var serverApiService: PlexServerApiService? = null

    val isAuthenticated: Boolean
        get() = !getAccessToken().isNullOrBlank()

    fun getAccessToken(): String? = prefs.getString(PREF_PLEX_TOKEN, null)
    fun getServerUrl(): String? = prefs.getString(PREF_PLEX_SERVER_URL, null)

    suspend fun authenticateWithPin(): Result<PlexPinResponse> = withContext(Dispatchers.IO) {
        try {
            val service = getPlexTvService()
            val response = service.createPin(
                strong = true,
                clientId = CLIENT_ID
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Pin creation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPinStatus(pinId: String): Result<PlexPinStatus> = withContext(Dispatchers.IO) {
        try {
            val service = getPlexTvService()
            val response = service.checkPin(pinId, CLIENT_ID)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.authToken != null) {
                    prefs.edit().putString(PREF_PLEX_TOKEN, body.authToken).apply()
                    Result.success(PlexPinStatus.Authenticated(body.authToken))
                } else {
                    Result.success(PlexPinStatus.Pending)
                }
            } else {
                Result.failure(Exception("Check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverServers(): Result<List<PlexServer>> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            val service = getPlexTvService()
            val response = service.getResources(token, CLIENT_ID, includeHttps = true)
            if (response.isSuccessful) {
                val resources = response.body() ?: emptyList()
                val servers = resources.filter { it.provides?.contains("server") == true }
                    .map { resource ->
                        val connection = resource.connections?.firstOrNull { it.local == false }
                            ?: resource.connections?.firstOrNull()
                        PlexServer(
                            name = resource.name,
                            clientIdentifier = resource.clientIdentifier,
                            address = connection?.uri ?: "",
                            accessToken = resource.accessToken ?: token
                        )
                    }
                Result.success(servers)
            } else {
                Result.failure(Exception("Discovery failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun selectServer(server: PlexServer) {
        prefs.edit().putString(PREF_PLEX_SERVER_URL, server.address).apply()
        serverApiService = null
    }

    fun logout() {
        apiService = null
        serverApiService = null
        prefs.edit()
            .remove(PREF_PLEX_TOKEN)
            .remove(PREF_PLEX_SERVER_URL)
            .apply()
    }

    private fun getPlexTvService(): PlexApiService {
        if (apiService == null) {
            apiService = Retrofit.Builder()
                .baseUrl(PLEX_TV_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlexApiService::class.java)
        }
        return apiService!!
    }

    fun getServerService(): PlexServerApiService? {
        val serverUrl = getServerUrl()?.trimEnd('/') + "/"
            ?: return null
        val token = getAccessToken() ?: return null

        if (serverApiService == null) {
            val authClient = okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-Plex-Token", token)
                        .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                        .addHeader("X-Plex-Product", CLIENT_NAME)
                        .build()
                    chain.proceed(request)
                }
                .build()

            serverApiService = Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlexServerApiService::class.java)
        }
        return serverApiService
    }
}

data class PlexPinResponse(
    val id: String,
    val code: String,
    val product: String,
    val clientIdentifier: String
)

sealed class PlexPinStatus {
    object Pending : PlexPinStatus()
    data class Authenticated(val token: String) : PlexPinStatus()
}

data class PlexServer(
    val name: String,
    val clientIdentifier: String,
    val address: String,
    val accessToken: String
)
