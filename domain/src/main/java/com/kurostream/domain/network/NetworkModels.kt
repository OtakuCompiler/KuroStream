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

package com.kurostream.domain.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitorRepository {
    val networkStats: Flow<NetworkStats>
    val connectionQuality: Flow<ConnectionQuality>
    val activeInterfaces: Flow<List<NetworkInterfaceInfo>>

    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    suspend fun runSpeedTest(): SpeedTestResult
    suspend fun runPingTest(host: String = "8.8.8.8"): PingTestResult
    suspend fun getNetworkInterfaceInfo(): List<NetworkInterfaceInfo>
}

@Serializable
data class NetworkStats(
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val latencyMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val jitterMs: Double = 0.0,
    val wifiSignalStrengthDbm: Int? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val activeInterface: String? = null,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val isMetered: Boolean = false,
    val isVpnActive: Boolean = false,
    val dnsLatencyMs: Double = 0.0,
    val tcpRetransmits: Long = 0,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
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
data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val type: NetworkType,
    val ipAddress: String,
    val gateway: String?,
    val dnsServers: List<String>,
    val isUp: Boolean,
    val speedMbps: Int?,
    val mtu: Int,
)

enum class NetworkType { WIFI, ETHERNET, MOBILE, VPN, UNKNOWN }

@Serializable
data class SpeedTestResult(
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Double,
    val serverLocation: String,
    val serverIp: String,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null,
)

@Serializable
data class PingTestResult(
    val host: String,
    val minMs: Double,
    val maxMs: Double,
    val avgMs: Double,
    val packetLossPercent: Double,
    val timestamp: Long = System.currentTimeMillis(),
)