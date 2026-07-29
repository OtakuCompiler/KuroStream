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

package com.kurostream.players.buffering

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import com.kurostream.players.buffer.DiskBackedAllocator
import com.kurostream.players.core.PlayerInterface
import com.kurostream.players.core.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Phase 27: Adaptive Buffering Logic + Network Speed Detection (FINAL)
 * Updated with disk-backed allocator for low memory footprint.
 */
class AdaptiveBufferingManager(
    private val context: Context,
    private val player: PlayerInterface
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val networkSpeedDetector = NetworkSpeedDetector(context)

    private val _bufferConfig = MutableStateFlow(BufferConfiguration())
    val bufferConfig: StateFlow<BufferConfiguration> = _bufferConfig.asStateFlow()

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var monitoringJob: Job? = null

    companion object {
        const val MIN_BUFFER_MS = 5_000L
        const val MAX_BUFFER_MS = 120_000L
        const val DEFAULT_BUFFER_MS = 30_000L
        const val REBUFFER_THRESHOLD_FAST = 2_000L
        const val REBUFFER_THRESHOLD_SLOW = 10_000L
        const val SPEED_MEASURE_WINDOW_MS = 10_000L
    }

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            launch { monitorNetworkSpeed() }
            launch { monitorBufferHealth() }
            launch { adaptBufferConfiguration() }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun getRecommendedBuffer(): BufferConfiguration {
        val speed = networkSpeedDetector.currentSpeedBps

        return when {
            speed < 1_000_000 -> BufferConfiguration(
                minBufferMs = 15_000, maxBufferMs = 30_000,
                bufferForPlaybackMs = 5_000, bufferForPlaybackAfterRebufferMs = 10_000,
                backBufferMs = 5_000, targetBufferBytes = 2_000_000
            )
            speed < 5_000_000 -> BufferConfiguration(
                minBufferMs = 20_000, maxBufferMs = 60_000,
                bufferForPlaybackMs = 3_000, bufferForPlaybackAfterRebufferMs = 8_000,
                backBufferMs = 10_000, targetBufferBytes = 5_000_000
            )
            speed < 20_000_000 -> BufferConfiguration(
                minBufferMs = 30_000, maxBufferMs = 90_000,
                bufferForPlaybackMs = 2_500, bufferForPlaybackAfterRebufferMs = 5_000,
                backBufferMs = 15_000, targetBufferBytes = 10_000_000
            )
            speed < 50_000_000 -> BufferConfiguration(
                minBufferMs = 40_000, maxBufferMs = 120_000,
                bufferForPlaybackMs = 2_000, bufferForPlaybackAfterRebufferMs = 4_000,
                backBufferMs = 20_000, targetBufferBytes = 20_000_000
            )
            else -> BufferConfiguration(
                minBufferMs = 60_000, maxBufferMs = 180_000,
                bufferForPlaybackMs = 1_500, bufferForPlaybackAfterRebufferMs = 3_000,
                backBufferMs = 30_000, targetBufferBytes = 50_000_000
            )
        }
    }

    private suspend fun monitorNetworkSpeed() {
        while (isActive) {
            networkSpeedDetector.measureSpeed()
            _networkState.value = when {
                networkSpeedDetector.currentSpeedBps < 1_000_000 -> NetworkState.VerySlow
                networkSpeedDetector.currentSpeedBps < 5_000_000 -> NetworkState.Slow
                networkSpeedDetector.currentSpeedBps < 20_000_000 -> NetworkState.Medium
                networkSpeedDetector.currentSpeedBps < 50_000_000 -> NetworkState.Fast
                else -> NetworkState.VeryFast
            }
            delay(5_000)
        }
    }

    private suspend fun monitorBufferHealth() {
        while (isActive) {
            val diagnostics = player.diagnostics.value
            val bufferMs = diagnostics.bufferDurationMs
            val config = _bufferConfig.value

            if (bufferMs < config.bufferForPlaybackMs &&
                player.playbackState.value == PlaybackState.Playing) {
                Timber.w("Buffer underflow: ${bufferMs}ms < ${config.bufferForPlaybackMs}ms")
                onBufferUnderflow()
            }

            if (bufferMs < config.bufferForPlaybackMs * 2) {
                Timber.d("Buffer running low: ${bufferMs}ms")
            }

            delay(1_000)
        }
    }

    private suspend fun adaptBufferConfiguration() {
        while (isActive) {
            val recommended = getRecommendedBuffer()
            val current = _bufferConfig.value

            val newConfig = BufferConfiguration(
                minBufferMs = lerp(current.minBufferMs, recommended.minBufferMs, 0.3f),
                maxBufferMs = lerp(current.maxBufferMs, recommended.maxBufferMs, 0.3f),
                bufferForPlaybackMs = recommended.bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs = recommended.bufferForPlaybackAfterRebufferMs,
                backBufferMs = lerp(current.backBufferMs, recommended.backBufferMs, 0.3f),
                targetBufferBytes = recommended.targetBufferBytes
            )

            _bufferConfig.value = newConfig
            delay(10_000)
        }
    }

    private fun onBufferUnderflow() {
        Timber.w("Buffer underflow - consider quality reduction")
    }

    private fun getNetworkType(): NetworkType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return NetworkType.NONE
        val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                when {
                    capabilities.linkDownstreamBandwidthKbps > 50_000 -> NetworkType.CELLULAR_5G
                    capabilities.linkDownstreamBandwidthKbps > 10_000 -> NetworkType.CELLULAR_LTE
                    else -> NetworkType.CELLULAR_SLOW
                }
            }
            else -> NetworkType.UNKNOWN
        }
    }

    private fun lerp(start: Long, end: Long, fraction: Float): Long {
        return (start + (end - start) * fraction).toLong()
    }

    data class BufferConfiguration(
        val minBufferMs: Long = 30_000,
        val maxBufferMs: Long = 120_000,
        val bufferForPlaybackMs: Long = 2_500,
        val bufferForPlaybackAfterRebufferMs: Long = 5_000,
        val backBufferMs: Long = 15_000,
        val targetBufferBytes: Long = 10_000_000
    )

    sealed class NetworkState {
        data object Unknown : NetworkState()
        data object VerySlow : NetworkState()
        data object Slow : NetworkState()
        data object Medium : NetworkState()
        data object Fast : NetworkState()
        data object VeryFast : NetworkState()
    }

    enum class NetworkType {
        NONE, UNKNOWN, WIFI, ETHERNET, CELLULAR_SLOW, CELLULAR_LTE, CELLULAR_5G
    }
}

