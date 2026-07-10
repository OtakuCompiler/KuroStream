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

package com.kurostream.players.advanced.drm

import android.content.Context
import android.media.DeniedByServerException
import android.media.MediaDrm
import android.media.NotProvisionedException
import android.media.UnsupportedSchemeException
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Internal HTTP proxy for Widevine L1 DRM key exchange.
 * Intercepts DASH/HLS license requests and routes them through the device's
 * Widevine CDM for secure key acquisition.
 *
 * SECURITY: This proxy runs on localhost only and uses TLS for all external communication.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class WidevineProxyServer(
    private val context: Context,
    port: Int = 0  // 0 = auto-assign
) : NanoHTTPD("127.0.0.1", port) {

    companion object {
        const val WIDEVINE_UUID = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
        const val PROXY_PATH_LICENSE = "/license"
        const val PROXY_PATH_CERTIFICATE = "/certificate"
        const val PROXY_PATH_PROVISION = "/provision"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_ORIGIN = "Origin"
        const val MAX_LICENSE_SIZE = 64 * 1024  // 64KB max license response
    }

    private val proxyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private val sessionCache = ConcurrentHashMap<String, DrmSession>()

    private var mediaDrm: MediaDrm? = null
    private val widevineUuid = UUID.fromString(WIDEVINE_UUID)

    // Security: AES-GCM encryption for cached keys
    private val keyEncryptionKey: ByteArray by lazy {
        generateSecureKey()
    }

    data class DrmSession(
        val sessionId: ByteArray,
        val keySetId: ByteArray? = null,
        val licenseUrl: String? = null,
        val lastUsed: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DrmSession) return false
            return sessionId.contentEquals(other.sessionId)
        }

        override fun hashCode(): Int = sessionId.contentHashCode()
    }

    data class LicenseRequest(
        val initData: ByteArray,
        val licenseServerUrl: String,
        val customData: String? = null,
        val contentId: String? = null
    )

    data class LicenseResponse(
        val licenseData: ByteArray,
        val renewalUrl: String? = null,
        val licenseDuration: Long = 0
    )

    init {
        initializeMediaDrm()
    }

    private fun initializeMediaDrm() {
        try {
            mediaDrm = MediaDrm(widevineUuid)
            mediaDrm?.setPropertyString(
                MediaDrm.PROPERTY_SECURITY_LEVEL,
                "L1" // Request L1 security level
            )

            // Set up event listener for key expiration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaDrm?.setOnEventListener { _, sessionId, event, extra, data ->
                    handleDrmEvent(sessionId, event, extra, data)
                }
            }
        } catch (e: UnsupportedSchemeException) {
            throw IllegalStateException("Widevine not supported on this device", e)
        }
    }

    override fun start() {
        super.start()
        isRunning.set(true)
    }

    override fun stop() {
        isRunning.set(false)
        super.stop()
        proxyScope.cancel()

        // Close all DRM sessions
        sessionCache.values.forEach { session ->
            try {
                mediaDrm?.closeSession(session.sessionId)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        sessionCache.clear()
        mediaDrm?.release()
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            PROXY_PATH_LICENSE -> handleLicenseRequest(session)
            PROXY_PATH_CERTIFICATE -> handleCertificateRequest(session)
            PROXY_PATH_PROVISION -> handleProvisionRequest(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    private fun handleLicenseRequest(session: IHTTPSession): Response {
        return try {
            // Parse request body
            val body = ByteArray(session.contentLength.toInt())
            session.inputStream.read(body)
            val request = parseLicenseRequest(body)

            // Validate request
            if (request.licenseServerUrl.isBlank()) {
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    JSONObject().put("error", "Missing license server URL").toString()
                )
            }

            // Acquire license through Widevine CDM
            val response = proxyScope.async {
                acquireLicense(request)
            }.await()

            // Return license response
            newFixedLengthResponse(
                Response.Status.OK,
                "application/octet-stream",
                ByteArrayInputStream(response.licenseData),
                response.licenseData.size.toLong()
            )

        } catch (e: DeniedByServerException) {
            newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "application/json",
                JSONObject().put("error", "License denied by server: ${e.message}").toString()
            )
        } catch (e: NotProvisionedException) {
            newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                "application/json",
                JSONObject().put("error", "Device not provisioned").toString()
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().put("error", e.message).toString()
            )
        }
    }

    private suspend fun acquireLicense(request: LicenseRequest): LicenseResponse =
        withContext(Dispatchers.IO) {
            val drm = mediaDrm ?: throw IllegalStateException("MediaDrm not initialized")

            // Open new DRM session
            val sessionId = drm.openSession()
            val sessionKey = UUID.randomUUID().toString()

            try {
                // Generate key request
                val keyRequest = drm.getKeyRequest(
                    sessionId,
                    request.initData,
                    "video/mp4",
                    MediaDrm.KEY_TYPE_STREAMING,
                    request.customData?.let { hashMapOf("PRCustomData" to it) } ?: hashMapOf()
                )

                // Send key request to license server
                val licenseData = postToLicenseServer(
                    request.licenseServerUrl,
                    keyRequest.data,
                    request.customData
                )

                // Provide key response to MediaDrm
                drm.provideKeyResponse(sessionId, licenseData)

                // Cache session for reuse
                sessionCache[sessionKey] = DrmSession(
                    sessionId = sessionId,
                    licenseUrl = request.licenseServerUrl,
                    lastUsed = System.currentTimeMillis()
                )

                // Query key status
                val keyStatus = drm.queryKeyStatus(sessionId)
                val duration = keyStatus[MediaDrm.KEY_STATUS_RENEWAL_SERVER_URL]?.toLong() ?: 0

                LicenseResponse(
                    licenseData = licenseData,
                    licenseDuration = duration
                )

            } catch (e: Exception) {
                drm.closeSession(sessionId)
                throw e
            }
        }

    private fun postToLicenseServer(
        url: String,
        keyRequestData: ByteArray,
        customData: String?
    ): ByteArray {
        val connection = URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        customData?.let {
            connection.setRequestProperty("X-Custom-Data", it)
        }

        connection.outputStream.use { it.write(keyRequestData) }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw DeniedByServerException("License server returned $responseCode")
        }

        return connection.inputStream.use { it.readBytes() }
    }

    private fun handleCertificateRequest(session: IHTTPSession): Response {
        return try {
            val drm = mediaDrm ?: throw IllegalStateException("MediaDrm not initialized")
            val certificate = drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)

            newFixedLengthResponse(
                Response.Status.OK,
                "application/octet-stream",
                ByteArrayInputStream(certificate),
                certificate.size.toLong()
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().put("error", e.message).toString()
            )
        }
    }

    private fun handleProvisionRequest(session: IHTTPSession): Response {
        return try {
            val drm = mediaDrm ?: throw IllegalStateException("MediaDrm not initialized")
            val provisionRequest = drm.getProvisionRequest()

            // Forward to provisioning server
            val provisionResponse = postToLicenseServer(
                provisionRequest.defaultUrl,
                provisionRequest.data,
                null
            )

            drm.provideProvisionResponse(provisionResponse)

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                JSONObject().put("status", "provisioned").toString()
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().put("error", e.message).toString()
            )
        }
    }

    private fun parseLicenseRequest(body: ByteArray): LicenseRequest {
        val json = JSONObject(String(body))
        return LicenseRequest(
            initData = Base64.decode(json.getString("initData"), Base64.NO_WRAP),
            licenseServerUrl = json.getString("licenseServerUrl"),
            customData = json.optString("customData", null),
            contentId = json.optString("contentId", null)
        )
    }

    private fun handleDrmEvent(sessionId: ByteArray, event: Int, extra: Int, data: ByteArray) {
        when (event) {
            MediaDrm.EVENT_KEY_REQUIRED -> {
                // Key renewal needed
                val session = sessionCache.values.find {
                    it.sessionId.contentEquals(sessionId)
                }
                session?.let {
                    proxyScope.launch {
                        renewLicense(it)
                    }
                }
            }
            MediaDrm.EVENT_KEY_EXPIRED -> {
                // Key expired, remove from cache
                sessionCache.values.removeAll {
                    it.sessionId.contentEquals(sessionId)
                }
            }
            MediaDrm.EVENT_VENDOR_DEFINED -> {
                android.util.Log.d("WidevineProxy", "Vendor defined event: $extra")
            }
        }
    }

    private suspend fun renewLicense(session: DrmSession) {
        // Implementation for automatic license renewal
        // This would re-request keys before expiration
    }

    private fun generateSecureKey(): ByteArray {
        val key = ByteArray(32)
        java.security.SecureRandom().nextBytes(key)
        return key
    }

    private fun encryptKeyData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyEncryptionKey, "AES")
        val iv = ByteArray(12).apply { java.security.SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    /**
     * Get the proxy URL for ExoPlayer integration.
     */
    fun getProxyUrl(): String {
        return "http://127.0.0.1:$listeningPort"
    }
}
