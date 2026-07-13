package com.kurostream.players.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalThrottlingManager @Inject constructor(
    private val context: Context,
) {
    
    companion object {
        private const val TAG = "ThermalThrottling"
        
        // Temperature thresholds in Celsius
        const val THERMAL_THRESHOLD_NORMAL = 35f
        const val THERMAL_THRESHOLD_WARM = 40f
        const val THERMAL_THRESHOLD_HOT = 45f
        const val THERMAL_THRESHOLD_CRITICAL = 50f
        const val THERMAL_THRESHOLD_EMERGENCY = 55f
    }
    
    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()
    
    private val _cpuThrottleLevel = MutableStateFlow(0)
    val cpuThrottleLevel: StateFlow<Int> = _cpuThrottleLevel.asStateFlow()
    
    private val _gpuThrottleLevel = MutableStateFlow(0)
    val gpuThrottleLevel: StateFlow<Int> = _gpuThrottleLevel.asStateFlow()
    
    private val _isThrottling = MutableStateFlow(false)
    val isThrottling: StateFlow<Boolean> = _isThrottling.asStateFlow()
    
    private val _currentTempCelsius = MutableStateFlow(0f)
    val currentTempCelsius: StateFlow<Float> = _currentTempCelsius.asStateFlow()
    
    private val _throttleRecommendations = MutableStateFlow(ThrottleRecommendations())
    val throttleRecommendations: StateFlow<ThrottleRecommendations> = _throttleRecommendations.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val thermalCallback: PowerManager.OnThermalStatusChangedListener? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PowerManager.OnThermalStatusChangedListener { status ->
            handleThermalStatus(status)
        }
    } else null
    
    private val tempMonitorExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ThermalMonitor").apply { priority = Thread.MIN_PRIORITY }
    }
    
    private var isMonitoring = false
    private var lastTempRead = 0L
    private var consecutiveHighTemps = 0
    
    init {
        startMonitoring()
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && thermalCallback != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
                powerManager.addThermalStatusListener(thermalCallback!!)
                Timber.d("Thermal listener registered")
            } catch (e: Exception) {
                Timber.w(e, "Failed to register thermal listener")
            }
        }
        
        scope.launch {
            monitorTemperatureLoop()
        }
        
        Timber.i("Thermal monitoring started")
    }
    
    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && thermalCallback != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
                powerManager.removeThermalStatusListener(thermalCallback!!)
            } catch (e: Exception) {
                Timber.w(e, "Failed to remove thermal listener")
            }
        }
        
        Timber.i("Thermal monitoring stopped")
    }
    
    private suspend fun monitorTemperatureLoop() {
        while (isMonitoring) {
            val temp = readTemperature()
            if (temp > 0) {
                updateTemperature(temp)
            }
            
            try {
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
            } catch (e: Exception) {
                break
            }
        }
    }
    
    private fun readTemperature(): Float {
        return try {
            // Try to read from thermal zones
            val thermalZoneFiles = java.io.File("/sys/class/thermal").listFiles()
                ?.filter { it.name.startsWith("thermal_zone") }
                ?.sortedBy { it.name }
            
            var maxTemp = 0f
            thermalZoneFiles?.forEach { zone ->
                val tempFile = java.io.File(zone, "temp")
                if (tempFile.exists()) {
                    val tempStr = tempFile.readText().trim()
                    val temp = tempStr.toFloatOrNull()
                    if (temp != null) {
                        val tempCelsius = if (temp > 1000) temp / 1000 else temp
                        maxTemp = kotlin.math.max(maxTemp, tempCelsius)
                    }
                }
            }
            
            if (maxTemp > 0) {
                lastTempRead = System.currentTimeMillis()
                return maxTemp
            }
            
            // Fallback: try CPU temperature
            val cpuTempFile = java.io.File("/sys/class/thermal/thermal_zone0/temp")
            if (cpuTempFile.exists()) {
                val tempStr = cpuTempFile.readText().trim()
                val temp = tempStr.toFloatOrNull()
                if (temp != null) {
                    val tempCelsius = if (temp > 1000) temp / 1000 else temp
                    lastTempRead = System.currentTimeMillis()
                    return tempCelsius
                }
            }
            
            0f
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun updateTemperature(tempCelsius: Float) {
        _currentTempCelsius.value = tempCelsius
        
        val newState = when {
            tempCelsius >= THERMAL_THRESHOLD_EMERGENCY -> ThermalState.EMERGENCY
            tempCelsius >= THERMAL_THRESHOLD_CRITICAL -> ThermalState.CRITICAL
            tempCelsius >= THERMAL_THRESHOLD_HOT -> ThermalState.HOT
            tempCelsius >= THERMAL_THRESHOLD_WARM -> ThermalState.WARM
            tempCelsius >= THERMAL_THRESHOLD_NORMAL -> ThermalState.ELEVATED
            else -> ThermalState.NORMAL
        }
        
        if (newState != _thermalState.value) {
            _thermalState.value = newState
            Timber.w("Thermal state changed: ${newState.name} (${tempCelsius}°C)")
            applyThermalPolicy(newState)
        }
        
        // Track consecutive high temps
        if (tempCelsius >= THERMAL_THRESHOLD_WARM) {
            consecutiveHighTemps++
        } else {
            consecutiveHighTemps = 0
        }
    }
    
    private fun handleThermalStatus(status: Int) {
        val state = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.ELEVATED
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.WARM
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.HOT
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.SHUTDOWN
            else -> ThermalState.NORMAL
        }
        
        if (state != _thermalState.value) {
            _thermalState.value = state
            Timber.w("System thermal status: ${state.name}")
            applyThermalPolicy(state)
        }
    }
    
    private fun applyThermalPolicy(state: ThermalState) {
        val recommendations = when (state) {
            ThermalState.NORMAL -> ThrottleRecommendations(
                cpuThrottle = 0,
                gpuThrottle = 0,
                reduceVideoQuality = false,
                reduceFrameRate = false,
                disableUpscaling = false,
                limitNetworkRequests = false,
                reduceBufferSize = false,
                suspendBackgroundTasks = false,
            )
            ThermalState.ELEVATED -> ThrottleRecommendations(
                cpuThrottle = 1,
                gpuThrottle = 0,
                reduceVideoQuality = false,
                reduceFrameRate = false,
                disableUpscaling = false,
                limitNetworkRequests = false,
                reduceBufferSize = false,
                suspendBackgroundTasks = false,
            )
            ThermalState.WARM -> ThrottleRecommendations(
                cpuThrottle = 2,
                gpuThrottle = 1,
                reduceVideoQuality = true,
                reduceFrameRate = false,
                disableUpscaling = true,
                limitNetworkRequests = true,
                reduceBufferSize = true,
                suspendBackgroundTasks = true,
            )
            ThermalState.HOT -> ThrottleRecommendations(
                cpuThrottle = 3,
                gpuThrottle = 2,
                reduceVideoQuality = true,
                reduceFrameRate = true,
                disableUpscaling = true,
                limitNetworkRequests = true,
                reduceBufferSize = true,
                suspendBackgroundTasks = true,
            )
            ThermalState.CRITICAL -> ThrottleRecommendations(
                cpuThrottle = 4,
                gpuThrottle = 3,
                reduceVideoQuality = true,
                reduceFrameRate = true,
                disableUpscaling = true,
                limitNetworkRequests = true,
                reduceBufferSize = true,
                suspendBackgroundTasks = true,
            )
            ThermalState.EMERGENCY -> ThrottleRecommendations(
                cpuThrottle = 5,
                gpuThrottle = 4,
                reduceVideoQuality = true,
                reduceFrameRate = true,
                disableUpscaling = true,
                limitNetworkRequests = true,
                reduceBufferSize = true,
                suspendBackgroundTasks = true,
            )
            ThermalState.SHUTDOWN -> ThrottleRecommendations(
                cpuThrottle = 5,
                gpuThrottle = 5,
                reduceVideoQuality = true,
                reduceFrameRate = true,
                disableUpscaling = true,
                limitNetworkRequests = true,
                reduceBufferSize = true,
                suspendBackgroundTasks = true,
            )
        }
        
        _throttleRecommendations.value = recommendations
        _cpuThrottleLevel.value = recommendations.cpuThrottle
        _gpuThrottleLevel.value = recommendations.gpuThrottle
        _isThrottling.value = state != ThermalState.NORMAL && state != ThermalState.ELEVATED
        
        Timber.i("Applied thermal policy for ${state.name}: CPU=${recommendations.cpuThrottle}, GPU=${recommendations.gpuThrottle}")
    }
    
    fun getCurrentRecommendations(): ThrottleRecommendations = _throttleRecommendations.value
    
    fun getCpuThrottleLevel(): Int = _cpuThrottleLevel.value
    fun getGpuThrottleLevel(): Int = _gpuThrottleLevel.value
    
    fun applyCpuThrottle(throttleLevel: Int) {
        try {
            // Set CPU governor to powersave for high throttle levels
            if (throttleLevel >= 3) {
                setCpuGovernor("powersave")
            } else if (throttleLevel >= 1) {
                setCpuGovernor("ondemand")
            }
            
            // Adjust thread priorities
            adjustThreadPriorities(throttleLevel)
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply CPU throttle")
        }
    }
    
    fun applyGpuThrottle(throttleLevel: Int) {
        try {
            // Reduce GPU frequency through thermal zones if available
            // This is device-specific and may not work on all devices
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply GPU throttle")
        }
    }
    
    private fun setCpuGovernor(governor: String) {
        try {
            val cpuDir = java.io.File("/sys/devices/system/cpu")
            val cpuFiles = cpuDir.listFiles { _, name -> name.startsWith("cpu") && name[3].isDigit() }
            
            cpuFiles?.forEach { cpu ->
                val governorFile = java.io.File(cpu, "cpufreq/scaling_governor")
                if (governorFile.exists() && governorFile.canWrite()) {
                    governorFile.writeText(governor)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to set CPU governor")
        }
    }
    
    private fun adjustThreadPriorities(throttleLevel: Int) {
        val priority = when (throttleLevel) {
            0 -> Thread.NORM_PRIORITY
            1 -> Thread.NORM_PRIORITY - 1
            2 -> Thread.NORM_PRIORITY - 2
            3 -> Thread.MIN_PRIORITY + 2
            4 -> Thread.MIN_PRIORITY + 1
            else -> Thread.MIN_PRIORITY
        }
        
        Thread.currentThread().priority = priority
    }
    
    fun forceCooldown() {
        _thermalState.value = ThermalState.NORMAL
        _throttleRecommendations.value = ThrottleRecommendations()
        _cpuThrottleLevel.value = 0
        _gpuThrottleLevel.value = 0
        _isThrottling.value = false
        consecutiveHighTemps = 0
        
        try {
            setCpuGovernor("performance")
        } catch (e: Exception) {}
        
        Timber.i("Forced cooldown - thermal state reset")
    }
    
    fun shutdown() {
        stopMonitoring()
        scope.cancel()
        tempMonitorExecutor.shutdown()
    }
    
    data class ThrottleRecommendations(
        val cpuThrottle: Int = 0,
        val gpuThrottle: Int = 0,
        val reduceVideoQuality: Boolean = false,
        val reduceFrameRate: Boolean = false,
        val disableUpscaling: Boolean = false,
        val limitNetworkRequests: Boolean = false,
        val reduceBufferSize: Boolean = false,
        val suspendBackgroundTasks: Boolean = false,
    )
    
    enum class ThermalState {
        NORMAL, ELEVATED, WARM, HOT, CRITICAL, EMERGENCY, SHUTDOWN
    }
}

class ThermalLifecycleObserver(
    private val manager: ThermalThrottlingManager,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        manager.startMonitoring()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        manager.stopMonitoring()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        manager.shutdown()
    }
}

@Deprecated("Move to app module - Composable UI should not be in library", ReplaceWith("/* ThermalOverlay moved to app module */"))
fun ThermalOverlay(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    thermalManager: ThermalThrottlingManager,
    isVisible: Boolean = true,
) {
    // ThermalOverlay moved to app module to avoid Compose dependencies in library
}
            )
            
            androidx.compose.material3.Text(
                text = "CPU Throttle: $cpuThrottle/5",
                color = if (cpuThrottle > 0) androidx.compose.ui.graphics.Color.Yellow else androidx.compose.ui.graphics.Color.Green,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            
            androidx.compose.material3.Text(
                text = "GPU Throttle: $gpuThrottle/5",
                color = if (gpuThrottle > 0) androidx.compose.ui.graphics.Color.Yellow else androidx.compose.ui.graphics.Color.Green,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            
            if (recommendations.reduceVideoQuality) {
                androidx.compose.material3.Text(
                    text = "⚠ Reducing video quality",
                    color = androidx.compose.ui.graphics.Color.Yellow,
                    fontSize = 12.sp,
                )
            }
            
            if (recommendations.disableUpscaling) {
                androidx.compose.material3.Text(
                    text = "⚠ Upscaling disabled",
                    color = androidx.compose.ui.graphics.Color.Yellow,
                    fontSize = 12.sp,
                )
            }
            
            if (recommendations.reduceFrameRate) {
                androidx.compose.material3.Text(
                    text = "⚠ Frame rate reduced",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 12.sp,
                )
            }
        }
    }
}