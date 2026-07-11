package com.kurostream.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkCondition(
    val bandwidthEstimateMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val isMetered: Boolean = false,
    val isWifi: Boolean = false,
    val qualityScore: Int = 0,
)

enum class StreamingQuality {
    LOW_480P, MEDIUM_720P, HIGH_1080P, UHD_4K, AUTO
}

class AdaptiveBitrateController(private val context: Context) {
    private val _currentCondition = MutableStateFlow(NetworkCondition())
    val currentCondition: StateFlow<NetworkCondition> = _currentCondition.asStateFlow()

    private val _currentQuality = MutableStateFlow(StreamingQuality.AUTO)
    val currentQuality: StateFlow<StreamingQuality> = _currentQuality.asStateFlow()

    fun evaluateNetworkCondition(): NetworkCondition {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return NetworkCondition()
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkCondition()

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val bandwidth = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 50.0
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 100.0
            else -> 10.0
        }

        val condition = NetworkCondition(
            bandwidthEstimateMbps = bandwidth,
            latencyMs = if (isWifi) 20.0 else 50.0,
            isMetered = isMetered,
            isWifi = isWifi,
            qualityScore = when {
                bandwidth > 50 -> 100
                bandwidth > 25 -> 80
                bandwidth > 10 -> 60
                bandwidth > 5 -> 40
                else -> 20
            },
        )
        _currentCondition.value = condition
        return condition
    }

    fun selectQuality(condition: NetworkCondition): StreamingQuality {
        val quality = when {
            condition.bandwidthEstimateMbps >= 50 -> StreamingQuality.UHD_4K
            condition.bandwidthEstimateMbps >= 25 -> StreamingQuality.HIGH_1080P
            condition.bandwidthEstimateMbps >= 10 -> StreamingQuality.MEDIUM_720P
            else -> StreamingQuality.LOW_480P
        }
        _currentQuality.value = quality
        return quality
    }
}

object NetworkOptimizationConfig {
    const val CONNECTION_POOL_SIZE = 8
    const val CONNECTION_KEEP_ALIVE_SECONDS = 60L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val MAX_IDLE_CONNECTIONS = 5
    const val CHUNK_SIZE_BYTES = 256 * 1024
    const val MAX_RETRY_COUNT = 3
    const val RETRY_BACKOFF_BASE_MS = 1000L
    const val RETRY_BACKOFF_MAX_MS = 30000L
    const val ENABLE_QUIC = true
    const val ENABLE_HTTP2 = true
    const val ENABLE_CACHE = true
    const val CACHE_SIZE_BYTES = 50L * 1024 * 1024
}
