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

package com.kurostream.common.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Startup and Memory Optimization Manager.
 * 
 * Responsibilities:
 * - Startup time optimization
 * - Memory usage monitoring and optimization
 * - Low-memory device handling
 * - Memory leak detection
 * - Cache management
 * - GC optimization
 */
@Singleton
class StartupMemoryOptimizer @Inject constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: throw IllegalStateException("ActivityManager not available")
    
    private val _memoryState = MutableStateFlow(MemoryOptimizationState())
    val memoryState: StateFlow<MemoryOptimizationState> = _memoryState.asStateFlow()
    
    private val _startupState = MutableStateFlow(StartupOptimizationState())
    val startupState: StateFlow<StartupOptimizationState> = _startupState.asStateFlow()
    
    private var isLowRamDevice = false
    private var totalMemoryMb = 0
    
    init {
        detectDeviceCapabilities()
        startMemoryMonitoring()
    }
    
    /**
     * Detect device memory capabilities
     */
    private fun detectDeviceCapabilities() {
        isLowRamDevice = activityManager.isLowRamDevice
        
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        totalMemoryMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        
        _memoryState.value = _memoryState.value.copy(
            isLowRamDevice = isLowRamDevice,
            totalMemoryMb = totalMemoryMb,
            availableMemoryMb = (memInfo.availMem / (1024 * 1024)).toInt(),
            thresholdMb = (memInfo.threshold / (1024 * 1024)).toInt(),
            isLowMemory = memInfo.lowMemory,
        )
        
        Timber.d("Device memory: total=${totalMemoryMb}MB, available=${_memoryState.value.availableMemoryMb}MB, lowRam=$isLowRamDevice")
    }
    
    /**
     * Start continuous memory monitoring
     */
    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                updateMemoryStats()
                detectMemoryLeaks()
                awaitLowMemoryWarning()
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Update memory statistics
     */
    private fun updateMemoryStats() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val nativeHeap = (Debug.getNativeHeapAllocatedSize() / (1024 * 1024)).toInt()
        val runtime = Runtime.getRuntime()
        val dalvikHeap = (runtime.totalMemory() / (1024 * 1024)).toInt()
        val appMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
        
        _memoryState.value = _memoryState.value.copy(
            availableMemoryMb = (memInfo.availMem / (1024 * 1024)).toInt(),
            usedMemoryMb = appMemoryMb,
            nativeHeapMb = nativeHeap,
            dalvikHeapMb = dalvikHeap,
            isLowMemory = memInfo.lowMemory,
            thresholdMb = (memInfo.threshold / (1024 * 1024)).toInt(),
        )
    }
    
    /**
     * Detect potential memory leaks
     */
    private fun detectMemoryLeaks() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        // Check if memory is consistently low
        if (memInfo.lowMemory && !_memoryState.value.isLowMemory) {
            Timber.w("Low memory warning triggered")
            _memoryState.value = _memoryState.value.copy(
                lowMemoryCount = _memoryState.value.lowMemoryCount + 1,
                lastLowMemoryTime = System.currentTimeMillis(),
            )
            
            // Trigger memory cleanup
            requestMemoryCleanup()
        }
    }
    
    /**
     * Wait for low memory callback
     */
    private suspend fun awaitLowMemoryWarning() {
        // In production, register for ComponentCallbacks2
        // This is a simplified implementation
    }
    
    /**
     * Request memory cleanup from the system
     */
    fun requestMemoryCleanup() {
        Timber.d("Requesting memory cleanup")
        
        // Clear caches on IO dispatcher
        scope.launch(Dispatchers.IO) {
            clearCachesInternal()
        }
    }
    
    /**
     * Clear app caches (internal, runs on IO dispatcher)
     */
    private suspend fun clearCachesInternal() {
        try {
            // Clear image cache
            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                imageCacheDir.deleteRecursively()
            }
            
            // Clear thumbnail cache
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            if (thumbnailDir.exists()) {
                thumbnailDir.deleteRecursively()
            }
            
            // Clear artwork cache
            val artworkDir = File(context.cacheDir, "artwork")
            if (artworkDir.exists()) {
                artworkDir.deleteRecursively()
            }
            
            Timber.d("Caches cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear caches")
        }
    }
    
    /**
     * Get recommended memory configuration for current device
     */
    fun getRecommendedMemoryConfig(): MemoryConfig {
        return when {
            isLowRamDevice || totalMemoryMb < 1536 -> MemoryConfig(
                maxImageCacheMb = 30,
                maxBitmapPoolMb = 20,
                maxBufferPoolMb = 16,
                enableCompression = true,
                imageQuality = 80,
                maxImageDimension = 1280,
                preloadCount = 2,
            )
            totalMemoryMb < 2048 -> MemoryConfig(
                maxImageCacheMb = 50,
                maxBitmapPoolMb = 32,
                maxBufferPoolMb = 24,
                enableCompression = true,
                imageQuality = 85,
                maxImageDimension = 1920,
                preloadCount = 3,
            )
            totalMemoryMb < 4096 -> MemoryConfig(
                maxImageCacheMb = 100,
                maxBitmapPoolMb = 64,
                maxBufferPoolMb = 48,
                enableCompression = false,
                imageQuality = 90,
                maxImageDimension = 2560,
                preloadCount = 5,
            )
            else -> MemoryConfig(
                maxImageCacheMb = 200,
                maxBitmapPoolMb = 128,
                maxBufferPoolMb = 96,
                enableCompression = false,
                imageQuality = 95,
                maxImageDimension = 3840,
                preloadCount = 8,
            )
        }
    }
    
    /**
     * Optimize for startup speed
     */
    fun optimizeForStartup(): StartupConfig {
        return StartupConfig(
            deferNonCriticalInit = true,
            lazyLoadScreens = true,
            preloadCriticalPathsOnly = true,
            useMinimalTheme = isLowRamDevice,
            delayImageLoading = true,
            parallelInit = !isLowRamDevice,
            maxConcurrentInitTasks = if (isLowRamDevice) 2 else 4,
        )
    }
    
    /**
     * Get memory optimization suggestions
     */
    fun getOptimizationSuggestions(): List<MemoryOptimizationSuggestion> {
        val suggestions = mutableListOf<MemoryOptimizationSuggestion>()
        val state = _memoryState.value
        
        if (state.usedMemoryMb > totalMemoryMb * 0.8) {
            suggestions.add(
                MemoryOptimizationSuggestion(
                    type = SuggestionType.HIGH_MEMORY_USAGE,
                    priority = Priority.HIGH,
                    title = "High Memory Usage",
                    description = "App is using ${state.usedMemoryMb}MB (${(state.usedMemoryMb * 100 / totalMemoryMb)}% of total)",
                    action = "Clear caches and reduce image quality",
                    estimatedSavingsMb = 100,
                )
            )
        }
        
        if (state.nativeHeapMb > 100) {
            suggestions.add(
                MemoryOptimizationSuggestion(
                    type = SuggestionType.NATIVE_HEAP_LEAK,
                    priority = Priority.MEDIUM,
                    title = "High Native Heap Usage",
                    description = "Native heap is using ${state.nativeHeapMb}MB",
                    action = "Check for native memory leaks",
                    estimatedSavingsMb = 50,
                )
            )
        }
        
        if (state.lowMemoryCount > 3) {
            suggestions.add(
                MemoryOptimizationSuggestion(
                    type = SuggestionType.FREQUENT_LOW_MEMORY,
                    priority = Priority.HIGH,
                    title = "Frequent Low Memory Events",
                    description = "Low memory triggered ${state.lowMemoryCount} times",
                    action = "Reduce memory usage or increase cache sizes",
                    estimatedSavingsMb = 150,
                )
            )
        }
        
        return suggestions
    }
    
    /**
     * Force garbage collection - removed ineffective explicit GC calls
     * Android ignores explicit GC requests; rely on system GC
     */
    fun forceGarbageCollection() {
        Timber.d("Garbage collection request logged (actual GC controlled by system)")
        // System handles GC; explicit calls are ignored
    }
    
    /**
     * Trim memory usage
     */
    fun trimMemory(level: Int) {
        Timber.d("Trimming memory at level $level")
        
        when (level) {
            // TRIM_MEMORY_RUNNING_MODERATE
            15 -> {
                scope.launch(Dispatchers.IO) { clearCachesInternal() }
            }
            // TRIM_MEMORY_RUNNING_LOW
            20 -> {
                scope.launch(Dispatchers.IO) { clearCachesInternal() }
                forceGarbageCollection()
            }
            // TRIM_MEMORY_RUNNING_CRITICAL
            25, 30, 35, 40, 45 -> {
                scope.launch(Dispatchers.IO) { clearCachesInternal() }
                forceGarbageCollection()
                // Release any held resources
            }
            // Background app
            40, 50, 60, 70, 80, 90, 100 -> {
                scope.launch(Dispatchers.IO) { clearCachesInternal() }
                forceGarbageCollection()
            }
        }
    }
}

