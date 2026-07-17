package com.kurostream.backup.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kurostream.backup.domain.GitHubApiService
import com.kurostream.backup.domain.GitHubAuthState
import com.kurostream.core.common.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GitHubApiService,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            "github_auth", masterKey, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadAuthState(): GitHubAuthState {
        val token = encryptedPrefs.getString("access_token", null)
        val username = encryptedPrefs.getString("username", null)
        val userId = encryptedPrefs.getLong("user_id", -1L)
        val avatarUrl = encryptedPrefs.getString("avatar_url", null)
        val expiresAt = encryptedPrefs.getLong("expires_at", -1L)

        return if (token != null && username != null) {
            GitHubAuthState(
                isAuthenticated = true, accessToken = token, username = username,
                userId = if (userId > 0) userId else null,
                avatarUrl = avatarUrl, expiresAt = if (expiresAt > 0) expiresAt else null,
            )
        } else GitHubAuthState()
    }

    suspend fun authenticate(deviceCode: String, interval: Int, onAuthState: (GitHubAuthState) -> Unit): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = pollForToken(deviceCode, interval)
            if (tokenResult is Result.Error) return@withContext tokenResult

            val accessToken = tokenResult.data.accessToken
            val userResult = apiService.getUser("Bearer $accessToken")
            if (userResult is Result.Error) return@withContext userResult

            val user = userResult.data
            val authState = GitHubAuthState(
                isAuthenticated = true, accessToken = accessToken,
                username = user.login, userId = user.id, avatarUrl = user.avatarUrl,
            )
            saveAuthState(authState)
            onAuthState(authState)
            Result.Success(authState)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun checkAuth(): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        val token = encryptedPrefs.getString("access_token", null) ?: return@withContext Result.Error(Exception("No token"))
        val userResult = apiService.getUser("Bearer $token")
        if (userResult is Result.Error) {
            logout()
            return@withContext userResult
        }
        Result.Success(loadAuthState())
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
        Result.Success(Unit)
    }

    private suspend fun pollForToken(deviceCode: String, interval: Int): Result<TokenResponse> = withContext(Dispatchers.IO) {
        TODO("Implement token polling")
    }

    private fun saveAuthState(state: GitHubAuthState) {
        encryptedPrefs.edit()
            .putString("access_token", state.accessToken ?: "")
            .putString("username", state.username ?: "")
            .putLong("user_id", state.userId ?: -1)
            .putString("avatar_url", state.avatarUrl ?: "")
            .putLong("expires_at", state.expiresAt ?: -1)
            .apply()
    }
}
