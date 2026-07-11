package com.kurostream.torrent.engine

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.kurostream.torrent.domain.TorrentSessionSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveLimitsCalculator @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {

    private val TAG = "AdaptiveLimits"

    data class DeviceCapabilities(
        val totalMemoryMb: Int,
        val availableMemoryMb: Int,
        val cpuCores: Int,
        val isHighEnd: Boolean,
        val networkType: NetworkType,
        val networkBandwidthMbps: Int,
    )

    enum class NetworkType { WIFI, ETHERNET, CELLULAR_FAST, CELLULAR_SLOW, UNKNOWN }

    fun detectCapabilities(): DeviceCapabilities {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMemoryMb = (memInfo.totalMem / 1024 / 1024).toInt()
        val availableMemoryMb = (memInfo.availMem / 1024 / 1024).toInt()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val isHighEnd = totalMemoryMb >= 3000 && cpuCores >= 4

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        val networkType = when {
            caps == null -> NetworkType.UNKNOWN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (caps.linkDownstreamBandwidthKbps > 10000) NetworkType.CELLULAR_FAST
                else NetworkType.CELLULAR_SLOW
            }
            else -> NetworkType.UNKNOWN
        }

        val bandwidthMbps = caps?.linkDownstreamBandwidthKbps?.div(1000) ?: 0

        return DeviceCapabilities(
            totalMemoryMb = totalMemoryMb,
            availableMemoryMb = availableMemoryMb,
            cpuCores = cpuCores,
            isHighEnd = isHighEnd,
            networkType = networkType,
            networkBandwidthMbps = bandwidthMbps,
        )
    }

    fun calculateOptimalSettings(capabilities: DeviceCapabilities = detectCapabilities()): TorrentSessionSettings {
        val maxConnections = when {
            capabilities.isHighEnd && capabilities.networkType == NetworkType.WIFI -> 300
            capabilities.isHighEnd -> 200
            capabilities.totalMemoryMb < 1000 -> 50
            else -> 120
        }

        val connectionsPerTorrent = (maxConnections / 3).coerceIn(20, 100)

        val maxUploadSlots = when {
            capabilities.isHighEnd -> 75
            capabilities.totalMemoryMb < 1000 -> 20
            else -> 40
        }

        val halfOpenConnections = when {
            capabilities.isHighEnd -> 16
            capabilities.totalMemoryMb < 1000 -> 4
            else -> 8
        }

        Log.i(TAG, "Optimal settings: maxConn=$maxConnections, perTorrent=$connectionsPerTorrent, " +
                "uploadSlots=$maxUploadSlots, halfOpen=$halfOpenConnections " +
                "[${capabilities.totalMemoryMb}MB RAM, ${capabilities.cpuCores} cores, ${capabilities.networkType}]")

        return TorrentSessionSettings(
            maxConnections = maxConnections,
            connectionsPer_torrent = connectionsPerTorrent,
            maxUploadSlots = maxUploadSlots,
            maxHalfOpenConnections = halfOpenConnections,
            connectionsPerTorrent = connectionsPerTorrent,
            maxUploadSlotsPerTorrent = (maxUploadSlots / 3).coerceIn(5, 25),
        )
    }

    fun shouldEnableAggressiveMode(capabilities: DeviceCapabilities = detectCapabilities()): Boolean {
        return capabilities.isHighEnd && capabilities.networkType in setOf(
            NetworkType.WIFI, NetworkType.ETHERNET
        )
    }
}
