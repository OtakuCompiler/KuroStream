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

package com.kurostream.common.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kurostream.common.memory.MemoryMonitor
import com.kurostream.common.thermal.ThermalGuard
import com.kurostream.extensions.PluginManager
import com.kurostream.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Lazy Service Stop Manager
 * 
 * Unloads heavy services after 30 seconds of idle time to reduce memory usage.
 * Services are automatically restarted when needed.
 * 
 * Target: <45MB idle RAM, <55MB during 1080p streaming
 */
class LazyServiceStopManager private constructor(
    private val context: Context,
    private val config: ServiceStopConfig = ServiceStopConfig.DEFAULT
) {
    private val TAG = "LazyServiceStopManager"
    
    // Service references
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null
    private var syncManager: SyncManager? = null
    private var pluginManager: PluginManager? = null
    
    // State
    private val _state = MutableStateFlow(ServiceStopState())
    val state: StateFlow<ServiceStopState> = _state.asStateFlow()
    
    private val isActive = AtomicBoolean(false)
    private val lastActivityTime = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    
    // Memory and thermal monitors
    private val memoryMonitor = MemoryMonitor.getInstance(context)
    private val thermalGuard = ThermalGuard.getInstance(context)
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: LazyServiceStopManager? = null
        
        fun getInstance(context: Context, config: ServiceStopConfig = ServiceStopConfig.DEFAULT): LazyServiceStopManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LazyServiceStopManager(context.applicationContext, config).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    init {
        // Initialize with current services
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        syncManager = SyncManager.getInstance(context)
        pluginManager = PluginManager.getInstance(context)
        
        // Register memory pressure callback
        memoryMonitor.addPressureCallback { pressure ->
            onMemoryPressure(pressure)
        }
        
        // Start monitoring
        startMonitoring()
    }
    
    /**
     * Start monitoring for idle time.
     */
    fun startMonitoring() {
        if (isActive.getAndSet(true)) return
        
        // Reset last activity time
        lastActivityTime.set(System.currentTimeMillis())
        
        // Start monitoring loop
        scope.launch {
            while (isActive.get()) {
                checkIdleTime()
                delay(config.checkIntervalMs)
            }
        }
        
        Timber.i("Lazy service stop monitoring started")
    }
    
    /**
     * Stop monitoring.
     */
    fun stopMonitoring() {
        isActive.set(false)
        scope.cancel()
        Timber.i("Lazy service stop monitoring stopped")
    }
    
    /**
     * Notify that user activity occurred.
     */
    fun notifyActivity() {
        lastActivityTime.set(System.currentTimeMillis())
        
        // Restart services if they were stopped
        restartStoppedServices()
    }
    
    /**
     * Shutdown and release resources.
     */
    fun shutdown() {
        stopMonitoring()
        stopAllServices()
        memoryMonitor.removePressureCallback { onMemoryPressure(it) }
    }
    
    /**
     * Check if services should be stopped due to idle time.
     */
    private suspend fun checkIdleTime() {
        val idleTime = System.currentTimeMillis() - lastActivityTime.get()
        
        if (idleTime >= config.idleTimeoutMs) {
            // Check if we're under memory pressure
            val memoryPressure = memoryMonitor.getCurrentPressure()
            val thermalPressure = thermalGuard.getCurrentThrottleStage()
            
            if (memoryPressure.ordinal >= config.memoryPressureThreshold.ordinal ||
                thermalPressure.ordinal >= config.thermalPressureThreshold.ordinal) {
                
                // Stop services to free memory
                stopHeavyServices()
            }
        }
    }
    
    /**
     * Stop heavy services to free memory.
     */
    private fun stopHeavyServices() {
        Timber.i("Stopping heavy services due to idle time")
        
        // Stop Firebase services
        stopFirebaseServices()
        
        // Stop sync manager
        stopSyncManager()
        
        // Stop plugin manager
        stopPluginManager()
        
        // Update state
        updateState()
    }
    
    /**
     * Restart services that were stopped.
     */
    private fun restartStoppedServices() {
        if (_state.value.firebaseAuthStopped) {
            restartFirebaseAuth()
        }
        if (_state.value.firebaseFirestoreStopped) {
            restartFirebaseFirestore()
        }
        if (_state.value.syncManagerStopped) {
            restartSyncManager()
        }
        if (_state.value.pluginManagerStopped) {
            restartPluginManager()
        }
    }
    
    /**
     * Stop Firebase Auth service.
     */
    private fun stopFirebaseAuth() {
        try {
            firebaseAuth?.let { auth ->
                // Sign out to release resources
                auth.signOut()
                // Clear instance
                firebaseAuth = null
                _state.value = _state.value.copy(firebaseAuthStopped = true)
                Timber.i("Firebase Auth stopped")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop Firebase Auth")
        }
    }
    
    /**
     * Stop Firebase Firestore service.
     */
    private fun stopFirebaseFirestore() {
        try {
            firebaseFirestore?.let { firestore ->
                // Terminate to release resources
                firestore.terminate()
                // Clear instance
                firebaseFirestore = null
                _state.value = _state.value.copy(firebaseFirestoreStopped = true)
                Timber.i("Firebase Firestore stopped")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop Firebase Firestore")
        }
    }
    
    /**
     * Stop Sync Manager.
     */
    private fun stopSyncManager() {
        try {
            syncManager?.let { sync ->
                // Stop sync
                sync.stopSync()
                // Clear instance
                syncManager = null
                _state.value = _state.value.copy(syncManagerStopped = true)
                Timber.i("Sync Manager stopped")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop Sync Manager")
        }
    }
    
    /**
     * Stop Plugin Manager.
     */
    private fun stopPluginManager() {
        try {
            pluginManager?.let { plugins ->
                // Unload all plugins
                plugins.unloadAll()
                // Clear instance
                pluginManager = null
                _state.value = _state.value.copy(pluginManagerStopped = true)
                Timber.i("Plugin Manager stopped")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop Plugin Manager")
        }
    }
    
    /**
     * Stop all services.
     */
    private fun stopAllServices() {
        stopFirebaseAuth()
        stopFirebaseFirestore()
        stopSyncManager()
        stopPluginManager()
    }
    
    /**
     * Restart Firebase Auth.
     */
    private fun restartFirebaseAuth() {
        try {
            if (firebaseAuth == null) {
                firebaseAuth = FirebaseAuth.getInstance()
                _state.value = _state.value.copy(firebaseAuthStopped = false)
                Timber.i("Firebase Auth restarted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restart Firebase Auth")
        }
    }
    
    /**
     * Restart Firebase Firestore.
     */
    private fun restartFirebaseFirestore() {
        try {
            if (firebaseFirestore == null) {
                firebaseFirestore = FirebaseFirestore.getInstance()
                _state.value = _state.value.copy(firebaseFirestoreStopped = false)
                Timber.i("Firebase Firestore restarted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restart Firebase Firestore")
        }
    }
    
    /**
     * Restart Sync Manager.
     */
    private fun restartSyncManager() {
        try {
            if (syncManager == null) {
                syncManager = SyncManager.getInstance(context)
                _state.value = _state.value.copy(syncManagerStopped = false)
                Timber.i("Sync Manager restarted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restart Sync Manager")
        }
    }
    
    /**
     * Restart Plugin Manager.
     */
    private fun restartPluginManager() {
        try {
            if (pluginManager == null) {
                pluginManager = PluginManager.getInstance(context)
                _state.value = _state.value.copy(pluginManagerStopped = false)
                Timber.i("Plugin Manager restarted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restart Plugin Manager")
        }
    }
    
    /**
     * Handle memory pressure events.
     */
    private fun onMemoryPressure(pressure: MemoryMonitor.MemoryPressure) {
        if (pressure.ordinal >= config.memoryPressureThreshold.ordinal) {
            Timber.w("High memory pressure detected: $pressure, stopping services")
            stopHeavyServices()
        }
    }
    
    /**
     * Update state with current service status.
     */
    private fun updateState() {
        _state.value = ServiceStopState(
            firebaseAuthStopped = firebaseAuth == null,
            firebaseFirestoreStopped = firebaseFirestore == null,
            syncManagerStopped = syncManager == null,
            pluginManagerStopped = pluginManager == null,
            lastActivityTime = lastActivityTime.get(),
            memoryPressure = memoryMonitor.getCurrentPressure(),
            thermalPressure = thermalGuard.getCurrentThrottleStage()
        )
    }
    
    /**
     * Get current state.
     */
    fun getState(): ServiceStopState = _state.value
    
    // ===== Data Classes =====
    
    data class ServiceStopConfig(
        val idleTimeoutMs: Long = 30_000, // 30 seconds
        val checkIntervalMs: Long = 5_000, // 5 seconds
        val memoryPressureThreshold: MemoryMonitor.MemoryPressure = MemoryMonitor.MemoryPressure.HIGH,
        val thermalPressureThreshold: ThermalGuard.ThrottleStage = ThermalGuard.ThrottleStage.WARNING
    ) {
        companion object {
            val DEFAULT = ServiceStopConfig()
        }
    }
    
    data class ServiceStopState(
        val firebaseAuthStopped: Boolean = false,
        val firebaseFirestoreStopped: Boolean = false,
        val syncManagerStopped: Boolean = false,
        val pluginManagerStopped: Boolean = false,
        val lastActivityTime: Long = 0,
        val memoryPressure: MemoryMonitor.MemoryPressure = MemoryMonitor.MemoryPressure.NORMAL,
        val thermalPressure: ThermalGuard.ThrottleStage = ThermalGuard.ThrottleStage.NONE
    ) {
        val allServicesStopped: Boolean
            get() = firebaseAuthStopped && firebaseFirestoreStopped && 
                   syncManagerStopped && pluginManagerStopped
    }
}