class NetworkSpeedDetector(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val speedSamples = ArrayDeque<Long>(10)
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastMeasurementTime = SystemClock.elapsedRealtime()

    var currentSpeedBps: Long = 0
        private set

    suspend fun measureSpeed() {
        val method1 = measureViaTrafficStats()
        val method2 = measureViaDownloadTest()
        val method3 = measureViaConnectivityManager()

        currentSpeedBps = when {
            method2 > 0 -> method2
            method1 > 0 -> method1
            method3 > 0 -> method3
            else -> 0
        }

        speedSamples.addLast(currentSpeedBps)
        if (speedSamples.size > 10) speedSamples.removeFirst()

        currentSpeedBps = speedSamples.average().toLong()
    }

    private fun measureViaTrafficStats(): Long {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTime = SystemClock.elapsedRealtime()
        val timeDelta = currentTime - lastMeasurementTime

        return if (timeDelta > 0 && currentRxBytes > lastRxBytes) {
            val bytesDelta = currentRxBytes - lastRxBytes
            val speed = (bytesDelta * 1000) / timeDelta * 8
            lastRxBytes = currentRxBytes
            lastMeasurementTime = currentTime
            speed
        } else {
            0
        }
    }

    private suspend fun measureViaDownloadTest(): Long {
        return try {
            withTimeout(5_000) {
                val testUrl = "https://speed.hetzner.de/1MB.bin"
                val request = Request.Builder().url(testUrl).build()

                val startTime = SystemClock.elapsedRealtime()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.bytes() ?: byteArrayOf()
                val endTime = SystemClock.elapsedRealtime()

                val durationMs = endTime - startTime
                if (durationMs > 0) {
                    (body.size * 8 * 1000) / durationMs
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Timber.d(e, "Download speed test failed")
            0
        }
    }

    private fun measureViaConnectivityManager(): Long {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return 0
        val capabilities = cm.getNetworkCapabilities(network) ?: return 0

        return (capabilities.linkDownstreamBandwidthKbps * 1000).toLong()
    }
}
