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

package com.kurostream.common.thermal

import android.content.Context
import android.os.Build
import timber.log.Timber
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Real-time thermal monitoring for Fire TV Stick HD and other Android TV devices.
 * Reads temperature from thermal zones and exposes reactive state for throttling decisions.
 */
class ThermalGuard private constructor(context: Context) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val thermalZonePaths = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/class/thermal/thermal_zone3/temp",
        "/sys/class/thermal/thermal_zone4/temp",
    )

    private val hwPropertiesManager: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE)
        } catch (e: Exception) {
            null
        }
    } else null

    private val _currentTempCelsius = MutableStateFlow(0.0)
    val currentTempCelsius: StateFlow<Double> = _currentTempCelsius.asStateFlow()

    private val _throttleStage = MutableStateFlow(ThrottleStage.NONE)
    val throttleStage: StateFlow<ThrottleStage> = _throttleStage.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val warningThreshold = 33.0
    private val criticalThreshold = 35.0
    private val pollingIntervalMs = 2000L

    private var monitoringJob: Job? = null
    private var lastWarningShown = 0L

    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: ThermalGuard? = null

        fun getInstance(context: Context): ThermalGuard {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThermalGuard(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }

    fun startMonitoring() {
        if (_isMonitoring.value) return
        _isMonitoring.value = true

        monitoringJob = scope.launch {
            while (isActive) {
                val temp = readMaxTemperature()
                if (temp > 0) {
                    _currentTempCelsius.value = temp
                    evaluateThrottleStage(temp)
                }
                kotlinx.coroutines.delay(pollingIntervalMs)
            }
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun shutdown() {
        stopMonitoring()
        scope.coroutineContext.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    private fun readMaxTemperature(): Double {
        var maxTemp = 0.0

        for (path in thermalZonePaths) {
            maxTemp = readSysfsTemperature(path, maxTemp)
        }

        if (maxTemp == 0.0 && hwPropertiesManager != null) {
            maxTemp = readHardwarePropertiesTemperature()
        }

        return maxTemp
    }

    private fun readSysfsTemperature(path: String, currentMax: Double): Double {
        return try {
            val file = File(path)
            if (!file.exists()) return currentMax
            file.bufferedReader().use { reader ->
                val line = reader.readLine() ?: return@use currentMax
                val milliCelsius = line.toLongOrNull() ?: 0L
                val celsius = milliCelsius / 1000.0
                if (celsius > currentMax && celsius < 150.0) celsius else currentMax
            }
        } catch (e: Exception) {
            currentMax
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun readHardwarePropertiesTemperature(): Double {
        val manager = hwPropertiesManager as android.os.HardwarePropertiesManager
        return try {
            val temps = manager.getDeviceTemperatures(0, 1)
            if (temps.isEmpty()) return 0.0
            var max = 0.0
            for (temp in temps) {
                val celsius = (temp.toDouble() / 1000.0) - 273.15
                if (celsius > max && celsius < 150.0) max = celsius
            }
            max
        } catch (e: Exception) {
            0.0
        }
    }

    private fun evaluateThrottleStage(tempCelsius: Double) {
        val newStage = when {
            tempCelsius >= criticalThreshold -> ThrottleStage.CRITICAL
            tempCelsius >= warningThreshold -> ThrottleStage.WARNING
            else -> ThrottleStage.NONE
        }

        if (newStage != _throttleStage.value) {
            _throttleStage.value = newStage
            onThrottleStageChanged(newStage)
        }
    }

    private fun onThrottleStageChanged(stage: ThrottleStage) {
        when (stage) {
            ThrottleStage.WARNING -> {
                Timber.tag("ThermalGuard").w("Temperature ${_currentTempCelsius.value}°C >= $warningThreshold°C — Starting throttling")
                ThermalThrottleCallback.onWarningStage()
            }
            ThrottleStage.CRITICAL -> {
                val now = System.currentTimeMillis()
                if (now - lastWarningShown > 30000) {
                    lastWarningShown = now
                    Timber.tag("ThermalGuard").e("CRITICAL: Temperature ${_currentTempCelsius.value}°C >= $criticalThreshold°C — Aggressive throttling active")
                    ThermalThrottleCallback.onCriticalStage()
                }
            }
            ThrottleStage.NONE -> {
                Timber.tag("ThermalGuard").i("Temperature normalized: ${_currentTempCelsius.value}°C")
                ThermalThrottleCallback.onNormalized()
            }
        }
    }

    fun getThrottleConfig(component: ThrottleComponent): ThrottleConfig {
        return when (_throttleStage.value) {
            ThrottleStage.NONE -> ThrottleConfig.NONE
            ThrottleStage.WARNING -> ThrottleConfig.WARNING_CONFIGS[component] ?: ThrottleConfig.NONE
            ThrottleStage.CRITICAL -> ThrottleConfig.CRITICAL_CONFIGS[component] ?: ThrottleConfig.NONE
        }
    }
}

enum class ThrottleStage {
    NONE, WARNING, CRITICAL
}

enum class ThrottleComponent {
    DECODER_THREADS, AI_UPSCALING, FRAME_INTERPOLATION, DOWNLOAD_CONNECTIONS, UI_ANIMATION_FPS, AUDIO_DSP_QUALITY, SUBTITLE_RENDERING
}

data class ThrottleConfig(
    val decoderThreadCount: Int = -1,
    val aiUpscalingEnabled: Boolean = true,
    val frameInterpolationEnabled: Boolean = true,
    val maxDownloadConnections: Int = -1,
    val uiAnimationFps: Int = 60,
    val audioDspQuality: AudioDspQuality = AudioDspQuality.HIGH,
    val subtitleRenderQuality: SubtitleRenderQuality = SubtitleRenderQuality.HIGH,
) {
    companion object {
        val NONE = ThrottleConfig()

        private val WARNING_DEFAULTS = ThrottleConfig(
            decoderThreadCount = 2,
            aiUpscalingEnabled = false,
            frameInterpolationEnabled = true,
            maxDownloadConnections = 2,
            uiAnimationFps = 30,
            audioDspQuality = AudioDspQuality.MEDIUM,
            subtitleRenderQuality = SubtitleRenderQuality.MEDIUM,
        )

        private val CRITICAL_DEFAULTS = ThrottleConfig(
            decoderThreadCount = 1,
            aiUpscalingEnabled = false,
            frameInterpolationEnabled = false,
            maxDownloadConnections = 1,
            uiAnimationFps = 24,
            audioDspQuality = AudioDspQuality.LOW,
            subtitleRenderQuality = SubtitleRenderQuality.LOW,
        )

        val WARNING_CONFIGS = mapOf(
            ThrottleComponent.DECODER_THREADS to WARNING_DEFAULTS.copy(decoderThreadCount = 2),
            ThrottleComponent.AI_UPSCALING to WARNING_DEFAULTS.copy(aiUpscalingEnabled = false),
            ThrottleComponent.FRAME_INTERPOLATION to WARNING_DEFAULTS.copy(frameInterpolationEnabled = true),
            ThrottleComponent.DOWNLOAD_CONNECTIONS to WARNING_DEFAULTS.copy(maxDownloadConnections = 2),
            ThrottleComponent.UI_ANIMATION_FPS to WARNING_DEFAULTS.copy(uiAnimationFps = 30),
            ThrottleComponent.AUDIO_DSP_QUALITY to WARNING_DEFAULTS.copy(audioDspQuality = AudioDspQuality.MEDIUM),
            ThrottleComponent.SUBTITLE_RENDERING to WARNING_DEFAULTS.copy(subtitleRenderQuality = SubtitleRenderQuality.MEDIUM),
        )

        val CRITICAL_CONFIGS = mapOf(
            ThrottleComponent.DECODER_THREADS to CRITICAL_DEFAULTS.copy(decoderThreadCount = 1),
            ThrottleComponent.AI_UPSCALING to CRITICAL_DEFAULTS.copy(aiUpscalingEnabled = false),
            ThrottleComponent.FRAME_INTERPOLATION to CRITICAL_DEFAULTS.copy(frameInterpolationEnabled = false),
            ThrottleComponent.DOWNLOAD_CONNECTIONS to CRITICAL_DEFAULTS.copy(maxDownloadConnections = 1),
            ThrottleComponent.UI_ANIMATION_FPS to CRITICAL_DEFAULTS.copy(uiAnimationFps = 24),
            ThrottleComponent.AUDIO_DSP_QUALITY to CRITICAL_DEFAULTS.copy(audioDspQuality = AudioDspQuality.LOW),
            ThrottleComponent.SUBTITLE_RENDERING to CRITICAL_DEFAULTS.copy(subtitleRenderQuality = SubtitleRenderQuality.LOW),
        )
    }
}

enum class AudioDspQuality { HIGH, MEDIUM, LOW }
enum class SubtitleRenderQuality { HIGH, MEDIUM, LOW }

interface ThermalThrottleCallback {
    fun onWarningStage()
    fun onCriticalStage()
    fun onNormalized()
}
