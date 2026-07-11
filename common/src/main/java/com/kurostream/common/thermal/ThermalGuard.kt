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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.ref.WeakReference

/**
 * Real-time thermal monitoring for Fire TV Stick HD and other Android TV devices.
 * Reads temperature from thermal zones and exposes reactive state for throttling decisions.
 *
 * Target: Keep device ≤ 35°C. Throttling stages:
 * - ≥ 33°C: Reduce decoder threads, disable AI upscaling, cap downloads, lower UI fps
 * - ≥ 35°C: Show warning, further throttle, disable frame interpolation
 */
class ThermalGuard private constructor(context: Context) : LifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    // Thermal zones paths (common on Fire TV / Amlogic devices)
    private val thermalZonePaths = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/class/thermal/thermal_zone3/temp",
        "/sys/class/thermal/thermal_zone4/temp",
    )

    // HardwarePropertiesManager fallback (API 24+)
    private val hwPropertiesManager: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE)
        } catch (e: Exception) {
            null
        }
    } else null

    // Reactive state
    private val _currentTempCelsius = MutableStateFlow(0.0)
    val currentTempCelsius: StateFlow<Double> = _currentTempCelsius.asStateFlow()

    private val _throttleStage = MutableStateFlow(ThrottleStage.NONE)
    val throttleStage: StateFlow<ThrottleStage> = _throttleStage.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Configuration
    private val warningThreshold = 33.0 // °C - start gradual throttling
    private val criticalThreshold = 35.0 // °C - aggressive throttling + warning
    private val pollingIntervalMs = 2000L // 2 seconds

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
        // Auto-start when app reaches STARTED, stop at STOPPED
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
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

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
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

    /** Read max temperature across all thermal zones (millidegrees Celsius → Celsius) */
    private fun readMaxTemperature(): Double {
        var maxTemp = 0.0

        // Primary: read from sysfs thermal zones
        for (path in thermalZonePaths) {
            try {
                File(path).bufferedReader().use { reader ->
                    val line = reader.readLine() ?: return@use
                    val milliCelsius = line.toLongOrNull() ?: 0L
                    val celsius = milliCelsius / 1000.0
                    if (celsius > maxTemp && celsius < 150.0) { // sanity check
                        maxTemp = celsius
                    }
                }
            } catch (e: Exception) {
                // Ignore unreadable zones
            }
        }

        // Fallback: HardwarePropertiesManager (API 24+)
        if (maxTemp == 0.0 && hwPropertiesManager != null) {
            try {
                maxTemp = readTempViaHardwarePropertiesManager()
            } catch (e: Exception) {
                Log.w("ThermalGuard", "HardwarePropertiesManager read failed", e)
            }
        }

        return maxTemp
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun readTempViaHardwarePropertiesManager(): Double {
        // Use reflection to avoid compile-time dependency on HardwarePropertiesManager
        val manager = hwPropertiesManager as android.os.HardwarePropertiesManager
        try {
            val temps = manager.deviceTemperatures
            var max = 0.0
            for (temp in temps) {
                // Temperature in millikelvin → Celsius
                val celsius = (temp.value / 1000.0) - 273.15
                if (celsius > max && celsius < 150.0) max = celsius
            }
            return max
        } catch (e: Exception) {
            return 0.0
        }
    }

    /** Evaluate throttle stage based on current temperature */
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

    /** Called on main thread when throttle stage changes */
    private fun onThrottleStageChanged(stage: ThrottleStage) {
        handler.post {
            when (stage) {
                ThrottleStage.WARNING -> {
                    Log.w("ThermalGuard", "⚠️ Temperature ${_currentTempCelsius.value}°C ≥ $warningThreshold°C — Starting throttling")
                    ThermalThrottleCallback.onWarningStage()
                }
                ThrottleStage.CRITICAL -> {
                    val now = System.currentTimeMillis()
                    if (now - lastWarningShown > 30000) { // Show toast max once per 30s
                        lastWarningShown = now
                        Log.e("ThermalGuard", "🔥 CRITICAL: Temperature ${_currentTempCelsius.value}°C ≥ $criticalThreshold°C — Aggressive throttling active")
                        ThermalThrottleCallback.onCriticalStage()
                    }
                }
                ThrottleStage.NONE -> {
                    Log.i("ThermalGuard", "✅ Temperature normalized: ${_currentTempCelsius.value}°C")
                    ThermalThrottleCallback.onNormalized()
                }
            }
        }
    }

    /** Get current throttle configuration for a specific component */
    fun getThrottleConfig(component: ThrottleComponent): ThrottleConfig {
        return when (_throttleStage.value) {
            ThrottleStage.NONE -> ThrottleConfig.NONE
            ThrottleStage.WARNING -> ThrottleConfig.WARNING_CONFIGS[component] ?: ThrottleConfig.NONE
            ThrottleStage.CRITICAL -> ThrottleConfig.CRITICAL_CONFIGS[component] ?: ThrottleConfig.NONE
        }
    }
}

/** Throttle severity stages */
enum class ThrottleStage {
    NONE,      // < 33°C - Full performance
    WARNING,   // 33-35°C - Gradual throttling
    CRITICAL   // ≥ 35°C - Aggressive throttling + user warning
}

/** Components that can be throttled */
enum class ThrottleComponent {
    DECODER_THREADS,      // FFmpeg/MPV decoder thread count
    AI_UPSCALING,         // ESRGAN/RealESRGAN upscaling
    FRAME_INTERPOLATION,  // RIFE frame interpolation
    DOWNLOAD_CONNECTIONS, // Parallel download connections
    UI_ANIMATION_FPS,     // Compose animation frame rate
    AUDIO_DSP_QUALITY,    // Sonic DSP quality level
    SUBTITLE_RENDERING,   // libass glyph cache / quality
}

/** Throttle configuration per component per stage */
data class ThrottleConfig(
    val decoderThreadCount: Int = -1,           // -1 = auto/unlimited
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

/** Callback interface for components to react to thermal changes */
interface ThermalThrottleCallback {
    companion object {
        private var listener: ThermalThrottleCallback? = null

        fun register(callback: ThermalThrottleCallback) {
            listener = callback
        }

        fun unregister() {
            listener = null
        }

        private fun onWarningStage() = listener?.onWarningStage()
        private fun onCriticalStage() = listener?.onCriticalStage()
        private fun onNormalized() = listener?.onNormalized()
    }

    fun onWarningStage()
    fun onCriticalStage()
    fun onNormalized()
}

// Compose helper moved to app module to avoid ui dependency in common