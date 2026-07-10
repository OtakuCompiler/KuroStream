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

package com.kurostream.app.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.common.result.Result
import com.kurostream.domain.network.NetworkMonitorRepository
import com.kurostream.domain.network.NetworkStats
import com.kurostream.domain.network.ConnectionQuality
import com.kurostream.domain.network.SpeedTestResult
import com.kurostream.domain.network.PingTestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkDashboardViewModel @Inject constructor(
    private val repository: NetworkMonitorRepository,
) : ViewModel() {

    private val _networkStats = MutableStateFlow<NetworkStats>(NetworkStats(
        downloadSpeedMbps = 0.0,
        uploadSpeedMbps = 0.0,
        latencyMs = 0.0,
        packetLossPercent = 0.0,
        jitterMs = 0.0,
        wifiSignalStrengthDbm = null,
        wifiLinkSpeedMbps = null,
        wifiFrequencyMhz = null,
        activeInterface = null,
        connectionType = com.kurostream.domain.network.ConnectionType.UNKNOWN,
        isMetered = false,
        isVpnActive = false,
        dnsLatencyMs = 0.0,
        tcpRetransmits = 0,
        bytesReceived = 0,
        bytesSent = 0,
    ))
    val networkStats = _networkStats.asStateFlow()

    private val _connectionQuality = MutableStateFlow<ConnectionQuality>(ConnectionQuality(
        overallScore = 0,
        latencyScore = 0,
        throughputScore = 0,
        stabilityScore = 0,
        rating = com.kurostream.domain.network.QualityRating.UNKNOWN,
        issues = emptyList(),
    ))
    val connectionQuality = _connectionQuality.asStateFlow()

    private val _speedTestResult = MutableStateFlow<Result<SpeedTestResult>?>(null)
    val speedTestResult = _speedTestResult.asStateFlow()

    private val _pingTestResult = MutableStateFlow<Result<PingTestResult>?>(null)
    val pingTestResult = _pingTestResult.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            repository.startMonitoring()
        }

        viewModelScope.launch {
            repository.observeNetworkStats().collect { stats ->
                _networkStats.value = stats
                updateConnectionQuality(stats)
            }
        }
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
            overallScore >= 80 -> com.kurostream.domain.network.QualityRating.EXCELLENT
            overallScore >= 60 -> com.kurostream.domain.network.QualityRating.GOOD
            overallScore >= 40 -> com.kurostream.domain.network.QualityRating.FAIR
            overallScore >= 20 -> com.kurostream.domain.network.QualityRating.POOR
            else -> com.kurostream.domain.network.QualityRating.UNUSABLE
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

    fun runSpeedTest() {
        viewModelScope.launch {
            _speedTestResult.value = Result.loading()
            val result = repository.runSpeedTest()
            _speedTestResult.value = result
        }
    }

    fun runPingTest(host: String = "8.8.8.8") {
        viewModelScope.launch {
            _pingTestResult.value = Result.loading()
            val result = repository.runPingTest(host)
            _pingTestResult.value = result
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.stopMonitoring()
        }
    }
}