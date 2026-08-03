package com.kurostream.app.security

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PlayIntegrityChecker(private val context: Context) {

    private val integrityManager by lazy { IntegrityManagerFactory.create(context) }

    suspend fun checkIntegrity(): IntegrityResult = withContext(Dispatchers.IO) {
        try {
            val gpsAvailable = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
            if (!gpsAvailable) {
                return@withContext IntegrityResult.Error("Google Play Services not available")
            }

            val nonce = generateNonce()
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            val tokenResponse = suspendCoroutine<com.google.android.play.core.integrity.IntegrityTokenResponse> { continuation ->
                val task = integrityManager.requestIntegrityToken(request)
                task.addOnSuccessListener { continuation.resume(it) }
                task.addOnFailureListener { continuation.resumeWithException(it) }
            }

            IntegrityResult.Success(
                token = tokenResponse.token(),
                nonce = nonce,
                message = "Token generated. Must verify server-side."
            )
        } catch (e: Exception) {
            Timber.e(e, "Play Integrity check failed")
            IntegrityResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun isPlayInstalled(): Boolean {
        return try {
            checkIntegrity() is IntegrityResult.Success
        } catch (e: Exception) { false }
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString() + System.currentTimeMillis()
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    sealed class IntegrityResult {
        data class Success(val token: String, val nonce: String, val message: String) : IntegrityResult()
        data class Error(val message: String) : IntegrityResult()
    }
}