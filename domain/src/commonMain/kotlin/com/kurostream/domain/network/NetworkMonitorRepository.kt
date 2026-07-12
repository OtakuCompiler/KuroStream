package com.kurostream.domain.network
import com.kurostream.core.platform.platformCurrentTimeMillis

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface NetworkMonitorRepository {
    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    fun observeNetworkStats(): Flow<NetworkStats>
    fun observeConnectionQuality(): Flow<ConnectionQuality>
    fun observeActiveConnections(): Flow<List<ActiveConnection>>
    suspend fun runSpeedTest(): SpeedTestResult
    suspend fun runPingTest(host: String = "8.8.8.8"): PingTestResult
    suspend fun getNetworkInterfaceInfo(): List<NetworkInterfaceInfo>
}

@Serializable
data class NetworkStats(
    val timestamp: Long = platformCurrentTimeMillis(),
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val wifiSignalStrengthDbm: Int? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val isMetered: Boolean = false,
    val isVpnActive: Boolean = false,
    val dnsLatencyMs: Double = 0.0,
    val tcpRetransmits: Long = 0,
    val totalBytesReceived: Long = 0,
    val totalBytesSent: Long = 0,
)

@Serializable
data class ConnectionQuality(
    val overallScore: Int = 0,
    val latencyScore: Int = 0,
    val throughputScore: Int = 0,
    val stabilityScore: Int = 0,
    val rating: QualityRating = QualityRating.UNKNOWN,
    val issues: List<String> = emptyList(),
)

enum class QualityRating { EXCELLENT, GOOD, FAIR, POOR, UNUSABLE, UNKNOWN }

enum class ConnectionType { WIFI, ETHERNET, MOBILE, VPN, UNKNOWN }

@Serializable
data class ActiveConnection(
    val id: String,
    val remoteHost: String,
    val remotePort: Int,
    val localPort: Int,
    val protocol: String,
    val state: String,
    val bytesSent: Long,
    val bytesReceived: Long,
    val startTime: Long,
    val processName: String? = null,
)

@Serializable
data class SpeedTestResult(
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Double,
    val serverLocation: String,
    val serverIp: String,
    val timestamp: Long = platformCurrentTimeMillis(),
    val error: String? = null,
)

@Serializable
data class PingTestResult(
    val host: String,
    val minMs: Double,
    val maxMs: Double,
    val avgMs: Double,
    val packetLossPercent: Double,
    val timestamp: Long = platformCurrentTimeMillis(),
)

@Serializable
data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val type: NetworkType,
    val ipAddress: String,
    val gateway: String? = null,
    val dnsServers: List<String> = emptyList(),
    val isUp: Boolean,
    val speedMbps: Int? = null,
    val mtu: Int = 1500,
)

enum class NetworkType { WIFI, ETHERNET, MOBILE, VPN, UNKNOWN }
