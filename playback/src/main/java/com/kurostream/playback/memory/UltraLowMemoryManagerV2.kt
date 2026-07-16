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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.annotation.Keep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
@Keep
class UltraLowMemoryManagerV2 @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryClass = activityManager.memoryClass
    private val largeMemoryClass = activityManager.largeMemoryClass
    private val isLowRamDevice = activityManager.isLowRamDevice

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentConfig: MemoryConfig? = null
    private var lastTrimTime = 0L
    private val trimCooldownMs = 5000L

    private val _memoryPressure = MutableStateFlow(MemoryPressure.NONE)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure

    private val _availableMemory = MutableStateFlow(0L)
    val availableMemory: StateFlow<Long> = _availableMemory

    data class MemoryConfig(
        val p2pBufferSize: Int,
        val decoderBuffer: Int,
        val upscalerBuffer: Int,
        val audioBuffer: Int,
        val networkBuffer: Int,
        val uiBuffer: Int,
        val maxPeers: Int,
        val chunkSize: Int,
        val prebufferChunks: Int,
        val framePoolSize: Int,
        val compressedCacheSize: Int,
        val enableZeroCopy: Boolean
    )

    enum class MemoryPressure {
        NONE, LOW, MODERATE, HIGH, CRITICAL
    }

    enum class VideoQuality {
        LD_360P,
        SD_480P,
        HD_720P,
        FHD_1080P,
        UHD_4K
    }

    init {
        startMemoryMonitoring()
    }

    fun getOptimizedConfig(
        quality: VideoQuality,
        hasUpscaling: Boolean,
        hasTranscoding: Boolean,
        networkSpeedMbps: Long = 10,
        thermalState: ThermalState = ThermalState.NORMAL
    ): MemoryConfig {
        val targetBudget = calculateTargetBudget(thermalState)
        currentConfig = when {
            quality == VideoQuality.UHD_4K && hasUpscaling && hasTranscoding -> {
                getUltraOptimized4KConfig(targetBudget, networkSpeedMbps, thermalState)
            }
            quality == VideoQuality.UHD_4K && hasUpscaling -> {
                getOptimized1080pTo4KConfig(targetBudget, networkSpeedMbps, thermalState)
            }
            quality == VideoQuality.UHD_4K -> {
                getOptimized4KDirectConfig(targetBudget, networkSpeedMbps, thermalState)
            }
            quality == VideoQuality.FHD_1080P && hasUpscaling -> {
                getOptimized1080pUpscaleConfig(targetBudget, networkSpeedMbps, thermalState)
            }
            else -> {
                getStandardConfig(targetBudget, quality, networkSpeedMbps, thermalState)
            }
        }
        return currentConfig!!
    }

    private fun calculateTargetBudget(thermalState: ThermalState): Int {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        _availableMemory.value = info.availMem

        val availableMB = info.availMem / 1024 / 1024
        val totalMB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.totalMem / 1024 / 1024
        } else {
            memoryClass
        }

        val baseThreshold = when {
            totalMB < 512 -> 0.35
            totalMB < 1024 -> 0.45
            totalMB < 1536 -> 0.55
            totalMB < 2048 -> 0.65
            totalMB < 3072 -> 0.70
            else -> 0.75
        }

        val thermalMultiplier = when (thermalState) {
            ThermalState.NORMAL -> 1.0
            ThermalState.LIGHT -> 0.85
            ThermalState.MODERATE -> 0.70
            ThermalState.SEVERE -> 0.55
            ThermalState.CRITICAL -> 0.40
        }

        val networkMultiplier = when {
            networkSpeedMbps < 5 -> 0.75
            networkSpeedMbps < 10 -> 0.85
            networkSpeedMbps < 25 -> 0.95
            else -> 1.0
        }

        val adjustedThreshold = baseThreshold * thermalMultiplier * networkMultiplier
        val target = (availableMB * adjustedThreshold * 1024 * 1024).toLong()
        return min(target.toInt(), totalMB * 1024 * 1024 / 3)
    }

    private fun getUltraOptimized4KConfig(
        budget: Int,
        networkSpeedMbps: Long,
        thermalState: ThermalState
    ): MemoryConfig {
        val thermalFactor = when (thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.LIGHT -> 0.85f
            ThermalState.MODERATE -> 0.65f
            ThermalState.SEVERE -> 0.5f
            ThermalState.CRITICAL -> 0.35f
        }

        val p2pBuffer = min((8 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.12)
        val decoderBuffer = min((12 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.18)
        val upscalerBuffer = min((14 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.20)
        val audioBuffer = min((4 * 1024 * 1024).toInt(), budget * 0.05)
        val networkBuffer = min((2 * 1024 * 1024).toInt(), budget * 0.03)
        val uiBuffer = min((4 * 1024 * 1024).toInt(), budget * 0.04)
        val compressedCache = min((6 * 1024 * 1024).toInt(), budget * 0.08)
        val framePool = min((4 * 1024 * 1024).toInt(), budget * 0.05)

        return MemoryConfig(
            p2pBufferSize = p2pBuffer,
            decoderBuffer = decoderBuffer,
            upscalerBuffer = upscalerBuffer,
            audioBuffer = audioBuffer,
            networkBuffer = networkBuffer,
            uiBuffer = uiBuffer,
            maxPeers = when (thermalState) {
                ThermalState.CRITICAL -> 4
                ThermalState.SEVERE -> 6
                ThermalState.MODERATE -> 8
                ThermalState.LIGHT -> 10
                else -> 12
            },
            chunkSize = 1024 * 1024,
            prebufferChunks = 2,
            framePoolSize = framePool,
            compressedCacheSize = compressedCache,
            enableZeroCopy = true
        )
    }

    private fun getOptimized1080pTo4KConfig(
        budget: Int,
        networkSpeedMbps: Long,
        thermalState: ThermalState
    ): MemoryConfig {
        val thermalFactor = when (thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.LIGHT -> 0.9f
            ThermalState.MODERATE -> 0.75f
            ThermalState.SEVERE -> 0.6f
            ThermalState.CRITICAL -> 0.45f
        }

        val p2pBuffer = min((6 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.10)
        val decoderBuffer = min((10 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.15)
        val upscalerBuffer = min((12 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.18)
        val audioBuffer = min((3 * 1024 * 1024).toInt(), budget * 0.04)
        val networkBuffer = min((2 * 1024 * 1024).toInt(), budget * 0.03)
        val uiBuffer = min((3 * 1024 * 1024).toInt(), budget * 0.04)
        val compressedCache = min((5 * 1024 * 1024).toInt(), budget * 0.07)
        val framePool = min((3 * 1024 * 1024).toInt(), budget * 0.04)

        return MemoryConfig(
            p2pBufferSize = p2pBuffer,
            decoderBuffer = decoderBuffer,
            upscalerBuffer = upscalerBuffer,
            audioBuffer = audioBuffer,
            networkBuffer = networkBuffer,
            uiBuffer = uiBuffer,
            maxPeers = when (thermalState) {
                ThermalState.CRITICAL -> 3
                ThermalState.SEVERE -> 5
                ThermalState.MODERATE -> 7
                ThermalState.LIGHT -> 9
                else -> 10
            },
            chunkSize = 1024 * 1024,
            prebufferChunks = 3,
            framePoolSize = framePool,
            compressedCacheSize = compressedCache,
            enableZeroCopy = true
        )
    }

    private fun getOptimized4KDirectConfig(
        budget: Int,
        networkSpeedMbps: Long,
        thermalState: ThermalState
    ): MemoryConfig {
        val thermalFactor = when (thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.LIGHT -> 0.9f
            ThermalState.MODERATE -> 0.75f
            ThermalState.SEVERE -> 0.6f
            ThermalState.CRITICAL -> 0.45f
        }

        val p2pBuffer = min((10 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.14)
        val decoderBuffer = min((14 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.20)
        val audioBuffer = min((4 * 1024 * 1024).toInt(), budget * 0.05)
        val networkBuffer = min((3 * 1024 * 1024).toInt(), budget * 0.04)
        val uiBuffer = min((4 * 1024 * 1024).toInt(), budget * 0.05)
        val compressedCache = min((8 * 1024 * 1024).toInt(), budget * 0.10)
        val framePool = min((5 * 1024 * 1024).toInt(), budget * 0.07)

        return MemoryConfig(
            p2pBufferSize = p2pBuffer,
            decoderBuffer = decoderBuffer,
            upscalerBuffer = 0,
            audioBuffer = audioBuffer,
            networkBuffer = networkBuffer,
            uiBuffer = uiBuffer,
            maxPeers = when (thermalState) {
                ThermalState.CRITICAL -> 5
                ThermalState.SEVERE -> 8
                ThermalState.MODERATE -> 10
                ThermalState.LIGHT -> 12
                else -> 15
            },
            chunkSize = 2 * 1024 * 1024,
            prebufferChunks = 3,
            framePoolSize = framePool,
            compressedCacheSize = compressedCache,
            enableZeroCopy = true
        )
    }

    private fun getOptimized1080pUpscaleConfig(
        budget: Int,
        networkSpeedMbps: Long,
        thermalState: ThermalState
    ): MemoryConfig {
        val thermalFactor = when (thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.LIGHT -> 0.9f
            ThermalState.MODERATE -> 0.75f
            ThermalState.SEVERE -> 0.6f
            ThermalState.CRITICAL -> 0.45f
        }

        val p2pBuffer = min((4 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.08)
        val decoderBuffer = min((8 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.12)
        val upscalerBuffer = min((10 * 1024 * 1024 * thermalFactor).toInt(), budget * 0.15)
        val audioBuffer = min((2 * 1024 * 1024).toInt(), budget * 0.03)
        val networkBuffer = min((1 * 1024 * 1024).toInt(), budget * 0.02)
        val uiBuffer = min((2 * 1024 * 1024).toInt(), budget * 0.03)
        val compressedCache = min((3 * 1024 * 1024).toInt(), budget * 0.05)
        val framePool = min((2 * 1024 * 1024).toInt(), budget * 0.03)

        return MemoryConfig(
            p2pBufferSize = p2pBuffer,
            decoderBuffer = decoderBuffer,
            upscalerBuffer = upscalerBuffer,
            audioBuffer = audioBuffer,
            networkBuffer = networkBuffer,
            uiBuffer = uiBuffer,
            maxPeers = when (thermalState) {
                ThermalState.CRITICAL -> 3
                ThermalState.SEVERE -> 4
                ThermalState.MODERATE -> 6
                ThermalState.LIGHT -> 8
                else -> 10
            },
            chunkSize = 512 * 1024,
            prebufferChunks = 4,
            framePoolSize = framePool,
            compressedCacheSize = compressedCache,
            enableZeroCopy = true
        )
    }

    private fun getStandardConfig(
        budget: Int,
        quality: VideoQuality,
        networkSpeedMbps: Long,
        thermalState: ThermalState
    ): MemoryConfig {
        val thermalFactor = when (thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.LIGHT -> 0.9f
            ThermalState.MODERATE -> 0.75f
            ThermalState.SEVERE -> 0.6f
            ThermalState.CRITICAL -> 0.45f
        }

        val baseBuffer = when (quality) {
            VideoQuality.UHD_4K -> 10 * 1024 * 1024
            VideoQuality.FHD_1080P -> 5 * 1024 * 1024
            VideoQuality.HD_720P -> 3 * 1024 * 1024
            VideoQuality.SD_480P -> 2 * 1024 * 1024
            else -> 1 * 1024 * 1024
        }

        val p2pBuffer = min((baseBuffer * thermalFactor).toInt(), budget * 0.12)
        val decoderBuffer = min((baseBuffer * 1.2 * thermalFactor).toInt(), budget * 0.16)
        val networkBuffer = min((baseBuffer * 0.3).toInt(), budget * 0.04)
        val uiBuffer = min((3 * 1024 * 1024).toInt(), budget * 0.04)
        val compressedCache = min((4 * 1024 * 1024).toInt(), budget * 0.06)
        val framePool = min((2 * 1024 * 1024).toInt(), budget * 0.03)

        return MemoryConfig(
            p2pBufferSize = p2pBuffer,
            decoderBuffer = decoderBuffer,
            upscalerBuffer = 0,
            audioBuffer = min((2 * 1024 * 1024).toInt(), budget * 0.03),
            networkBuffer = networkBuffer,
            uiBuffer = uiBuffer,
            maxPeers = when (quality) {
                VideoQuality.UHD_4K -> when (thermalState) {
                    ThermalState.CRITICAL -> 4
                    ThermalState.SEVERE -> 6
                    ThermalState.MODERATE -> 8
                    else -> 10
                }
                VideoQuality.FHD_1080P -> when (thermalState) {
                    ThermalState.CRITICAL -> 3
                    ThermalState.SEVERE -> 5
                    ThermalState.MODERATE -> 7
                    else -> 10
                }
                else -> 6
            },
            chunkSize = when (quality) {
                VideoQuality.UHD_4K -> 1024 * 1024
                VideoQuality.FHD_1080P -> 512 * 1024
                else -> 256 * 1024
            },
            prebufferChunks = 4,
            framePoolSize = framePool,
            compressedCacheSize = compressedCache,
            enableZeroCopy = true
        )
    }

    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                val info = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(info)
                _availableMemory.value = info.availMem

                val pressure = when {
                    info.availMem < info.totalMem * 0.05 -> MemoryPressure.CRITICAL
                    info.availMem < info.totalMem * 0.10 -> MemoryPressure.HIGH
                    info.availMem < info.totalMem * 0.15 -> MemoryPressure.MODERATE
                    info.availMem < info.totalMem * 0.25 -> MemoryPressure.LOW
                    else -> MemoryPressure.NONE
                }

                if (pressure != _memoryPressure.value) {
                    _memoryPressure.value = pressure
                    if (pressure.ordinal >= MemoryPressure.MODERATE.ordinal) {
                        autoTrim(pressure)
                    }
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun autoTrim(pressure: MemoryPressure) {
        val now = SystemClock.uptimeMillis()
        if (now - lastTrimTime < trimCooldownMs) return
        lastTrimTime = now

        val reduction = when (pressure) {
            MemoryPressure.CRITICAL -> 0.6f
            MemoryPressure.HIGH -> 0.45f
            MemoryPressure.MODERATE -> 0.3f
            MemoryPressure.LOW -> 0.15f
            else -> 0f
        }

        currentConfig?.let { config ->
            val newConfig = config.copy(
                p2pBufferSize = (config.p2pBufferSize * (1 - reduction)).toInt(),
                decoderBuffer = (config.decoderBuffer * (1 - reduction)).toInt(),
                upscalerBuffer = (config.upscalerBuffer * (1 - reduction)).toInt(),
                compressedCacheSize = (config.compressedCacheSize * (1 - reduction)).toInt(),
                framePoolSize = (config.framePoolSize * (1 - reduction)).toInt(),
                maxPeers = max(2, (config.maxPeers * (1 - reduction)).toInt())
            )
            currentConfig = newConfig
        }
    }

    fun trimMemory(level: Int) {
        val reduction = when (level) {
            ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> 0.2f
            ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.4f
            ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> 0.6f
            ActivityManager.TRIM_MEMORY_UI_HIDDEN -> 0.3f
            ActivityManager.TRIM_MEMORY_BACKGROUND -> 0.5f
            ActivityManager.TRIM_MEMORY_COMPLETE -> 0.8f
            else -> 0f
        }

        currentConfig?.let { config ->
            currentConfig = config.copy(
                p2pBufferSize = (config.p2pBufferSize * (1 - reduction)).toInt(),
                decoderBuffer = (config.decoderBuffer * (1 - reduction)).toInt(),
                upscalerBuffer = (config.upscalerBuffer * (1 - reduction)).toInt(),
                compressedCacheSize = (config.compressedCacheSize * (1 - reduction)).toInt(),
                framePoolSize = (config.framePoolSize * (1 - reduction)).toInt(),
                maxPeers = max(2, (config.maxPeers * (1 - reduction)).toInt())
            )
        }
    }

    fun getCurrentConfig(): MemoryConfig? = currentConfig

    fun getMemoryStats(): Map<String, Any> {
        val config = currentConfig ?: return emptyMap()
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return mapOf(
            "totalBudgetMB" to (config.p2pBufferSize + config.decoderBuffer + config.upscalerBuffer +
                    config.audioBuffer + config.networkBuffer + config.uiBuffer +
                    config.compressedCacheSize + config.framePoolSize) / 1024 / 1024,
            "p2pBufferMB" to config.p2pBufferSize / 1024 / 1024,
            "decoderBufferMB" to config.decoderBuffer / 1024 / 1024,
            "upscalerBufferMB" to config.upscalerBuffer / 1024 / 1024,
            "audioBufferMB" to config.audioBuffer / 1024 / 1024,
            "networkBufferMB" to config.networkBuffer / 1024 / 1024,
            "uiBufferMB" to config.uiBuffer / 1024 / 1024,
            "compressedCacheMB" to config.compressedCacheSize / 1024 / 1024,
            "framePoolMB" to config.framePoolSize / 1024 / 1024,
            "maxPeers" to config.maxPeers,
            "chunkSizeKB" to config.chunkSize / 1024,
            "prebufferChunks" to config.prebufferChunks,
            "enableZeroCopy" to config.enableZeroCopy,
            "availableMemoryMB" to (info.availMem / 1024 / 1024),
            "memoryPressure" to _memoryPressure.value.name,
            "thermalState" to getThermalState().name
        )
    }

    private fun getThermalState(): ThermalState {
        try {
            val thermalManager = context.getSystemService(Context.THERMAL_SERVICE) as? android.hardware.thermal.ThermalManager
            return when (thermalManager?.currentThermalStatus) {
                1 -> ThermalState.NORMAL
                2 -> ThermalState.LIGHT
                3 -> ThermalState.MODERATE
                4 -> ThermalState.SEVERE
                5 -> ThermalState.CRITICAL
                else -> ThermalState.NORMAL
            }
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
    }

    enum class ThermalState {
        NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL
    }
}