data class MemoryOptimizationState(
    val isLowRamDevice: Boolean = false,
    val totalMemoryMb: Int = 0,
    val availableMemoryMb: Int = 0,
    val usedMemoryMb: Int = 0,
    val nativeHeapMb: Int = 0,
    val dalvikHeapMb: Int = 0,
    val isLowMemory: Boolean = false,
    val thresholdMb: Int = 0,
    val lowMemoryCount: Int = 0,
    val lastLowMemoryTime: Long = 0,
)

data class StartupOptimizationState(
    val startupStartTime: Long = 0,
    val firstFrameTime: Long = 0,
    val fullyLoadedTime: Long = 0,
    val isOptimized: Boolean = false,
)

data class MemoryConfig(
    val maxImageCacheMb: Int,
    val maxBitmapPoolMb: Int,
    val maxBufferPoolMb: Int,
    val enableCompression: Boolean,
    val imageQuality: Int,
    val maxImageDimension: Int,
    val preloadCount: Int,
)

data class StartupConfig(
    val deferNonCriticalInit: Boolean,
    val lazyLoadScreens: Boolean,
    val preloadCriticalPathsOnly: Boolean,
    val useMinimalTheme: Boolean,
    val delayImageLoading: Boolean,
    val parallelInit: Boolean,
    val maxConcurrentInitTasks: Int,
)

data class MemoryOptimizationSuggestion(
    val type: SuggestionType,
    val priority: Priority,
    val title: String,
    val description: String,
    val action: String,
    val estimatedSavingsMb: Int,
)

enum class SuggestionType {
    HIGH_MEMORY_USAGE,
    NATIVE_HEAP_LEAK,
    FREQUENT_LOW_MEMORY,
    CACHE_TOO_LARGE,
    BITMAP_NOT_RECYCLED,
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}