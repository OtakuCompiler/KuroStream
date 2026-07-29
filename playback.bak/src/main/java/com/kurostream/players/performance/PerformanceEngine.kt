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

package com.kurostream.players.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance Engine that coordinates all performance optimizations.
 * 
 * Responsibilities:
 * - Real-time performance monitoring
 * - Adaptive quality adjustment
 * - Thermal throttling management
 * - Memory pressure response
 * - Frame rate stabilization
 * - Buffer health management
 */
@Singleton
class PerformanceEngine @Inject constructor(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()
    
    private val _performanceLevel = MutableStateFlow(PerformanceLevel.BALANCED)
    val performanceLevel: StateFlow<PerformanceLevel> = _performanceLevel.asStateFlow()
    
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fpsSum = 0f
    private var fpsSamples = 0
    
    private var monitoringJob: kotlinx.coroutines.Job? = null
    
    init {
        startMonitoring()
    }
    
    /**
     * Start performance monitoring
     */
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (true) {
                updatePerformanceMetrics()
                evaluatePerformanceLevel()
                applyOptimizations()
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        // Update memory stats
        performanceMonitor.updateMemoryStats()
        
        val memStats = performanceMonitor.memoryStats.value
        
        // Calculate CPU usage
        val cpuUsage = calculateCpuUsage()
        
        // Calculate FPS
        val fps = calculateFps()
        
        // Update metrics
        performanceMonitor.updatePlaybackMetrics(
            resolution = _engineState.value.currentResolution,
            isUpscaling = _engineState.value.isUpscaling,
            isAudioTranscoding = _engineState.value.isAudioTranscoding,
            frameRate = fps,
            droppedFrames = _engineState.value.droppedFrames,
            bufferHealth = _engineState.value.bufferHealth,
            downloadSpeedBps = _engineState.value.downloadSpeedBps,
            cpuUsage = cpuUsage,
        )
        
        // Update engine state
        _engineState.value = _engineState.value.copy(
            memoryUsageMb = memStats.totalPrivateMemoryKb / 1024,
            cpuUsage = cpuUsage,
            currentFps = fps,
            timestamp = System.currentTimeMillis(),
        )
    }
    
    /**
     * Evaluate and adjust performance level
     */
    private fun evaluatePerformanceLevel() {
        val state = _engineState.value
        val memStats = performanceMonitor.memoryStats.value
        
        // Calculate pressure score
        val memoryPressure = memStats.totalPrivateMemoryKb / 1024f / getTotalMemoryMb()
        val cpuPressure = state.cpuUsage / 100f
        val framePressure = if (state.currentFps < 30) 0.3f else 0f
        val bufferPressure = if (state.bufferHealth < 0.3f) 0.2f else 0f
        
        val totalPressure = (memoryPressure * 0.4f + cpuPressure * 0.3f + framePressure * 0.2f + bufferPressure * 0.1f)
            .coerceIn(0f, 1f)
        
        // Determine performance level
        val newLevel = when {
            totalPressure > 0.85f -> PerformanceLevel.POWER_SAVER
            totalPressure > 0.7f -> PerformanceLevel.BATTERY_SAVER
            totalPressure > 0.5f -> PerformanceLevel.BALANCED
            totalPressure > 0.3f -> PerformanceLevel.HIGH_PERFORMANCE
            else -> PerformanceLevel.MAX_PERFORMANCE
        }
        
        if (newLevel != _performanceLevel.value) {
            _performanceLevel.value = newLevel
            Timber.d("Performance level changed to: $newLevel (pressure=$totalPressure)")
        }
    }
    
    /**
     * Apply optimizations based on current state
     */
    private fun applyOptimizations() {
        val level = _performanceLevel.value
        val state = _engineState.value
        
        // Adjust resolution based on performance level
        val targetResolution = when (level) {
            PerformanceLevel.MAX_PERFORMANCE -> 3840 to 2160
            PerformanceLevel.HIGH_PERFORMANCE -> 2560 to 1440
            PerformanceLevel.BALANCED -> 1920 to 1080
            PerformanceLevel.BATTERY_SAVER -> 1280 to 720
            PerformanceLevel.POWER_SAVER -> 854 to 480
        }
        
        // Adjust upscaling based on performance level
        val shouldUpscale = when (level) {
            PerformanceLevel.MAX_PERFORMANCE, PerformanceLevel.HIGH_PERFORMANCE -> state.isUpscaling
            else -> false
        }
        
        // Adjust buffer size based on performance level
        val bufferSize = when (level) {
            PerformanceLevel.MAX_PERFORMANCE -> 32 * 1024 * 1024
            PerformanceLevel.HIGH_PERFORMANCE -> 24 * 1024 * 1024
            PerformanceLevel.BALANCED -> 16 * 1024 * 1024
            PerformanceLevel.BATTERY_SAVER -> 8 * 1024 * 1024
            PerformanceLevel.POWER_SAVER -> 4 * 1024 * 1024
        }
        
        // Apply changes
        _engineState.value = state.copy(
            targetResolution = targetResolution,
            shouldDisableUpscaling = !shouldUpscale,
            targetBufferSize = bufferSize,
        )
    }
    
    /**
     * Set the current playback configuration
     */
    fun setPlaybackConfig(config: PlaybackConfiguration) {
        _engineState.value = _engineState.value.copy(
            currentResolution = config.resolution,
            isUpscaling = config.isUpscaling,
            isAudioTranscoding = config.isAudioTranscoding,
            downloadSpeedBps = config.downloadSpeedBps,
        )
    }
    
    /**
     * Report dropped frames
     */
    fun reportDroppedFrames(count: Int) {
        _engineState.value = _engineState.value.copy(
            droppedFrames = _engineState.value.droppedFrames + count
        )
    }
    
    /**
     * Update buffer health
     */
    fun updateBufferHealth(health: Float) {
        _engineState.value = _engineState.value.copy(bufferHealth = health)
    }
    
    /**
     * Get current optimization recommendations
     */
    fun getOptimizations(): List<PerformanceOptimization> {
        val optimizations = mutableListOf<PerformanceOptimization>()
        val state = _engineState.value
        val level = _performanceLevel.value
        
        // Resolution optimization
        if (state.currentResolution.first > state.targetResolution.first) {
            optimizations.add(
                PerformanceOptimization(
                    type = OptimizationType.RESOLUTION,
                    action = "Reduce resolution to ${state.targetResolution.first}x${state.targetResolution.second}",
                    reason = "Performance level: $level",
                    estimatedSavingsMw = 500,
                )
            )
        }
        
        // Upscaling optimization
        if (state.isUpscaling && state.shouldDisableUpscaling) {
            optimizations.add(
                PerformanceOptimization(
                    type = OptimizationType.UPSCALE,
                    action = "Disable video upscaling",
                    reason = "Low performance detected",
                    estimatedSavingsMw = 800,
                )
            )
        }
        
        // Buffer optimization
        if (state.targetBufferSize < 16 * 1024 * 1024) {
            optimizations.add(
                PerformanceOptimization(
                    type = OptimizationType.BUFFER,
                    action = "Reduce buffer size to ${state.targetBufferSize / 1024 / 1024}MB",
                    reason = "Memory optimization",
                    estimatedSavingsMw = 200,
                )
            )
        }
        
        // Frame rate optimization
        if (state.currentFps > 60) {
            optimizations.add(
                PerformanceOptimization(
                    type = OptimizationType.FRAME_RATE,
                    action = "Cap frame rate at 60fps",
                    reason = "Reduce GPU load",
                    estimatedSavingsMw = 300,
                )
            )
        }
        
        return optimizations
    }
    
    /**
     * Get thermal throttling recommendations
     */
    fun getThermalRecommendations(): List<ThermalRecommendation> {
        val recommendations = mutableListOf<ThermalRecommendation>()
        
        // Check thermal state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val thermalStatus = powerManager.currentThermalStatus
            
            when (thermalStatus) {
                android.os.PowerManager.THERMAL_STATUS_NONE,
                android.os.PowerManager.THERMAL_STATUS_LIGHT -> {
                    // No action needed
                }
                android.os.PowerManager.THERMAL_STATUS_MODERATE -> {
                    recommendations.add(
                        ThermalRecommendation(
                            level = ThermalLevel.MODERATE,
                            action = "Reduce video quality to 720p",
                            reason = "Device is warming up",
                        )
                    )
                }
                android.os.PowerManager.THERMAL_STATUS_SEVERE -> {
                    recommendations.add(
                        ThermalRecommendation(
                            level = ThermalLevel.SEVERE,
                            action = "Reduce video quality to 480p and disable upscaling",
                            reason = "Device is hot",
                        )
                    )
                }
                android.os.PowerManager.THERMAL_STATUS_CRITICAL,
                android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> {
                    recommendations.add(
                        ThermalRecommendation(
                            level = ThermalLevel.CRITICAL,
                            action = "Pause playback and let device cool down",
                            reason = "Device is overheating",
                        )
                    )
                }
                android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                    recommendations.add(
                        ThermalRecommendation(
                            level = ThermalLevel.SHUTDOWN,
                            action = "Stop playback immediately",
                            reason = "Device will shut down",
                        )
                    )
                }
            }
        }
        
        return recommendations
    }
    
    private fun calculateCpuUsage(): Float {
        // Simple CPU usage estimation
        val startTime = SystemClock.currentThreadTimeMillis()
        delay(10L)
        val endTime = SystemClock.currentThreadTimeMillis()
        return ((endTime - startTime) * 100f / 10f).coerceIn(0f, 100f)
    }
    
    private fun calculateFps(): Float {
        val currentTime = System.nanoTime()
        if (lastFrameTime > 0) {
            val delta = (currentTime - lastFrameTime) / 1_000_000_000f
            if (delta > 0) {
                val fps = 1f / delta
                fpsSum += fps
                fpsSamples++
            }
        }
        lastFrameTime = currentTime
        
        return if (fpsSamples > 0) fpsSum / fpsSamples else 60f
    }
    
    private fun getTotalMemoryMb(): Float {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024f * 1024f)
    }
    
    /**
     * Release resources
     */
    fun release() {
        monitoringJob?.cancel()
        Timber.d("PerformanceEngine released")
    }
}

