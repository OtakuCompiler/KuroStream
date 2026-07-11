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
import kotlinx.serialization.Serializable

interface NetworkMonitorRepository {
    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    fun observeNetworkStats(): Flow<NetworkStats>
    fun observeConnectionQuality(): Flow<ConnectionQuality>
    fun observeActiveConnections(): Flow<List<ActiveConnection>>
    suspend fun runSpeedTest(): SpeedTestResult
    suspend fun runPingTest(host: String): PingTestResult
    suspend fun getNetworkInterfaceInfo(): List<NetworkInterfaceInfo>
}

@Serializable
data class NetworkStats(
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double,
    val wifiSignalStrengthDbm: Int?,
    val wifiLinkSpeedMbps: Int?,
    val networkType: NetworkType,
    val isMetered: Boolean,
    val totalBytesReceived: Long,
    val totalBytesSent: Long,
)

@Serializable
data class ConnectionQuality(
    val overallScore: Int, // 0-100
    val latencyScore: Int,
    val throughputScore: Int,
    val stabilityScore: Int,
    val rating: QualityRating,
    val issues: List<String> = emptyList(),
)

enum class QualityRating { EXCELLENT, GOOD, FAIR, POOR, UNUSABLE }

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
    val processName: String?,
)

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

@Serializable
data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val type: NetworkType,
    val ipAddress: String,
    val gateway: String?,
    val dnsServers: List<String>,
    val isUp: Boolean,
    val speedMbps: Long?,
    val mtu: Int,
)

enum class NetworkType { WIFI, ETHERNET, MOBILE, VPN, UNKNOWN }