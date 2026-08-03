package com.kurostream.data.kurocloud.auth

import com.kurostream.data.security.EncryptedPreferences
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Singleton
class KuroTokenManager @Inject constructor(
    private val encryptedPrefs: EncryptedPreferences,
) {
    private val ACCESS_TOKEN_KEY = "kuro_access_token"
    private val REFRESH_TOKEN_KEY = "kuro_refresh_token"
    private val EXPIRY_KEY = "kuro_token_expiry"

    private val lock = Any()

    var accessToken: String?
        get() = synchronized(lock) { encryptedPrefs.getString(ACCESS_TOKEN_KEY) }
        set(value) = synchronized(lock) { encryptedPrefs.putString(ACCESS_TOKEN_KEY, value ?: "") }

    var refreshToken: String?
        get() = synchronized(lock) { encryptedPrefs.getString(REFRESH_TOKEN_KEY) }
        set(value) = synchronized(lock) { encryptedPrefs.putString(REFRESH_TOKEN_KEY, value ?: "") }

    var expiryTimestamp: Long
        get() = synchronized(lock) { encryptedPrefs.getString(EXPIRY_KEY)?.toLongOrNull() ?: 0 }
        set(value) = synchronized(lock) { encryptedPrefs.putString(EXPIRY_KEY, value.toString()) }

    fun isTokenValid(): Boolean {
        val token = accessToken
        val expiry = expiryTimestamp
        return token != null && expiry > System.currentTimeMillis() + 60_000 // 1 min buffer
    }

    fun clear() {
        synchronized(lock) {
            encryptedPrefs.remove(ACCESS_TOKEN_KEY)
            encryptedPrefs.remove(REFRESH_TOKEN_KEY)
            encryptedPrefs.remove(EXPIRY_KEY)
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiryTimestamp = System.currentTimeMillis() + expiresIn * 1000
    }
}

class KuroAuthenticator @Inject constructor(
    private val tokenManager: KuroTokenManager,
    private val authApi: com.kurostream.data.kurocloud.auth.KuroAuthService,
    private val anonKey: String,
) : Authenticator {

    private var isRefreshing = false
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null // Prevent infinite loops

        synchronized(refreshLock) {
            if (isRefreshing) {
                try {
                    (refreshLock as java.lang.Object).wait(5000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            } else {
                isRefreshing = true
                try {
                    val refreshToken = tokenManager.refreshToken ?: return null
                    val tokens = runBlocking(Dispatchers.IO) {
                        authApi.refreshToken(anonKey, body = com.kurostream.data.kurocloud.KuroRefreshRequest(refreshToken))
                    }
                    tokenManager.saveTokens(
                        tokens.accessToken,
                        tokens.refreshToken,
                        tokens.expiresIn
                    )
                } catch (e: Exception) {
                    tokenManager.clear()
                    return null
                } finally {
                    isRefreshing = false
                    (refreshLock as java.lang.Object).notifyAll()
                }
            }
        }

        val newToken = tokenManager.accessToken ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prev = response.priorResponse
        while (prev != null) {
            count++
            prev = prev.priorResponse
        }
        return count
    }
}