data class EngineState(
    val currentResolution: Pair<Int, Int> = 1920 to 1080,
    val targetResolution: Pair<Int, Int> = 1920 to 1080,
    val isUpscaling: Boolean = false,
    val shouldDisableUpscaling: Boolean = false,
    val isAudioTranscoding: Boolean = false,
    val currentFps: Float = 60f,
    val droppedFrames: Long = 0,
    val bufferHealth: Float = 1f,
    val downloadSpeedBps: Long = 0,
    val memoryUsageMb: Int = 0,
    val cpuUsage: Float = 0f,
    val targetBufferSize: Int = 16 * 1024 * 1024,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PlaybackConfiguration(
    val resolution: Pair<Int, Int> = 1920 to 1080,
    val isUpscaling: Boolean = false,
    val isAudioTranscoding: Boolean = false,
    val downloadSpeedBps: Long = 0,
)

enum class PerformanceLevel {
    MAX_PERFORMANCE,
    HIGH_PERFORMANCE,
    BALANCED,
    BATTERY_SAVER,
    POWER_SAVER,
}

data class PerformanceOptimization(
    val type: OptimizationType,
    val action: String,
    val reason: String,
    val estimatedSavingsMw: Int,
)

enum class OptimizationType {
    RESOLUTION,
    UPSCALE,
    BUFFER,
    FRAME_RATE,
    AUDIO,
}

data class ThermalRecommendation(
    val level: ThermalLevel,
    val action: String,
    val reason: String,
)

enum class ThermalLevel {
    NOMINAL,
    MODERATE,
    SEVERE,
    CRITICAL,
    SHUTDOWN,
}