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

package com.kurostream.launcher.extensions.jellyfin

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val PREF_JELLYFIN_SERVER = "jellyfin_server_url"
        private const val PREF_JELLYFIN_TOKEN = "jellyfin_access_token"
        private const val PREF_JELLYFIN_USER_ID = "jellyfin_user_id"
        private const val CLIENT_NAME = "StreamBox"
        private const val CLIENT_VERSION = "1.0.0"
        private const val DEVICE_ID = "android-streambox"
    }

    private var apiService: JellyfinApiService? = null
    private var authToken: String? = null

    val isAuthenticated: Boolean
        get() = !getAccessToken().isNullOrBlank()

    fun getServerUrl(): String? = prefs.getString(PREF_JELLYFIN_SERVER, null)

    fun getAccessToken(): String? = prefs.getString(PREF_JELLYFIN_TOKEN, null)

    fun getUserId(): String? = prefs.getString(PREF_JELLYFIN_USER_ID, null)

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<JellyfinAuthResult> =
        withContext(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(serverUrl.trimEnd('/') + "/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(JellyfinApiService::class.java)
                apiService = service

                val authRequest = JellyfinAuthRequest(
                    Username = username,
                    Pw = password
                )

                val response = service.authenticateByName(authRequest)

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        authToken = it.AccessToken
                        prefs.edit()
                            .putString(PREF_JELLYFIN_SERVER, serverUrl)
                            .putString(PREF_JELLYFIN_TOKEN, it.AccessToken)
                            .putString(PREF_JELLYFIN_USER_ID, it.User?.Id)
                            .apply()

                        Result.success(
                            JellyfinAuthResult(
                                accessToken = it.AccessToken,
                                userId = it.User?.Id ?: "",
                                serverUrl = serverUrl
                            )
                        )
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Authentication failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun logout() {
        authToken = null
        apiService = null
        prefs.edit()
            .remove(PREF_JELLYFIN_SERVER)
            .remove(PREF_JELLYFIN_TOKEN)
            .remove(PREF_JELLYFIN_USER_ID)
            .apply()
    }

    fun getAuthenticatedService(): JellyfinApiService? {
        val token = getAccessToken() ?: return null
        val server = getServerUrl() ?: return null

        if (apiService == null) {
            val authClient = okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-Emby-Token", token)
                        .addHeader("X-Emby-Authorization", 
                            "MediaBrowser Client=\"${CLIENT_NAME}\", Device=\"${DEVICE_ID}\", DeviceId=\"${DEVICE_ID}\", Version=\"${CLIENT_VERSION}\"")
                        .build()
                    chain.proceed(request)
                }
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(server.trimEnd('/') + "/")
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(JellyfinApiService::class.java)
        }
        return apiService
    }
}

data class JellyfinAuthResult(
    val accessToken: String,
    val userId: String,
    val serverUrl: String
)
