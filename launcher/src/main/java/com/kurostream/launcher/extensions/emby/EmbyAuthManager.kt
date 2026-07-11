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

package com.kurostream.launcher.extensions.emby

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
class EmbyAuthManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val prefs: SharedPreferences,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val PREF_EMBY_SERVER = "emby_server_url"
        private const val PREF_EMBY_TOKEN = "emby_access_token"
        private const val PREF_EMBY_USER_ID = "emby_user_id"
        private const val CLIENT_NAME = "StreamBox"
        private const val CLIENT_VERSION = "1.0.0"
        private const val DEVICE_ID = "android-streambox"
    }

    private var apiService: EmbyApiService? = null

    val isAuthenticated: Boolean
        get() = !getAccessToken().isNullOrBlank()

    fun getServerUrl(): String? = prefs.getString(PREF_EMBY_SERVER, null)
    fun getAccessToken(): String? = prefs.getString(PREF_EMBY_TOKEN, null)
    fun getUserId(): String? = prefs.getString(PREF_EMBY_USER_ID, null)

    suspend fun authenticate(serverUrl: String, username: String, password: String): Result<EmbyAuthResult> =
        withContext(Dispatchers.IO) {
            try {
                val sha1Hash = password.toByteArray().let { bytes ->
                    java.security.MessageDigest.getInstance("SHA-1").digest(bytes)
                }.joinToString("") { "%02x".format(it) }

                val retrofit = Retrofit.Builder()
                    .baseUrl(serverUrl.trimEnd('/') + "/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(EmbyApiService::class.java)
                apiService = service

                val authRequest = EmbyAuthRequest(
                    Username = username,
                    Pw = sha1Hash
                )

                val response = service.authenticateByName(authRequest)

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        prefs.edit()
                            .putString(PREF_EMBY_SERVER, serverUrl)
                            .putString(PREF_EMBY_TOKEN, it.AccessToken)
                            .putString(PREF_EMBY_USER_ID, it.User?.Id)
                            .apply()

                        Result.success(
                            EmbyAuthResult(
                                accessToken = it.AccessToken,
                                userId = it.User?.Id ?: "",
                                serverUrl = serverUrl
                            )
                        )
                    } ?: Result.failure(Exception("Empty response"))
                } else {
                    Result.failure(Exception("Auth failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun logout() {
        apiService = null
        prefs.edit()
            .remove(PREF_EMBY_SERVER)
            .remove(PREF_EMBY_TOKEN)
            .remove(PREF_EMBY_USER_ID)
            .apply()
    }

    fun getAuthenticatedService(): EmbyApiService? {
        val token = getAccessToken() ?: return null
        val server = getServerUrl() ?: return null

        if (apiService == null) {
            val authClient = okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-Emby-Token", token)
                        .addHeader("X-Emby-Authorization",
                            "MediaBrowser Client="$CLIENT_NAME", Device="$DEVICE_ID", DeviceId="$DEVICE_ID", Version="$CLIENT_VERSION"")
                        .build()
                    chain.proceed(request)
                }
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(server.trimEnd('/') + "/")
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EmbyApiService::class.java)
        }
        return apiService
    }
}

data class EmbyAuthResult(
    val accessToken: String,
    val userId: String,
    val serverUrl: String
)
