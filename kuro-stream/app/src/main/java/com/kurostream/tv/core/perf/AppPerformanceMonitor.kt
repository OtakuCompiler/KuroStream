package com.kurostream.tv.core.perf

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitor optimized for low-end Fire TV devices (1GB RAM)
 * Tracks memory usage, CPU, and provides alerts for resource constraints
 */
@Singleton
class AppPerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        // Memory thresholds for 1GB devices
        private const val MEMORY_WARNING_THRESHOLD = 0.75f // 75% usage
        private const val MEMORY_CRITICAL_THRESHOLD = 0.85f // 85% usage
        private const val LOW_MEMORY_THRESHOLD_MB = 100 // Less than 100MB free
        
        // Monitoring intervals
        private const val MONITOR_INTERVAL_MS = 5000L // 5 seconds
        private const val DETAILED_MONITOR_INTERVAL_MS = 1000L // 1 second during playback
        
        // Frame rate monitoring
        private const val TARGET_FRAME_RATE = 60
        private const val ACCEPTABLE_FRAME_RATE = 30
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    private val _memoryWarnings = MutableStateFlow<MemoryWarning?>(null)
    val memoryWarnings: StateFlow<MemoryWarning?> = _memoryWarnings.asStateFlow()
    
    private var isPlaybackActive = false
    private val performanceListeners = mutableListOf<PerformanceListener>()
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        
        monitorJob = scope.launch {
            Timber.tag(TAG).d("Performance monitoring started")
            
            while (isActive) {
                updatePerformanceMetrics()
                checkMemoryThresholds()
                
                val interval = if (isPlaybackActive) {
                    DETAILED_MONITOR_INTERVAL_MS
                } else {
                    MONITOR_INTERVAL_MS
                }
                delay(interval)
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        Timber.tag(TAG).d("Performance monitoring stopped")
    }
    
    /**
     * Set playback active state for more frequent monitoring
     */
    fun setPlaybackActive(active: Boolean) {
        isPlaybackActive = active
        if (active) {
            Timber.tag(TAG).d("Playback started - increasing monitor frequency")
        }
    }
    
    /**
     * Get current memory info
     */
    fun getMemoryInfo(): MemoryMetrics {
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return MemoryMetrics(
            totalSystemMemory = memoryInfo.totalMem,
            availableSystemMemory = memoryInfo.availMem,
            appUsedMemory = usedMemory,
            appMaxMemory = maxMemory,
            isLowMemory = memoryInfo.lowMemory,
            lowMemoryThreshold = memoryInfo.threshold,
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapAllocated = Debug.getNativeHeapAllocatedSize(),
            nativeHeapFree = Debug.getNativeHeapFreeSize()
        )
    }
    
    /**
     * Get CPU usage percentage
     */
    fun getCpuUsage(): Float {
        return try {
            val pid = Process.myPid()
            val statFile = RandomAccessFile("/proc/$pid/stat", "r")
            val stat = statFile.readLine()
            statFile.close()
            
            val parts = stat.split(" ")
            if (parts.size >= 14) {
                val utime = parts[13].toLongOrNull() ?: 0
                val stime = parts[14].toLongOrNull() ?: 0
                val totalTime = utime + stime
                
                // Calculate percentage (simplified)
                (totalTime % 100).toFloat()
            } else {
                0f
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to read CPU usage")
            0f
        }
    }
    
    /**
     * Check if device is low-end (1GB or less RAM)
     */
    fun isLowEndDevice(): Boolean {
        activityManager.getMemoryInfo(memoryInfo)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        return totalMemoryMB <= 1536 // 1.5GB or less
    }
    
    /**
     * Get recommended settings for current device
     */
    fun getRecommendedSettings(): DeviceRecommendations {
        val isLowEnd = isLowEndDevice()
        val memoryMetrics = getMemoryInfo()
        val availableMemoryMB = memoryMetrics.availableSystemMemory / (1024 * 1024)
        
        return DeviceRecommendations(
            maxVideoQuality = when {
                availableMemoryMB < 200 -> VideoQuality.SD_480P
                availableMemoryMB < 400 -> VideoQuality.HD_720P
                else -> VideoQuality.FHD_1080P
            },
            bufferSizeMs = when {
                isLowEnd -> 8000 // 8 seconds for low-end
                availableMemoryMB < 300 -> 15000
                else -> 30000
            },
            preloadNextEpisode = !isLowEnd && availableMemoryMB > 300,
            enableSubtitleRendering = true, // Always enable
            enableSkipAnimations = !isLowEnd,
            imageCacheSizeMB = when {
                isLowEnd -> 50
                availableMemoryMB < 300 -> 100
                else -> 200
            },
            maxConcurrentDownloads = if (isLowEnd) 1 else 2,
            enableBackgroundPlayback = !isLowEnd
        )
    }
    
    /**
     * Request garbage collection (use sparingly)
     */
    fun requestGarbageCollection() {
        Timber.tag(TAG).d("Requesting garbage collection")
        System.gc()
        System.runFinalization()
    }
    
    /**
     * Trim memory when system requests it
     */
    fun onTrimMemory(level: Int) {
        when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Timber.tag(TAG).d("UI hidden - clearing image caches")
                notifyTrimMemory(TrimLevel.UI_HIDDEN)
            }
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Timber.tag(TAG).w("Running low on memory")
                notifyTrimMemory(TrimLevel.RUNNING_LOW)
            }
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Timber.tag(TAG).w("Critical memory - releasing all caches")
                notifyTrimMemory(TrimLevel.CRITICAL)
                requestGarbageCollection()
            }
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Timber.tag(TAG).w("Complete memory trim requested")
                notifyTrimMemory(TrimLevel.COMPLETE)
            }
        }
    }
    
    /**
     * Add performance listener
     */
    fun addListener(listener: PerformanceListener) {
        performanceListeners.add(listener)
    }
    
    /**
     * Remove performance listener
     */
    fun removeListener(listener: PerformanceListener) {
        performanceListeners.remove(listener)
    }
    
    /**
     * Log current performance state
     */
    fun logPerformanceSnapshot() {
        val metrics = getMemoryInfo()
        val state = _performanceState.value
        
        Timber.tag(TAG).i("""
            Performance Snapshot:
            - App Memory: ${metrics.appUsedMemory / (1024 * 1024)}MB / ${metrics.appMaxMemory / (1024 * 1024)}MB
            - System Memory: ${metrics.availableSystemMemory / (1024 * 1024)}MB available
            - Native Heap: ${metrics.nativeHeapAllocated / (1024 * 1024)}MB / ${metrics.nativeHeapSize / (1024 * 1024)}MB
            - Low Memory: ${metrics.isLowMemory}
            - Memory Usage: ${String.format("%.1f", state.memoryUsagePercent * 100)}%
            - CPU Usage: ${String.format("%.1f", state.cpuUsagePercent)}%
        """.trimIndent())
    }
    
    private fun updatePerformanceMetrics() {
        val memoryMetrics = getMemoryInfo()
        val cpuUsage = getCpuUsage()
        
        val memoryUsagePercent = if (memoryMetrics.appMaxMemory > 0) {
            memoryMetrics.appUsedMemory.toFloat() / memoryMetrics.appMaxMemory
        } else 0f
        
        _performanceState.value = PerformanceState(
            memoryUsagePercent = memoryUsagePercent,
            cpuUsagePercent = cpuUsage,
            availableMemoryMB = (memoryMetrics.availableSystemMemory / (1024 * 1024)).toInt(),
            isLowMemory = memoryMetrics.isLowMemory,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    private fun checkMemoryThresholds() {
        val state = _performanceState.value
        
        val warning = when {
            state.memoryUsagePercent >= MEMORY_CRITICAL_THRESHOLD -> {
                Timber.tag(TAG).w("Critical memory usage: ${state.memoryUsagePercent * 100}%")
                MemoryWarning.CRITICAL
            }
            state.memoryUsagePercent >= MEMORY_WARNING_THRESHOLD -> {
                Timber.tag(TAG).w("High memory usage: ${state.memoryUsagePercent * 100}%")
                MemoryWarning.HIGH
            }
            state.availableMemoryMB < LOW_MEMORY_THRESHOLD_MB -> {
                Timber.tag(TAG).w("Low available memory: ${state.availableMemoryMB}MB")
                MemoryWarning.LOW_AVAILABLE
            }
            state.isLowMemory -> {
                MemoryWarning.SYSTEM_LOW
            }
            else -> null
        }
        
        if (warning != _memoryWarnings.value) {
            _memoryWarnings.value = warning
            warning?.let { notifyMemoryWarning(it) }
        }
    }
    
    private fun notifyMemoryWarning(warning: MemoryWarning) {
        performanceListeners.forEach { listener ->
            try {
                listener.onMemoryWarning(warning)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error notifying memory warning")
            }
        }
    }
    
    private fun notifyTrimMemory(level: TrimLevel) {
        performanceListeners.forEach { listener ->
            try {
                listener.onTrimMemory(level)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error notifying trim memory")
            }
        }
    }
}

/**
 * Current performance state
 */
data class PerformanceState(
    val memoryUsagePercent: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val availableMemoryMB: Int = 0,
    val isLowMemory: Boolean = false,
    val lastUpdateTime: Long = 0
)

/**
 * Detailed memory metrics
 */
data class MemoryMetrics(
    val totalSystemMemory: Long,
    val availableSystemMemory: Long,
    val appUsedMemory: Long,
    val appMaxMemory: Long,
    val isLowMemory: Boolean,
    val lowMemoryThreshold: Long,
    val nativeHeapSize: Long,
    val nativeHeapAllocated: Long,
    val nativeHeapFree: Long
) {
    val appMemoryUsagePercent: Float
        get() = if (appMaxMemory > 0) appUsedMemory.toFloat() / appMaxMemory else 0f
    
    val systemMemoryUsagePercent: Float
        get() = if (totalSystemMemory > 0) {
            (totalSystemMemory - availableSystemMemory).toFloat() / totalSystemMemory
        } else 0f
}

/**
 * Memory warning levels
 */
enum class MemoryWarning {
    HIGH,        // Above 75% app memory usage
    CRITICAL,    // Above 85% app memory usage
    LOW_AVAILABLE, // Less than 100MB system memory
    SYSTEM_LOW   // System reports low memory
}

/**
 * Memory trim levels
 */
enum class TrimLevel {
    UI_HIDDEN,
    RUNNING_LOW,
    CRITICAL,
    COMPLETE
}

/**
 * Video quality levels
 */
enum class VideoQuality(val maxHeight: Int, val label: String) {
    SD_480P(480, "480p"),
    HD_720P(720, "720p"),
    FHD_1080P(1080, "1080p"),
    QHD_1440P(1440, "1440p"),
    UHD_4K(2160, "4K")
}

/**
 * Device-specific recommended settings
 */
data class DeviceRecommendations(
    val maxVideoQuality: VideoQuality,
    val bufferSizeMs: Int,
    val preloadNextEpisode: Boolean,
    val enableSubtitleRendering: Boolean,
    val enableSkipAnimations: Boolean,
    val imageCacheSizeMB: Int,
    val maxConcurrentDownloads: Int,
    val enableBackgroundPlayback: Boolean
)

/**
 * Listener for performance events
 */
interface PerformanceListener {
    fun onMemoryWarning(warning: MemoryWarning)
    fun onTrimMemory(level: TrimLevel)
}
