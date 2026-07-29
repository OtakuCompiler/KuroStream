package com.kurostream.torrent.network

import timber.log.Timber
import timber.log.Timber
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuicTorrentProxy @Inject constructor() {

    private val TAG = "QuicTorrentProxy"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var isEnabled = false

    fun enable() {
        isEnabled = true
        Timber.tag(TAG).i(, "QUIC tracker proxy enabled (experimental)")
    }

    fun disable() {
        isEnabled = false
        Timber.tag(TAG).i(, "QUIC tracker proxy disabled")
    }

    fun isQuicSupported(): Boolean {
        return try {
            Class.forName("org.chromium.net.CronetEngine")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    suspend fun announceViaQuic(
        trackerUrl: String,
        infoHash: String,
        port: Int,
    ): AnnounceResult {
        if (!isEnabled) {
            return AnnounceResult.NotEnabled
        }

        return try {
            val quicUrl = trackerUrl
                .replace("http://", "https://")
                .replace("udp://", "https://")

            val request = Request.Builder()
                .url(quicUrl)
                .header("User-Agent", "KuroStream/1.0 QUIC")
                .build()

            val startTime = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val durationMs = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                AnnounceResult.Success(
                    responseTimeMs = durationMs,
                    peerCount = 0,
                )
            } else {
                AnnounceResult.Failed("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(, "QUIC announce failed for $trackerUrl", e)
            AnnounceResult.Failed(e.message ?: "Unknown error")
        }
    }

    sealed class AnnounceResult {
        data object NotEnabled : AnnounceResult()
        data class Success(val responseTimeMs: Long, val peerCount: Int) : AnnounceResult()
        data class Failed(val error: String) : AnnounceResult()
    }
}
