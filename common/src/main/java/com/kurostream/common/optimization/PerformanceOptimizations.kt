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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import timber.log.Timber

/**
 * Battery-aware behavior manager that adjusts app behavior based on battery state
 */
class BatteryAwareManager private constructor(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var isBatterySaverEnabled = false
    private var batteryLevel = 100
    private var isCharging = false
    private val job = SupervisorJob()
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    // val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) // unused

                    batteryLevel = (level * 100) / scale
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    isBatterySaverEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        powerManager.isPowerSaveMode
                    } else false

                    onBatteryStateChanged()
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    isBatterySaverEnabled = powerManager.isPowerSaveMode
                    onBatteryStateChanged()
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private fun onBatteryStateChanged() {
        Timber.d("Battery state: level=$batteryLevel%, charging=$isCharging, saver=$isBatterySaverEnabled")
        
        // Adjust behavior based on battery state
        val shouldReduceNetwork = isBatterySaverEnabled || (batteryLevel < 20 && !isCharging)
        val shouldReduceBackground = isBatterySaverEnabled || (batteryLevel < 15 && !isCharging)
        val shouldReduceAnimations = isBatterySaverEnabled || batteryLevel < 10
        
        // Notify listeners
        batteryStateCallbacks.forEach { it(shouldReduceNetwork, shouldReduceBackground, shouldReduceAnimations) }
    }

    private val batteryStateCallbacks = mutableListOf<(Boolean, Boolean, Boolean) -> Unit>()

    fun addBatteryStateListener(callback: (reduceNetwork: Boolean, reduceBackground: Boolean, reduceAnimations: Boolean) -> Unit) {
        batteryStateCallbacks.add(callback)
    }

    fun removeBatteryStateListener(callback: (reduceNetwork: Boolean, reduceBackground: Boolean, reduceAnimations: Boolean) -> Unit) {
        batteryStateCallbacks.remove(callback)
    }

    fun shouldReduceNetworkPolling(): Boolean = isBatterySaverEnabled || (batteryLevel < 20 && !isCharging)
    fun shouldReduceBackgroundSync(): Boolean = isBatterySaverEnabled || (batteryLevel < 15 && !isCharging)
    fun shouldReduceAnimations(): Boolean = isBatterySaverEnabled || batteryLevel < 10
    fun shouldReduceImageQuality(): Boolean = isBatterySaverEnabled || (batteryLevel < 15 && !isCharging)
    fun shouldReduceBufferSize(): Boolean = isBatterySaverEnabled || (batteryLevel < 20 && !isCharging)

    fun shutdown() {
        job.cancel()
        context.unregisterReceiver(batteryReceiver)
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun create(context: Context): BatteryAwareManager {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            return BatteryAwareManager(context.applicationContext, scope)
        }
    }
}

/**
 * Startup profiler for measuring cold/warm start times
 */
class StartupProfiler {
    private var startTime: Long = 0
    private var firstDrawTime: Long = 0
    private var fullyDrawnTime: Long = 0

    companion object {
        private const val TAG = "StartupProfiler"
    }

    fun markProcessStart() {
        startTime = System.currentTimeMillis()
    }

    fun markFirstDraw() {
        firstDrawTime = System.currentTimeMillis()
    }

    fun markFullyDrawn() {
        fullyDrawnTime = System.currentTimeMillis()
        reportMetrics()
    }

    private fun reportMetrics() {
        val coldStartTime = firstDrawTime - startTime
        val fullyDrawnTime = fullyDrawnTime - startTime
        
        Timber.tag(TAG).i("Startup metrics - Cold start: ${coldStartTime}ms, Fully drawn: ${fullyDrawnTime}ms")
        
        // Report to Firebase Performance or analytics
        // FirebasePerformance.getInstance().newTrace("app_start").apply {
        //     putMetric("cold_start_ms", coldStartTime)
        //     putMetric("fully_drawn_ms", fullyDrawnTime)
        //     stop()
        // }
    }

    fun getMetrics(): StartupMetrics {
        return StartupMetrics(
            coldStartMs = if (firstDrawTime > 0) firstDrawTime - startTime else 0,
            fullyDrawnMs = if (fullyDrawnTime > 0) fullyDrawnTime - startTime else 0,
            processStartMs = startTime,
        )
    }
}

data class StartupMetrics(
    val coldStartMs: Long,
    val fullyDrawnMs: Long,
    val processStartMs: Long,
)

/**
 * Network call deduplication manager
 */
class NetworkDeduplicator {
    private val inFlightRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<*>>()
    private val lock = Any()

    suspend fun <T> execute(key: String, request: suspend () -> T): T {
        val existing = synchronized(lock) {
            inFlightRequests[key] as? kotlinx.coroutines.Deferred<T>
        }
        
        if (existing != null) {
            return existing.await()
        }
        
        val deferred = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).async {
            try {
                request()
            } finally {
                synchronized(lock) {
                    inFlightRequests.remove(key)
                }
            }
        }
        
        synchronized(lock) {
            inFlightRequests[key] = deferred
        }
        
        return deferred.await()
    }
}

/**
 * Lazy layout optimization helpers
 */
object LazyLayoutOptimizations {
    fun <T : Any> stableKey(item: T): Any = item

    fun <T> generateStableKeys(items: List<T>, keySelector: (T) -> Any): List<Any> {
        return items.map(keySelector)
    }
}

/**
 * Paging 3 configuration for large lists
 */
object PagingConfig {
    const val PAGE_SIZE = 20
    const val PREFETCH_DISTANCE = 10
    const val INITIAL_LOAD_SIZE = 30
    const val MAX_SIZE = 200
    const val ENABLE_PLACEHOLDERS = false
}

/**
 * Coil image caching configuration for low-memory devices
 */
object CoilCacheConfig {
    private var _memoryCacheSize: Long? = null
    private var _diskCacheSize: Long? = null

    fun memoryCacheSize(context: Context): Long {
        var result = _memoryCacheSize
        if (result == null) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryClass = activityManager.memoryClass
            result = (memoryClass * 1024L * 1024L / 16).coerceAtMost(64 * 1024 * 1024)
            _memoryCacheSize = result
        }
        return result
    }

    fun diskCacheSize(context: Context): Long {
        var result = _diskCacheSize
        if (result == null) {
            val cacheDir = context.cacheDir
            val usableSpace = cacheDir.usableSpace
            result = (usableSpace / 20).coerceAtMost(128 * 1024 * 1024)
            _diskCacheSize = result
        }
        return result
    }
}