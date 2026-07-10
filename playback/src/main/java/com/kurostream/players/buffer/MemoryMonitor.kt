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

package com.kurostream.players.buffer

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Memory pressure monitor that observes system memory and triggers
 * aggressive cleanup when available memory drops below thresholds.
 * 
 * Integrates with ThermalGuard for coordinated throttling.
 */
class MemoryMonitor private constructor(
    private val context: Context
) {
    private val TAG = "MemoryMonitor"
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    
    // Memory thresholds
    private val criticalThresholdMb = 50  // < 50MB: critical
    private val warningThresholdMb = 100  // < 100MB: warning
    private val normalThresholdMb = 200   // > 200MB: normal
    
    // State
    private val _memoryState = MutableStateFlow(MemoryState.Normal)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    private val _availableMemoryMb = MutableStateFlow(0)
    val availableMemoryMb: StateFlow<Int> = _availableMemoryMb.asStateFlow()
    
    private val _totalMemoryMb = MutableStateFlow(0)
    val totalMemoryMb: StateFlow<Int> = _totalMemoryMb.asStateFlow()
    
    // Callbacks for components to register
    private val memoryPressureCallbacks = mutableListOf<WeakReference<MemoryPressureCallback>>()
    
    // Component callbacks for specific subsystems
    private var coilCleanup: (() -> Unit)? = null
    private var diskBufferCleanup: (() -> Unit)? = null
    private var metadataCacheCleanup: (() -> Unit)? = null
    private var playbackBufferCleanup: (() -> Unit)? = null
    private var uiAnimationReducer: ((Int) -> Unit)? = null
    private var backgroundTaskCanceller: (() -> Unit)? = null
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: MemoryMonitor? = null
        
        fun getInstance(context: Context): MemoryMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    init {
        // Register as ComponentCallbacks2 for system memory callbacks
        (context as? androidx.lifecycle.DefaultLifecycleObserver)?.let { /* if app has lifecycle */ }
        // We'll also use ActivityManager for polling
        startMonitoring()
    }
    
    /**
     * Start periodic memory monitoring.
     */
    fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                checkMemoryAndUpdate()
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
            }
        }
    }
    
    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
    
    /**
     * Register a callback for memory pressure notifications.
     */
    fun registerCallback(callback: MemoryPressureCallback) {
        memoryPressureCallbacks.add(WeakReference(callback))
    }
    
    /**
     * Unregister a callback.
     */
    fun unregisterCallback(callback: MemoryPressureCallback) {
        memoryPressureCallbacks.removeAll { it.get() == callback }
    }
    
    /**
     * Set Coil memory cache cleanup action.
     */
    fun setCoilCleanup(action: () -> Unit) {
        coilCleanup = action
    }
    
    /**
     * Set disk buffer cleanup action.
     */
    fun setDiskBufferCleanup(action: () -> Unit) {
        diskBufferCleanup = action
    }
    
    /**
     * Set metadata cache cleanup action.
     */
    fun setMetadataCacheCleanup(action: () -> Unit) {
        metadataCacheCleanup = action
    }
    
    /**
     * Set playback buffer cleanup action.
     */
    fun setPlaybackBufferCleanup(action: () -> Unit) {
        playbackBufferCleanup = action
    }
    
    /**
     * Set UI animation FPS reducer.
     */
    fun setUiAnimationReducer(reducer: (Int) -> Unit) {
        uiAnimationReducer = reducer
    }
    
    /**
     * Set background task canceller.
     */
    fun setBackgroundTaskCanceller(canceller: () -> Unit) {
        backgroundTaskCanceller = canceller
    }
    
    /**
     * Force immediate memory check and cleanup if needed.
     */
    fun forceCheckAndCleanup(): MemoryState {
        return checkMemoryAndUpdate()
    }
    
    /**
     * Get current memory info snapshot.
     */
    fun getMemoryInfo(): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            availableMb = memInfo.availMem / 1024 / 1024,
            totalMb = memInfo.totalMem / 1024 / 1024,
            thresholdMb = memInfo.threshold / 1024 / 1024,
            isLowMemory = memInfo.lowMemory,
            state = _memoryState.value
        )
    }
    
    /**
     * Shutdown monitor.
     */
    fun shutdown() {
        stopMonitoring()
        scope.coroutineContext.cancel()
        memoryPressureCallbacks.clear()
    }
    
    // ===== Private Implementation =====
    
    private fun checkMemoryAndUpdate(): MemoryState {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        
        val availableMb = (memInfo.availMem / 1024 / 1024).toInt()
        val totalMb = (memInfo.totalMem / 1024 / 1024).toInt()
        
        _availableMemoryMb.value = availableMb
        _totalMemoryMb.value = totalMb
        
        val newState = when {
            availableMb < criticalThresholdMb -> MemoryState.Critical
            availableMb < warningThresholdMb -> MemoryState.Warning
            else -> MemoryState.Normal
        }
        
        if (newState != _memoryState.value) {
            _memoryState.value = newState
            onMemoryStateChanged(newState, availableMb)
        }
        
        return newState
    }
    
    private fun onMemoryStateChanged(state: MemoryState, availableMb: Int) {
        Log.i(TAG, "Memory state changed: $state (${availableMb}MB available)")
        
        // Notify callbacks
        memoryPressureCallbacks.forEach { ref ->
            ref.get()?.onMemoryPressure(state, availableMb)
        }
        
        // Execute cleanup actions based on severity
        when (state) {
            MemoryState.Critical -> {
                // Aggressive cleanup
                Log.w(TAG, "CRITICAL memory: ${availableMb}MB - Executing aggressive cleanup")
                
                // 1. Clear Coil memory cache completely
                coilCleanup?.invoke()
                
                // 2. Flush and trim disk buffer
                diskBufferCleanup?.invoke()
                
                // 3. Clear metadata caches
                metadataCacheCleanup?.invoke()
                
                // 4. Reduce playback buffer read-ahead
                playbackBufferCleanup?.invoke()
                
                // 5. Reduce UI animations to minimum
                uiAnimationReducer?.invoke(24) // 24 FPS minimum
                
                // 6. Cancel all background prefetch tasks
                backgroundTaskCanceller?.invoke()
                
                // 7. Force GC
                System.gc()
                System.runFinalization()
                
            }
            MemoryState.Warning -> {
                Log.w(TAG, "WARNING memory: ${availableMb}MB - Executing moderate cleanup")
                
                // 1. Reduce Coil cache size
                coilCleanup?.invoke()
                
                // 2. Trim disk buffer read-ahead
                diskBufferCleanup?.invoke()
                
                // 3. Reduce UI animations
                uiAnimationReducer?.invoke(30)
                
                // 4. Cancel non-critical background tasks
                backgroundTaskCanceller?.invoke()
            }
            MemoryState.Normal -> {
                Log.d(TAG, "Memory normalized: ${availableMb}MB")
                
                // Restore UI animations
                uiAnimationReducer?.invoke(60)
            }
        }
    }
    
    /**
     * Memory pressure states.
     */
    enum class MemoryState {
        Normal,     // > 200MB available
        Warning,    // 50-200MB available
        Critical    // < 50MB available
    }
    
    /**
     * Memory info snapshot.
     */
    data class MemoryInfo(
        val availableMb: Int,
        val totalMb: Int,
        val thresholdMb: Int,
        val isLowMemory: Boolean,
        val state: MemoryState
    )
    
    /**
     * Callback interface for memory pressure notifications.
     */
    interface MemoryPressureCallback {
        fun onMemoryPressure(state: MemoryState, availableMb: Int)
    }
}

/**
 * ComponentCallbacks2 implementation for system memory callbacks.
 * Can be registered in Application.onCreate().
 */
class MemoryPressureComponentCallbacks(
    private val monitor: MemoryMonitor
) : ComponentCallbacks2 {
    
    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.d("MemoryMonitor", "TRIM_MEMORY_UI_HIDDEN")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w("MemoryMonitor", "TRIM_MEMORY_RUNNING_LOW")
                monitor.forceCheckAndCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                Log.d("MemoryMonitor", "TRIM_MEMORY_BACKGROUND")
                monitor.forceCheckAndCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w("MemoryMonitor", "TRIM_MEMORY_MODERATE/COMPLETE")
                monitor.forceCheckAndCleanup()
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
    
    override fun onLowMemory() {
        Log.e("MemoryMonitor", "SYSTEM onLowMemory!")
        monitor.forceCheckAndCleanup()
    }
}