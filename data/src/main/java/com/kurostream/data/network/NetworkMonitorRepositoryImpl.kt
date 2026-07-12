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

package com.kurostream.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import com.kurostream.core.common.result.Result
import com.kurostream.domain.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class NetworkMonitorRepositoryImpl @Inject constructor(
    private val context: Context,
) : NetworkMonitorRepository {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _networkStats = MutableStateFlow<NetworkStats>(NetworkStats(
        downloadSpeedMbps = 0.0,
        uploadSpeedMbps = 0.0,
        latencyMs = 0.0,
        jitterMs = 0.0,
        packetLossPercent = 0.0,
        wifiSignalStrengthDbm = null,
        wifiLinkSpeedMbps = null,
        networkType = NetworkType.UNKNOWN,
        isMetered = false,
        totalBytesReceived = 0,
        totalBytesSent = 0,
    ))

    private val _connectionQuality = MutableStateFlow<ConnectionQuality>(ConnectionQuality(
        overallScore = 0,
        latencyScore = 0,
        throughputScore = 0,
        stabilityScore = 0,
        rating = QualityRating.UNUSABLE,
    ))

    private val _activeConnections = MutableStateFlow<List<ActiveConnection>>(emptyList())
    private val networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoring = false

    override suspend fun startMonitoring() = withContext(Dispatchers.IO) {
        if (isMonitoring) return@withContext
        isMonitoring = true

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateNetworkStats(network, capabilities)
            }

            override fun onLost(network: Network) {
                _networkStats.value = _networkStats.value.copy(
                    networkType = NetworkType.UNKNOWN,
                    downloadSpeedMbps = 0.0,
                    uploadSpeedMbps = 0.0,
                )
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        
        // Start periodic stats collection
        kotlinx.coroutines.Dispatchers.IO.launch {
            while (isMonitoring) {
                collectNetworkStats()
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }

    override suspend fun stopMonitoring() = withContext(Dispatchers.IO) {
        isMonitoring = false
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }

    override fun observeNetworkStats() = _networkStats.asStateFlow()

    override fun observeConnectionQuality() = _connectionQuality.asStateFlow()

    override fun observeActiveConnections() = _activeConnections.asStateFlow()

    private fun updateNetworkStats(network: Network, capabilities: NetworkCapabilities) {
        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }

        val signalStrength = if (networkType == NetworkType.WIFI) {
            getWifiSignalStrength()
        } else null

        val linkSpeed = if (networkType == NetworkType.WIFI) {
            getWifiLinkSpeed()
        } else null

        _networkStats.value = _networkStats.value.copy(
            networkType = networkType,
            isMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).not(),
            wifiSignalStrengthDbm = signalStrength,
            wifiLinkSpeedMbps = linkSpeed,
        )
    }

    private suspend fun collectNetworkStats() = withContext(Dispatchers.IO) {
        // Get network stats from TrafficStats or /proc/net/dev
        val stats = getSystemNetworkStats()
        val wifiInfo = wifiManager.connectionInfo

        val currentStats = _networkStats.value
        val newStats = currentStats.copy(
            timestamp = System.currentTimeMillis(),
            totalBytesReceived = stats.rxBytes,
            totalBytesSent = stats.txBytes,
            wifiSignalStrengthDbm = wifiInfo?.rssi,
            wifiLinkSpeedMbps = wifiInfo?.linkSpeed,
        )

        // Calculate speeds (simplified - would use delta over time)
        _networkStats.value = newStats

        // Update connection quality
        updateConnectionQuality(newStats)
    }

    private fun getWifiSignalStrength(): Int? {
        val wifiInfo = wifiManager.connectionInfo
        return if (wifiInfo != null) wifiInfo.rssi else null
    }

    private fun getWifiLinkSpeed(): Int? {
        val wifiInfo = wifiManager.connectionInfo
        return if (wifiInfo != null) wifiInfo.linkSpeed else null
    }

    private data class SystemStats(
        val rxBytes: Long,
        val txBytes: Long,
    )

    private fun getSystemNetworkStats(): SystemStats {
        // Read from /proc/net/dev or use TrafficStats
        var rxBytes = 0L
        var txBytes = 0L
        
        try {
            val process = Runtime.getRuntime().exec("cat /proc/net/dev")
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.contains(":") && !line.contains("lo:")) {
                        val parts = line.trim().split("\\s+".toRegex()).dropLast(1)
                        if (parts.size >= 10) {
                            rxBytes += parts[1].toLongOrNull() ?: 0L
                            txBytes += parts[9].toLongOrNull() ?: 0L
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read network stats")
        }
        
        return SystemStats(rxBytes, txBytes)
    }

    private fun updateConnectionQuality(stats: NetworkStats) {
        val latencyScore = when {
            stats.latencyMs < 50 -> 100
            stats.latencyMs < 100 -> 80
            stats.latencyMs < 200 -> 60
            stats.latencyMs < 500 -> 40
            else -> 20
        }

        val throughputScore = when {
            stats.downloadSpeedMbps > 50 -> 100
            stats.downloadSpeedMbps > 25 -> 80
            stats.downloadSpeedMbps > 10 -> 60
            stats.downloadSpeedMbps > 5 -> 40
            else -> 20
        }

        val stabilityScore = when {
            stats.packetLossPercent < 0.1 -> 100
            stats.packetLossPercent < 1 -> 80
            stats.packetLossPercent < 5 -> 60
            stats.packetLossPercent < 10 -> 40
            else -> 20
        }

        val overallScore = (latencyScore + throughputScore + stabilityScore) / 3
        val rating = when {
            overallScore >= 80 -> QualityRating.EXCELLENT
            overallScore >= 60 -> QualityRating.GOOD
            overallScore >= 40 -> QualityRating.FAIR
            overallScore >= 20 -> QualityRating.POOR
            else -> QualityRating.UNUSABLE
        }

        val issues = mutableListOf<String>()
        if (stats.latencyMs > 200) issues.add("High latency: ${stats.latencyMs}ms")
        if (stats.packetLossPercent > 1) issues.add("Packet loss: ${stats.packetLossPercent}%")
        if (stats.downloadSpeedMbps < 5) issues.add("Low download speed: ${stats.downloadSpeedMbps} Mbps")
        if (stats.wifiSignalStrengthDbm != null && stats.wifiSignalStrengthDbm!! < -70) {
            issues.add("Weak Wi-Fi signal: ${stats.wifiSignalStrengthDbm} dBm")
        }

        _connectionQuality.value = ConnectionQuality(
            overallScore = overallScore,
            latencyScore = latencyScore,
            throughputScore = throughputScore,
            stabilityScore = stabilityScore,
            rating = rating,
            issues = issues,
        )
    }

    override suspend fun runSpeedTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        // In production, would use a proper speed test service
        // This is a placeholder implementation
        try {
            val startTime = System.currentTimeMillis()
            val url = java.net.URL("https://speed.cloudflare.com/__down?bytes=10000000")
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            val inputStream = connection.getInputStream()
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                totalBytes += bytesRead
                bytesRead = inputStream.read(buffer)
            }
            inputStream.close()
            
            val durationSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            val downloadSpeedMbps = (totalBytes * 8) / (durationSeconds * 1_000_000)
            
            SpeedTestResult(
                downloadSpeedMbps = downloadSpeedMbps,
                uploadSpeedMbps = downloadSpeedMbps * 0.5, // Estimate
                latencyMs = _networkStats.value.latencyMs,
                serverLocation = "Cloudflare",
                serverIp = "1.1.1.1",
            )
        } catch (e: Exception) {
            SpeedTestResult(
                downloadSpeedMbps = 0.0,
                uploadSpeedMbps = 0.0,
                latencyMs = 0.0,
                serverLocation = "Unknown",
                serverIp = "Unknown",
                error = e.message,
            )
        }
    }

    override suspend fun runPingTest(host: String): PingTestResult = withContext(Dispatchers.IO) {
        var minMs = Double.MAX_VALUE
        var maxMs = 0.0
        var sumMs = 0.0
        var lost = 0
        val count = 10

        repeat(count) {
            try {
                val start = System.nanoTime()
                val address = java.net.InetAddress.getByName(host)
                val reachable = address.isReachable(2000)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                
                if (reachable) {
                    minMs = minOf(minMs, elapsed)
                    maxMs = maxOf(maxMs, elapsed)
                    sumMs += elapsed
                } else {
                    lost++
                }
            } catch (e: Exception) {
                lost++
            }
            Thread.sleep(100)
        }

        PingTestResult(
            host = host,
            minMs = if (minMs == Double.MAX_VALUE) 0.0 else minMs,
            maxMs = maxMs,
            avgMs = if (count - lost > 0) sumMs / (count - lost) else 0.0,
            packetLossPercent = (lost * 100.0) / count,
        )
    }

    override suspend fun getNetworkInterfaceInfo(): List<NetworkInterfaceInfo> = withContext(Dispatchers.IO) {
        val interfaces = mutableListOf<NetworkInterfaceInfo>()
        try {
            java.net.NetworkInterface.getNetworkInterfaces().asIterable().forEach { ni ->
                if (ni.isUp && !ni.isLoopback) {
                    val addresses = ni.interfaceAddresses.map { it.address.hostAddress }.toList()
                    val ipv4 = addresses.find { it.contains(".") } ?: ""
                    
                    interfaces.add(NetworkInterfaceInfo(
                        name = ni.name,
                        displayName = ni.displayName,
                        type = when {
                            ni.name.startsWith("wlan") || ni.name.startsWith("wifi") -> NetworkType.WIFI
                            ni.name.startsWith("eth") -> NetworkType.ETHERNET
                            ni.name.startsWith("rmnet") || ni.name.startsWith("pdp") -> NetworkType.MOBILE
                            ni.name.startsWith("tun") || ni.name.startsWith("vpn") -> NetworkType.VPN
                            else -> NetworkType.UNKNOWN
                        },
                        ipAddress = ipv4,
                        gateway = null, // Would need root to get
                        dnsServers = getDnsServers(),
                        isUp = ni.isUp,
                        speedMbps = null,
                        mtu = ni.mtu,
                    ))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get network interfaces")
        }
        return@withContext interfaces
    }

    private fun getDnsServers(): List<String> {
        val dnsServers = mutableListOf<String>()
        try {
            val file = java.io.File("/etc/resolv.conf")
            if (file.exists()) {
                file.readText().forEachLine { line ->
                    if (line.startsWith("nameserver")) {
                        dnsServers.add(line.substringAfter("nameserver").trim())
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return if (dnsServers.isEmpty()) listOf("8.8.8.8", "1.1.1.1") else dnsServers
    }
}