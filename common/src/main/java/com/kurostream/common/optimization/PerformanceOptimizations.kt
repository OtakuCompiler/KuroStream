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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

                    batteryLevel = (level * 100) / scale
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    isBatterySaverEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        powerManager.isPowerSaveMode
                    } else false

                    onBatteryStateChanged()
                }
                Intent.ACTION_POWER_SAVE_MODE_CHANGED -> {
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
            addAction(Intent.ACTION_POWER_SAVE_MODE_CHANGED)
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
 * Thread scheduler for optimized coroutine dispatching
 */
object ThreadScheduler {
    val IO: CoroutineDispatcher = Dispatchers.IO
    val DEFAULT: CoroutineDispatcher = Dispatchers.Default
    val MAIN: CoroutineDispatcher = Dispatchers.Main
    
    // Dedicated dispatchers for specific tasks
    val NETWORK_PARSING = Dispatchers.Default.limitedParallelism(2)
    val IMAGE_PROCESSING = Dispatchers.Default.limitedParallelism(2)
    val DATABASE = Dispatchers.IO.limitedParallelism(2)
    val BACKGROUND_SYNC = Dispatchers.IO.limitedParallelism(1)
    
    fun withContext(context: CoroutineDispatcher, block: suspend () -> Unit) {
        CoroutineScope(context).launch { block() }
    }
}

/**
 * Network call deduplication manager
 */
class NetworkDeduplicator {
    private val inFlightRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<Any>>()
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
 * Bitmap pooling for Coil to reduce GC pressure
 */
class BitmapPoolManager {
    private val pools = mutableMapOf<Int, android.util.LruCache<Int, android.graphics.Bitmap>>()
    private val maxPoolSize = 50 * 1024 * 1024 // 50MB

    fun getBitmap(width: Int, height: Int, config: android.graphics.Bitmap.Config = android.graphics.Bitmap.Config.ARGB_8888): android.graphics.Bitmap? {
        val key = (width * height * 4).hashCode() // Approximate size
        val pool = pools[key] ?: return null
        return pool.get(key)
    }

    fun putBitmap(bitmap: android.graphics.Bitmap) {
        val key = (bitmap.width * bitmap.height * 4).hashCode()
        val pool = pools.getOrPut(key) {
            android.util.LruCache<Int, android.graphics.Bitmap>(maxPoolSize / 4) { _, old ->
                old.byteCount.toLong()
            }
        }
        pool.put(key, bitmap)
    }

    fun clear() {
        pools.values.forEach { it.evictAll() }
    }

    fun resize(newMaxSize: Long) {
        // Recreate pools with new size
    }
}

/**
 * Lazy layout optimization helpers
 */
object LazyLayoutOptimizations {
    fun <T> stableKey(item: T): Any = item

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
    fun memoryCacheSize(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryClass = activityManager.memoryClass
        // Use 1/8th of available memory, max 256MB
        return (memoryClass * 1024L * 1024L / 8).coerceAtMost(256 * 1024 * 1024)
    }

    fun diskCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        val usableSpace = cacheDir.usableSpace
        // Use 10% of available space, max 256MB
        return (usableSpace / 10).coerceAtMost(256 * 1024 * 1024)
    }
}