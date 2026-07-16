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

package com.kurostream.playback.memory

import android.content.Context
import android.hardware.thermal.ThermalManager
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class ThermalQualityController @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ThermalQualityController"
        private const val MONITOR_INTERVAL_MS = 1000
        private const val THERMAL_HISTORY_SIZE = 60
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val thermalManager = context.getSystemService(Context.THERMAL_SERVICE) as? ThermalManager
    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState

    private val qualityConfigs = ConcurrentHashMap<String, QualityConfig>()
    private val qualityStates = ConcurrentHashMap<String, QualityState>()
    private val thermalHistory = java.util.concurrent.ConcurrentLinkedDeque<ThermalSnapshot>()
    private var monitoring = false

    data class QualityConfig(
        val streamId: String,
        val maxQuality: VideoQuality = VideoQuality.UHD_4K,
        val minQuality: VideoQuality = VideoQuality.HD_720P,
        val currentQuality: VideoQuality = VideoQuality.FHD_1080P,
        val enableAutoScaling: Boolean = true,
        val upscalingEnabled: Boolean = true,
        val atmosEnabled: Boolean = false,
        val targetFps: Int = 60,
        val maxBitrateKbps: Int = 25000
    )

    data class QualityState(
        val streamId: String,
        var currentQuality: VideoQuality,
        var currentBitrateKbps: Int,
        var currentFps: Int,
        var upscalingActive: Boolean,
        var atmosActive: Boolean,
        var lastChangeReason: String = "",
        var lastChangeTime: Long = 0
    )

    enum class VideoQuality {
        LD_360P(360, 640, 800),
        SD_480P(480, 854, 1200),
        HD_720P(720, 1280, 3000),
        FHD_1080P(1080, 1920, 8000),
        UHD_4K(2160, 3840, 25000);

        val height: Int
        val width: Int
        val typicalBitrateKbps: Int

        constructor(height: Int, width: Int, typicalBitrateKbps: Int) {
            this.height = height
            this.width = width
            this.typicalBitrateKbps = typicalBitrateKbps
        }

        fun getLower(): VideoQuality = when (this) {
            UHD_4K -> FHD_1080P
            FHD_1080P -> HD_720P
            HD_720P -> SD_480P
            SD_480P -> LD_360P
            else -> this
        }

        fun getHigher(): VideoQuality = when (this) {
            LD_360P -> SD_480P
            SD_480P -> HD_720P
            HD_720P -> FHD_1080P
            FHD_1080P -> UHD_4K
            else -> this
        }
    }

    enum class ThermalState {
        NORMAL(0, 1.0),
        LIGHT(1, 0.9),
        MODERATE(2, 0.7),
        SEVERE(3, 0.5),
        CRITICAL(4, 0.3);

        val level: Int
        val qualityMultiplier: Double

        constructor(level: Int, qualityMultiplier: Double) {
            this.level = level
            this.qualityMultiplier = qualityMultiplier
        }

        companion object {
            fun fromThermalStatus(status: Int): ThermalState {
                return when (status) {
                    ThermalManager.THERMAL_STATUS_LIGHT -> LIGHT
                    ThermalManager.THERMAL_STATUS_MODERATE -> MODERATE
                    ThermalManager.THERMAL_STATUS_SEVERE -> SEVERE
                    ThermalManager.THERMAL_STATUS_CRITICAL -> CRITICAL
                    else -> NORMAL
                }
            }
        }
    }

    data class ThermalSnapshot(
        val timestamp: Long,
        val state: ThermalState,
        val cpuTemp: Double?,
        val gpuTemp: Double?,
        val skinTemp: Double?
    )

    fun registerStream(config: QualityConfig) {
        qualityConfigs[config.streamId] = config
        qualityStates[config.streamId] = QualityState(
            streamId = config.streamId,
            currentQuality = config.currentQuality,
            currentBitrateKbps = config.currentQuality.typicalBitrateKbps,
            currentFps = config.targetFps,
            upscalingActive = config.upscalingEnabled,
            atmosActive = config.atmosEnabled
        )
        
        if (!monitoring) {
            startMonitoring()
        }
        
        Log.d(TAG, "Registered stream ${config.streamId} with quality ${config.currentQuality}")
    }

    fun unregisterStream(streamId: String) {
        qualityConfigs.remove(streamId)
        qualityStates.remove(streamId)
    }

    private fun startMonitoring() {
        monitoring = true
        scope.launch {
            while (monitoring) {
                updateThermalState()
                applyThermalThrottling()
                kotlinx.coroutines.delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    private fun updateThermalState() {
        val state = if (thermalManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ThermalState.fromThermalStatus(thermalManager.currentThermalStatus)
            } catch (e: Exception) {
                ThermalState.NORMAL
            }
        } else {
            // Fallback: estimate from CPU temp
            estimateThermalFromCpu()
        }

        if (state != _thermalState.value) {
            Log.i(TAG, "Thermal state changed: ${_thermalState.value} -> $state")
            _thermalState.value = state
            
            thermalHistory.addLast(ThermalSnapshot(
                timestamp = System.currentTimeMillis(),
                state = state,
                cpuTemp = readCpuTemp(),
                gpuTemp = null,
                skinTemp = null
            ))
            
            while (thermalHistory.size > THERMAL_HISTORY_SIZE) {
                thermalHistory.removeFirst()
            }
        }
    }

    private fun estimateThermalFromCpu(): ThermalState {
        val temp = readCpuTemp()
        return when {
            temp == null -> ThermalState.NORMAL
            temp < 50.0 -> ThermalState.NORMAL
            temp < 60.0 -> ThermalState.LIGHT
            temp < 70.0 -> ThermalState.MODERATE
            temp < 80.0 -> ThermalState.SEVERE
            else -> ThermalState.CRITICAL
        }
    }

    private fun readCpuTemp(): Double? {
        try {
            val files = java.io.File("/sys/class/thermal").listFiles()
            var maxTemp = 0.0
            var found = false
            
            files?.forEach { zone ->
                val tempFile = java.io.File(zone, "temp")
                if (tempFile.exists()) {
                    val temp = tempFile.readText().trim().toDoubleOrNull()
                    if (temp != null) {
                        maxTemp = max(maxTemp, temp / 1000.0)
                        found = true
                    }
                }
            }
            
            if (found) maxTemp else null
        } catch (e: Exception) {
            null
        }
    }

    private fun applyThermalThrottling() {
        val state = _thermalState.value
        val multiplier = state.qualityMultiplier
        
        qualityStates.forEach { (streamId, qualityState) ->
            val config = qualityConfigs[streamId]
            if (config == null || !config.enableAutoScaling) return@forEach
            
            val newQuality = calculateOptimalQuality(config, qualityState, multiplier)
            val newBitrate = calculateOptimalBitrate(config, newQuality, multiplier)
            val newFps = calculateOptimalFps(config, multiplier)
            val newUpscaling = config.upscalingEnabled && state.level <= ThermalState.MODERATE.level
            val newAtmos = config.atmosEnabled && state.level <= ThermalState.LIGHT.level
            
            if (newQuality != qualityState.currentQuality ||
                newBitrate != qualityState.currentBitrateKbps ||
                newFps != qualityState.currentFps ||
                newUpscaling != qualityState.upscalingActive ||
                newAtmos != qualityState.atmosActive) {
                
                val oldQuality = qualityState.currentQuality
                qualityState.currentQuality = newQuality
                qualityState.currentBitrateKbps = newBitrate
                qualityState.currentFps = newFps
                qualityState.upscalingActive = newUpscaling
                qualityState.atmosActive = newAtmos
                qualityState.lastChangeReason = "Thermal: ${state.name}, multiplier=${String.format("%.0f%%", multiplier * 100)}"
                qualityState.lastChangeTime = System.currentTimeMillis()
                
                Log.i(TAG, "Stream $streamId quality changed: $oldQuality -> $newQuality (bitrate: ${newBitrate}kbps, fps: $newFps, upscale: $newUpscaling, atmos: $newAtmos)")
            }
        }
    }

    private fun calculateOptimalQuality(config: QualityConfig, state: QualityState, multiplier: Double): VideoQuality {
        var targetQuality = config.currentQuality
        
        // Calculate based on thermal multiplier and available headroom
        val maxAllowedBitrate = (config.maxBitrateKbps * multiplier).toInt()
        val targetBitrate = (state.currentBitrateKbps * multiplier).toInt()
        
        // Find highest quality that fits
        var candidate = config.maxQuality
        while (candidate != config.minQuality && candidate.typicalBitrateKbps > maxAllowedBitrate) {
            candidate = candidate.getLower()
        }
        
        // Don't upgrade if we're already at or above target
        if (candidate.ordinal > targetQuality.ordinal) {
            // Only upgrade if thermal state is improving and we have headroom
            if (state.currentQuality == candidate.getLower() && multiplier > 0.8) {
                targetQuality = candidate
            }
        } else if (candidate.ordinal < targetQuality.ordinal) {
            // Downgrade aggressively
            targetQuality = candidate
        }
        
        return targetQuality.coerceIn(config.minQuality, config.maxQuality)
    }

    private fun calculateOptimalBitrate(config: QualityConfig, quality: VideoQuality, multiplier: Double): Int {
        val baseBitrate = quality.typicalBitrateKbps
        val thermalBitrate = (baseBitrate * multiplier).toInt()
        return min(thermalBitrate, config.maxBitrateKbps)
    }

    private fun calculateOptimalFps(config: QualityConfig, multiplier: Double): Int {
        return when {
            multiplier >= 0.9 -> config.targetFps
            multiplier >= 0.7 -> 30
            multiplier >= 0.5 -> 24
            else -> 20
        }
    }

    fun getCurrentQuality(streamId: String): QualityState? {
        return qualityStates[streamId]
    }

    fun forceQuality(streamId: String, quality: VideoQuality, reason: String = "Manual override") {
        val config = qualityConfigs[streamId]
        val state = qualityStates[streamId]
        if (config != null && state != null) {
            state.currentQuality = quality.coerceIn(config.minQuality, config.maxQuality)
            state.currentBitrateKbps = quality.typicalBitrateKbps
            state.lastChangeReason = reason
            state.lastChangeTime = System.currentTimeMillis()
            Log.i(TAG, "Forced quality for $streamId: $quality ($reason)")
        }
    }

    fun getThermalStats(): Map<String, Any> {
        val currentState = _thermalState.value
        val recentStates = thermalHistory.toList()
        val stateCounts = recentStates.groupBy { it.state }.mapValues { (_, v) -> v.size }
        
        return mapOf(
            "currentState" to currentState.name,
            "currentMultiplier" to String.format("%.0f%%", currentState.qualityMultiplier * 100),
            "historySize" to thermalHistory.size,
            "stateDistribution" to stateCounts.mapValues { (k, v) -> "$k: $v" },
            "avgCpuTemp" to recentStates.mapNotNull { it.cpuTemp }.average().let { if (it.isNaN()) "N/A" else String.format("%.1f°C", it) },
            "streams" to qualityStates.values.map { state ->
                mapOf(
                    "streamId" to state.streamId,
                    "quality" to state.currentQuality.name,
                    "bitrateKbps" to state.currentBitrateKbps,
                    "fps" to state.currentFps,
                    "upscaling" to state.upscalingActive,
                    "atmos" to state.atmosActive,
                    "lastChange" to state.lastChangeReason
                )
            }
        )
    }

    fun getThermalHistory(): List<ThermalSnapshot> = thermalHistory.toList()

    fun shutdown() {
        monitoring = false
        scope.coroutineContext[Job]?.cancel()
    }
}