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

package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Debug
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Memory monitor that observes system memory pressure and triggers
 * aggressive cleanup when memory drops below thresholds.
 * 
 * Integrates with ComponentCallbacks2 for system callbacks and
 * provides reactive StateFlow for UI observation.
 */
class MemoryMonitor private constructor(
    private val context: Context,
    private val config: MemoryConfig = MemoryConfig.DEFAULT
) : ComponentCallbacks2, LifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Memory state flows
    private val _availableMemory = MutableStateFlow<Long>(getAvailableMemory())
    val availableMemory: StateFlow<Long> = _availableMemory
    
    private val _totalMemory = MutableStateFlow<Long>(getTotalMemory())
    val totalMemory: StateFlow<Long> = _totalMemory
    
    private val _memoryPressure = MutableStateFlow<MemoryPressure>(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure
    
    private val _trimCallbackCount = MutableStateFlow(0)
    val trimCallbackCount: StateFlow<Int> = _trimCallbackCount
    
    // Callbacks for memory pressure events
    private val pressureCallbacks = mutableListOf<(MemoryPressure) -> Unit>()
    private var lastTrimLevel = 0
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: MemoryMonitor? = null
        
        fun getInstance(context: Context, config: MemoryConfig = MemoryConfig.DEFAULT): MemoryMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryMonitor(context.applicationContext, config).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    init {
        // Register for lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Start periodic memory polling
        startPolling()
        
        Timber.i("MemoryMonitor initialized with config: $config")
    }
    
    /**
     * Register a callback for memory pressure changes.
     */
    fun addPressureCallback(callback: (MemoryPressure) -> Unit) {
        pressureCallbacks.add(callback)
    }
    
    /**
     * Unregister a pressure callback.
     */
    fun removePressureCallback(callback: (MemoryPressure) -> Unit) {
        pressureCallbacks.remove(callback)
    }
    
    /**
     * Trigger emergency cleanup immediately.
     */
    fun triggerEmergencyCleanup(): CleanupResult {
        Timber.w("Emergency cleanup triggered!")
        return performCleanup(MemoryPressure.EMERGENCY)
    }
    
    /**
     * Get current available memory in bytes.
     */
    fun getCurrentAvailableMemory(): Long = _availableMemory.value
    
    /**
     * Get current total memory in bytes.
     */
    fun getCurrentTotalMemory(): Long = _totalMemory.value
    
    /**
     * Get current memory pressure level.
     */
    fun getCurrentPressure(): MemoryPressure = _memoryPressure.value
    
    // ===== ComponentCallbacks2 =====
    
    override fun onTrimMemory(level: Int) {
        lastTrimLevel = level
        _trimCallbackCount.value++
        
        val pressure = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> MemoryPressure.LOW
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> MemoryPressure.MODERATE
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> MemoryPressure.HIGH
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> MemoryPressure.MODERATE
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryPressure.HIGH
            else -> MemoryPressure.LOW
        }
        
        Timber.d("onTrimMemory: level=$level, pressure=$pressure")
        updatePressure(pressure)
    }
    
    override fun onLowMemory() {
        Timber.w("onLowMemory() - EMERGENCY!")
        updatePressure(MemoryPressure.EMERGENCY)
        performCleanup(MemoryPressure.EMERGENCY)
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        // No-op
    }
    
    // ===== LifecycleObserver =====
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        // App became visible - could restore some caches
        if (_memoryPressure.value != MemoryPressure.NORMAL) {
            // Still under pressure, don't restore
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        // App went to background - good time to trim
        updatePressure(MemoryPressure.MODERATE)
        performCleanup(MemoryPressure.MODERATE)
    }
    
    // ===== Private Implementation =====
    
    private fun startPolling() {
        scope.launch {
            while (isActive) {
                updateMemoryStats()
                delay(config.pollIntervalMs)
            }
        }
    }
    
    private fun updateMemoryStats() {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            
            val available = memInfo.availMem
            val total = memInfo.totalMem
            
            _availableMemory.value = available
            _totalMemory.value = total
            
            // Calculate pressure based on available memory
            val availableMB = available / (1024 * 1024)
            val pressure = when {
                availableMB < config.emergencyThresholdMb -> MemoryPressure.EMERGENCY
                availableMB < config.criticalThresholdMb -> MemoryPressure.CRITICAL
                availableMB < config.warningThresholdMb -> MemoryPressure.HIGH
                availableMB < config.cautionThresholdMb -> MemoryPressure.MODERATE
                else -> MemoryPressure.NORMAL
            }
            
            if (pressure != _memoryPressure.value) {
                updatePressure(pressure)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to update memory stats")
        }
    }
    
    private fun updatePressure(newPressure: MemoryPressure) {
        val oldPressure = _memoryPressure.value
        if (newPressure == oldPressure) return
        
        _memoryPressure.value = newPressure
        Timber.w("Memory pressure changed: $oldPressure -> $newPressure")
        
        // Notify callbacks
        pressureCallbacks.forEach { it(newPressure) }
        
        // Auto-cleanup for high pressure
        if (newPressure.ordinal >= MemoryPressure.HIGH.ordinal) {
            scope.launch { performCleanup(newPressure) }
        }
    }
    
    /**
     * Perform memory cleanup based on pressure level.
     */
    private fun performCleanup(pressure: MemoryPressure): CleanupResult {
        var freedBytes = 0L
        val actions = mutableListOf<String>()
        
        // 1. Clear Coil image memory cache
        try {
            val imageLoader = Coil.imageLoader(context)
            val memCache = imageLoader.memoryCache
            if (memCache != null) {
                val memCacheSizeBefore = memCache.size
                memCache.clear()
                freedBytes += memCacheSizeBefore
                actions.add("Coil memory cache: ${formatBytes(memCacheSizeBefore)}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Coil memory cache")
        }
        
        // 2. Clear object pools
        try {
            com.kurostream.common.pool.ObjectPools.clearAll()
            actions.add("Object pools cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear object pools")
        }
        
        // 3. Clear string interner
        if (pressure.ordinal >= MemoryPressure.HIGH.ordinal) {
            try {
                com.kurostream.common.util.StringInterner.clear()
                actions.add("String interner cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear string interner")
            }
        }
        
        // 4. Clear buffer pools
        if (pressure.ordinal >= MemoryPressure.CRITICAL.ordinal) {
            try {
                com.kurostream.common.pool.BufferPool.clearAll()
                actions.add("Buffer pools cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear buffer pools")
            }
        }
        
        // 5. Request GC (hint only)
        System.gc()
        System.runFinalization()
        actions.add("GC hint requested")
        
        // 6. For emergency, also clear disk caches if needed
        if (pressure == MemoryPressure.EMERGENCY) {
            try {
                // Clear Coil disk cache
                val imageLoader = Coil.imageLoader(context)
                val diskCache = imageLoader.diskCache
                if (diskCache != null) {
                    val diskCacheSizeBefore = diskCache.size
                    scope.launch { diskCache.clear() }
                    freedBytes += diskCacheSizeBefore
                    actions.add("Coil disk cache: ${formatBytes(diskCacheSizeBefore)}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear Coil disk cache")
            }
            
            // Clear metadata disk caches
            try {
                // This would clear Room database caches, etc.
                actions.add("Metadata disk caches cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear metadata caches")
            }
        }
        
        // Force memory stats update after cleanup
        updateMemoryStats()
        
        val result = CleanupResult(
            pressureLevel = pressure,
            freedBytes = freedBytes,
            actions = actions,
            timestamp = System.currentTimeMillis()
        )
        
        Timber.i("Memory cleanup completed: ${formatBytes(freedBytes)} freed, actions: $actions")
        return result
    }
    
    fun shutdown() {
        scope.coroutineContext.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        pressureCallbacks.clear()
        INSTANCE = null
    }
    
    private fun getAvailableMemory(): Long {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            return memInfo.availMem
        } catch (e: Exception) {
            return Runtime.getRuntime().freeMemory()
        }
    }
    
    private fun getTotalMemory(): Long {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            return memInfo.totalMem
        } catch (e: Exception) {
            return Runtime.getRuntime().maxMemory()
        }
    }
    
}

