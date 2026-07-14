package com.kurostream.torrent.streaming

import android.util.Log
import com.kurostream.torrent.domain.TorrentInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpFallbackManager @Inject constructor(
    private val streamingTorrentManager: StreamingTorrentManager,
) {

    private val TAG = "HttpFallbackManager"

    private val fallbackStates = ConcurrentHashMap<String, FallbackState>()
    private val fallbackJobs = ConcurrentHashMap<String, Job>()
    private val httpSourceCache = ConcurrentHashMap<String, HttpSourceInfo>()

    private val _globalFallbackState = MutableStateFlow(GlobalFallbackState())
    val globalFallbackState: StateFlow<GlobalFallbackState> = _globalFallbackState.asStateFlow()

    private val MIN_TORRENT_SPEED_BPS = 500_000L // 500 KB/s minimum
    private val FALLBACK_CHECK_INTERVAL_MS = 5000L
    private val FALLBACK_COOLDOWN_MS = 60_000L // 1 minute cooldown
    private val MAX_FALLBACK_ATTEMPTS = 3

    data class FallbackState(
        val infoHash: String,
        val fileIndex: Int,
        var isFallbackActive: Boolean = false,
        var fallbackUrl: String? = null,
        var fallbackStartTime: Long = 0,
        var fallbackAttempts: Int = 0,
        var lastFallbackTime: Long = 0,
        var originalTorrentSpeed: Long = 0,
        var httpSpeed: Long = 0,
        var canReturnToTorrent: Boolean = false,
    )

    data class HttpSourceInfo(
        val infoHash: String,
        val fileIndex: Int,
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val title: String = "",
        val quality: String = "",
        val verifiedAt: Long = System.currentTimeMillis(),
    )

    data class GlobalFallbackState(
        val activeFallbacks: Int = 0,
        val totalFallbacksTriggered: Int = 0,
        val successfulFallbacks: Int = 0,
        val failedFallbacks: Int = 0,
    )

    fun checkAndTriggerFallback(
        infoHash: String,
        fileIndex: Int,
        torrentSpeedBps: Long,
        httpUrl: String?,
        httpHeaders: Map<String, String> = emptyMap(),
    ): Boolean {
        val state = fallbackStates.getOrPut(infoHash) {
            FallbackState(infoHash = infoHash, fileIndex = fileIndex)
        }

        if (state.isFallbackActive) {
            if (state.fallbackUrl != null && httpUrl != null && state.fallbackUrl != httpUrl) {
                state.fallbackUrl = httpUrl
            }
            return state.isFallbackActive
        }

        val now = System.currentTimeMillis()
        if (state.fallbackAttempts >= MAX_FALLBACK_ATTEMPTS) {
            Log.d(TAG, "Max fallback attempts reached for $infoHash")
            return false
        }

        if (now - state.lastFallbackTime < FALLBACK_COOLDOWN_MS) {
            return false
        }

        if (torrentSpeedBps < MIN_TORRENT_SPEED_BPS) {
            Log.w(TAG, "Torrent speed ${formatSpeed(torrentSpeedBps)} below threshold ${formatSpeed(MIN_TORRENT_SPEED_BPS)}, triggering HTTP fallback for $infoHash")

            if (httpUrl != null) {
                triggerFallback(infoHash, fileIndex, httpUrl, httpHeaders, torrentSpeedBps)
                return true
            } else {
                Log.w(TAG, "No HTTP fallback URL available for $infoHash")
            }
        }

        return false
    }

    private fun triggerFallback(
        infoHash: String,
        fileIndex: Int,
        httpUrl: String,
        httpHeaders: Map<String, String>,
        torrentSpeedBps: Long
    ) {
        val state = fallbackStates[infoHash] ?: return
        state.isFallbackActive = true
        state.fallbackUrl = httpUrl
        state.fallbackStartTime = System.currentTimeMillis()
        state.fallbackAttempts++
        state.lastFallbackTime = System.currentTimeMillis()
        state.originalTorrentSpeed = torrentSpeedBps

        streamingTorrentManager.stopStreaming(infoHash)

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        fallbackJobs[infoHash] = scope.launch {
            try {
                val verifiedUrl = verifyHttpSource(httpUrl, httpHeaders)
                if (verifiedUrl != null) {
                    state.fallbackUrl = verifiedUrl
                    state.canReturnToTorrent = true

                    httpSourceCache[infoHash] = HttpSourceInfo(
                        infoHash = infoHash,
                        fileIndex = fileIndex,
                        url = verifiedUrl,
                        headers = httpHeaders,
                        verifiedAt = System.currentTimeMillis(),
                    )

                    _globalFallbackState.update { it.copy(
                        activeFallbacks = _globalFallbackState.value.activeFallbacks + 1,
                        totalFallbacksTriggered = _globalFallbackState.value.totalFallbacksTriggered + 1,
                        successfulFallbacks = _globalFallbackState.value.successfulFallbacks + 1,
                    )}

                    Log.i(TAG, "HTTP fallback activated for $infoHash: $verifiedUrl")

                    monitorFallbackHealth(infoHash, state)
                } else {
                    handleFallbackFailure(infoHash, state, "HTTP source verification failed")
                }
            } catch (e: Exception) {
                handleFallbackFailure(infoHash, state, e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun verifyHttpSource(url: String, headers: Map<String, String>): String? {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val requestBuilder = okhttp3.Request.Builder().url(url).head()
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            return if (response.isSuccessful) {
                val contentLength = response.header("Content-Length")?.toLongOrNull()
                if (contentLength != null && contentLength > 0) {
                    Log.d(TAG, "HTTP source verified: $url, size: $contentLength")
                    url
                } else {
                    Log.w(TAG, "HTTP source missing Content-Length: $url")
                    url
                }
            } else {
                Log.w(TAG, "HTTP source verification failed: ${response.code()} $url")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP source verification error: ${e.message}")
            null
        }
    }

    private fun monitorFallbackHealth(infoHash: String, state: FallbackState) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            while (isActive && state.isFallbackActive) {
                delay(FALLBACK_CHECK_INTERVAL_MS)

                if (!state.isFallbackActive) break

                val torrentSpeed = streamingTorrentManager.getStreamState(infoHash)?.bufferHealth?.downloadSpeedBps ?: 0
                val httpSpeed = measureHttpSpeed(state.fallbackUrl!!, emptyMap())

                state.httpSpeed = httpSpeed

                if (torrentSpeed > MIN_TORRENT_SPEED_BPS * 2 && state.canReturnToTorrent) {
                    Log.i(TAG, "Torrent speed recovered (${formatSpeed(torrentSpeed)}), switching back from HTTP fallback for $infoHash")
                    returnToTorrent(infoHash)
                    break
                }

                if (httpSpeed < MIN_TORRENT_SPEED_BPS / 2) {
                    Log.w(TAG, "HTTP fallback also slow (${formatSpeed(httpSpeed)}), trying alternative source for $infoHash")
                    tryAlternativeSource(infoHash)
                    break
                }
            }
        }
    }

    private suspend fun measureHttpSpeed(url: String, headers: Map<String, String> = emptyMap()): Long {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val requestBuilder = okhttp3.Request.Builder().url(url).get()
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            requestBuilder.addHeader("Range", "bytes=0-1048575")

            val startTime = System.currentTimeMillis()
            val response = client.newCall(requestBuilder.build()).execute()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val bytes = response.body?.contentLength() ?: 0
                if (elapsed > 0) {
                    (bytes * 1000L / elapsed).coerceAtLeast(0)
                } else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun tryAlternativeSource(infoHash: String) {
        val state = fallbackStates[infoHash] ?: return
        state.fallbackAttempts++

        if (state.fallbackAttempts >= MAX_FALLBACK_ATTEMPTS) {
            handleFallbackFailure(infoHash, state, "Max fallback attempts reached")
            return
        }

        val cached = httpSourceCache[infoHash]
        if (cached != null) {
            triggerFallback(infoHash, cached.fileIndex, cached.url, cached.headers, state.originalTorrentSpeed)
        }
    }

    private fun handleFallbackFailure(infoHash: String, state: FallbackState, reason: String) {
        Log.e(TAG, "HTTP fallback failed for $infoHash: $reason")
        state.isFallbackActive = false
        state.fallbackUrl = null

        _globalFallbackState.update { it.copy(
            activeFallbacks = maxOf(0, it.activeFallbacks - 1),
            failedFallbacks = it.failedFallbacks + 1,
        )}

        streamingTorrentManager.startStreaming(infoHash, state.fileIndex)
    }

    fun returnToTorrent(infoHash: String) {
        val state = fallbackStates[infoHash] ?: return
        if (!state.isFallbackActive) return

        Log.i(TAG, "Returning to torrent streaming for $infoHash")
        state.isFallbackActive = false
        state.fallbackUrl = null
        state.canReturnToTorrent = false

        fallbackJobs[infoHash]?.cancel()
        fallbackJobs.remove(infoHash)

        _globalFallbackState.update { it.copy(
            activeFallbacks = maxOf(0, it.activeFallbacks - 1),
        )}

        streamingTorrentManager.startStreaming(infoHash, state.fileIndex)
    }

    fun getFallbackUrl(infoHash: String): String? {
        return fallbackStates[infoHash]?.fallbackUrl
    }

    fun isFallbackActive(infoHash: String): Boolean {
        return fallbackStates[infoHash]?.isFallbackActive == true
    }

    fun getFallbackState(infoHash: String): FallbackState? = fallbackStates[infoHash]

    fun registerHttpSource(infoHash: String, fileIndex: Int, url: String, headers: Map<String, String> = emptyMap(), title: String = "", quality: String = "") {
        httpSourceCache[infoHash] = HttpSourceInfo(
            infoHash = infoHash,
            fileIndex = fileIndex,
            url = url,
            headers = headers,
            title = title,
            quality = quality,
        )
        Log.d(TAG, "Registered HTTP fallback source for $infoHash: $url")
    }

    fun shutdown() {
        fallbackJobs.values.forEach { it.cancel() }
        fallbackJobs.clear()
        fallbackStates.clear()
        httpSourceCache.clear()
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "${bytesPerSec} B/s"
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
            else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
        }
    }
}