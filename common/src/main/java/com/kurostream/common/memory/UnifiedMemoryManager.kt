package com.kurostream.common.memory
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Consolidated memory manager replacing 4 separate implementations.
 * Provides unified memory monitoring, trimming, and optimization.
 */
@Singleton
class UnifiedMemoryManager @Inject constructor(
    private val context: Context
) {
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    private val trimCallbacks = mutableListOf<(Int) -> Unit>()
    private var lastGCTime = 0L
    private var gcCount = 0

    val isLowMemoryDevice: Boolean by lazy {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.memoryClass < 256
    }

    val totalMemoryMb: Int by lazy {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memInfo)
        (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    init {
        updateMemoryState()
        Timber.d("UnifiedMemoryManager initialized: total=${totalMemoryMb}MB, lowMemDevice=$isLowMemoryDevice")
    }

    fun updateMemoryState() {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.getMemoryInfo(memInfo)

        val availMem = memInfo.availMem / (1024 * 1024)
        val lowMemory = memInfo.lowMemory

        val debugMemInfo = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(debugMemInfo)
        val pss = debugMemInfo.totalPss / 1024
        val nativeHeap = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        val targetMemory = calculateTargetMemory(availMem.toInt())
        val isCritical = availMem < 25 || pss > (activityManager.memoryClass * 0.9).toInt()

        _memoryState.value = MemoryState(
            totalPssMb = pss.toInt(),
            nativeHeapMb = nativeHeap,
            dalvikHeapMb = nativeHeap,
            availableMemoryMb = availMem.toInt(),
            targetMemoryMb = targetMemory,
            isLowMemory = lowMemory,
            isCritical = isCritical,
            memoryClass = activityManager.memoryClass,
            isLowMemoryDevice = isLowMemoryDevice,
            totalMemoryMb = totalMemoryMb,
            timestamp = System.currentTimeMillis(),
        )

        if (isCritical) {
            triggerAggressiveGC("Critical memory")
        } else if (pss > targetMemory) {
            triggerGC("Above target")
        }
    }

    private fun calculateTargetMemory(availMemMb: Int): Int {
        val deviceCategory = when {
            totalMemoryMb >= 8192 -> "high"
            totalMemoryMb >= 4096 -> "medium"
            else -> "low"
        }

        val baseTarget = when (deviceCategory) {
            "high" -> (_memoryState.value.memoryClass * 0.5).toInt()
            "medium" -> (_memoryState.value.memoryClass * 0.4).toInt()
            "low" -> (_memoryState.value.memoryClass * 0.3).toInt()
            else -> 100
        }

        val adaptiveTarget = (availMemMb * 0.3).toInt().coerceAtLeast(25)
        return baseTarget.coerceAtMost(adaptiveTarget)
    }

    fun getAvailableBudgetMb(): Int {
        updateMemoryState()
        val state = _memoryState.value
        return (state.availableMemoryMb - 25).coerceAtLeast(0)
    }

    fun allocateBuffer(sizeMb: Int): Boolean {
        val budget = getAvailableBudgetMb()
        if (sizeMb > budget) {
            Timber.w("Buffer allocation denied: requested ${sizeMb}MB, budget ${budget}MB")
            triggerGC("Pre-allocation")
            return getAvailableBudgetMb() >= sizeMb
        }
        return true
    }

    fun trimMemory(level: Int) {
        trimCallbacks.forEach { it(level) }
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                if (shouldGC()) performGC("UI hidden")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                if (shouldGC()) performGC("Running moderate")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                triggerAggressiveGC("Running low")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                triggerAggressiveGC("Running critical")
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                if (shouldGC()) performGC("Background")
            }
        }
    }

    private fun shouldGC(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastGCTime > 30000 && gcCount < 5
    }

    fun triggerGC(reason: String) {
        if (!shouldGC()) {
            Timber.d("GC skipped: $reason (cooldown or limit)")
            return
        }
        performGC(reason)
    }

    private fun performGC(reason: String) {
        val before = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            System.gc()
            Runtime.getRuntime().gc()
            
            lastGCTime = System.currentTimeMillis()
            gcCount++
            
            val after = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
            val saved = before - after
            
            Timber.d("GC ($reason): ${before}MB → ${after}MB (saved: ${saved}MB, count: $gcCount)")
            
            if (gcCount >= 5) {
                Timber.w("GC limit reached (5), resetting counter")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    gcCount = 0
                }, 60000)
            }
        }
    }

    fun triggerAggressiveGC(reason: String) {
        Timber.w("Aggressive GC: $reason")
        
        System.gc()
        Runtime.getRuntime().gc()
        Runtime.getRuntime().runFinalization()
        
        lastGCTime = System.currentTimeMillis()
        gcCount = 0
        
        updateMemoryState()
        val state = _memoryState.value
        Timber.d("Aggressive GC complete: ${state.totalPssMb}MB PSS")
    }

    fun registerTrimCallback(callback: (Int) -> Unit) {
        trimCallbacks.add(callback)
    }

    fun unregisterTrimCallback(callback: (Int) -> Unit) {
        trimCallbacks.remove(callback)
    }

    fun clearAllCaches() {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        context.externalCacheDir?.deleteRecursively()
        context.externalCacheDir?.mkdirs()
        Timber.d("All caches cleared")
    }

    fun getMemoryPressure(): Float {
        updateMemoryState()
        val state = _memoryState.value
        return if (state.memoryClass > 0) {
            state.totalPssMb.toFloat() / state.memoryClass
        } else 1f
    }

    fun isAllocationSafe(sizeMb: Int): Boolean {
        updateMemoryState()
        val state = _memoryState.value
        val projectedPss = state.totalPssMb + sizeMb
        return projectedPss < state.targetMemoryMb && !state.isCritical
    }

    fun reset() {
        gcCount = 0
        lastGCTime = 0
        trimCallbacks.clear()
        Timber.d("UnifiedMemoryManager reset")
    }

    fun getOptimalBitmapConfig(): android.graphics.Bitmap.Config {
        return when {
            isLowMemoryDevice -> android.graphics.Bitmap.Config.RGB_565
            totalMemoryMb < 4096 -> android.graphics.Bitmap.Config.RGB_565
            else -> android.graphics.Bitmap.Config.ARGB_8888
        }
    }

    fun getOptimalThreadPoolSize(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        val isPowerSave = (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isPowerSaveMode
        return when {
            isPowerSave -> cores.coerceIn(2, 4)
            isLowMemoryDevice -> cores.coerceIn(2, 6)
            else -> (cores * 2).coerceAtMost(8)
        }
    }
}

data class MemoryState(
    val totalPssMb: Int = 0,
    val nativeHeapMb: Long = 0,
    val dalvikHeapMb: Long = 0,
    val availableMemoryMb: Int = 0,
    val targetMemoryMb: Int = 100,
    val isLowMemory: Boolean = false,
    val isCritical: Boolean = false,
    val memoryClass: Int = 256,
    val isLowMemoryDevice: Boolean = false,
    val totalMemoryMb: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val memoryPressure: Float get() = totalPssMb.toFloat() / memoryClass
    val headroomMb: Int get() = (targetMemoryMb - totalPssMb).coerceAtLeast(0)
    val isHealthy: Boolean get() = totalPssMb < targetMemoryMb && !isCritical
    val recommendedAction: String
        get() = when {
            isCritical -> "IMMEDIATE_GC"
            totalPssMb > targetMemoryMb -> "TRIM_MEMORY"
            headroomMb < 50 -> "MONITOR"
            else -> "OK"
        }
}