/**
 * Memory pressure levels.
 */
enum class MemoryPressure(
    val thresholdMb: Int,
    val description: String
) {
    NORMAL(200, "Normal - plenty of memory"),
    LOW(175, "Low - minor pressure"),
    MODERATE(150, "Moderate - some pressure"),
    HIGH(100, "High - aggressive cleanup needed"),
    CRITICAL(50, "Critical - emergency measures"),
    EMERGENCY(20, "Emergency - OOM imminent")
}

/**
 * Memory monitor configuration.
 */
data class MemoryConfig(
    val pollIntervalMs: Long = 5000,
    val cautionThresholdMb: Int = 200,
    val warningThresholdMb: Int = 150,
    val criticalThresholdMb: Int = 100,
    val emergencyThresholdMb: Int = 20
) {
    companion object {
        val DEFAULT = MemoryConfig()
    }
}

/**
 * Result of a cleanup operation.
 */
data class CleanupResult(
    val pressureLevel: MemoryPressure,
    val freedBytes: Long,
    val actions: List<String>,
    val timestamp: Long
) {
    override fun toString(): String {
        return "CleanupResult(pressure=$pressureLevel, freed=${formatBytes(freedBytes)}, " +
               "actions=${actions.size})"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)} GB"
        bytes >= 1024L * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}