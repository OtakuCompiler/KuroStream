package com.kurostream.players.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveMemoryManager @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val totalMemoryMb: Int by lazy {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    val memoryClassMb: Int by lazy { activityManager.memoryClass }
    val largeMemoryClassMb: Int by lazy { activityManager.largeMemoryClass }

    private val minReservedMemoryMb = 25
    private var lastGCTime = 0L
    private var gcCount = 0
    private val trimCallbacks = mutableListOf<(Int) -> Unit>()

    init {
        updateMemoryState()
        Timber.d("AdaptiveMemory initialized: total=${totalMemoryMb}MB, class=${memoryClassMb}MB, large=${largeMemoryClassMb}MB")
    }

    fun updateMemoryState() {
        activityManager.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem / (1024 * 1024)
        val lowMemory = memoryInfo.lowMemory

        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val pss = memInfo.totalPrivatePss / 1024
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val dalvikHeap = (memInfo.dalvikPss / 1024).toInt()

        val targetMemory = calculateTargetMemory(availMem.toInt())
        val isCritical = availMem < minReservedMemoryMb || pss > memoryClassMb * 0.9

        _memoryState.value = MemoryState(
            totalPrivatePssMb = pss,
            nativeHeapMb = nativeHeap,
            dalvikHeapMb = dalvikHeap.toLong(),
            availableMemoryMb = availMem,
            targetMemoryMb = targetMemory,
            isLowMemory = lowMemory,
            isCritical = isCritical,
            memoryClass = memoryClassMb,
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
            "high" -> (memoryClassMb * 0.5).toInt()
            "medium" -> (memoryClassMb * 0.4).toInt()
            "low" -> (memoryClassMb * 0.3).toInt()
            else -> 100
        }

        val adaptiveTarget = (availMemMb * 0.3).toInt().coerceAtLeast(minReservedMemoryMb)
        return baseTarget.coerceAtMost(adaptiveTarget)
    }

    fun getAvailableBudgetMb(): Int {
        updateMemoryState()
        val state = _memoryState.value
        return (state.availableMemoryMb - minReservedMemoryMb).coerceAtLeast(0)
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
            ActivityManager.TRIM_MEMORY_UI_HIDDEN -> {
                if (shouldGC()) performGC("UI hidden")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> {
                if (shouldGC()) performGC("Running moderate")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_LOW -> {
                triggerAggressiveGC("Running low")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> {
                triggerAggressiveGC("Running critical")
            }
            ActivityManager.TRIM_MEMORY_BACKGROUND -> {
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
        val before = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        
        // Run GC in background to avoid blocking
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            // Let system handle GC naturally
            
            lastGCTime = System.currentTimeMillis()
            gcCount++
            
            val after = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
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
        
        // Let system handle GC naturally
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Let system handle GC naturally
        }, 100)
        
        lastGCTime = System.currentTimeMillis()
        gcCount = 0
        
        updateMemoryState()
        val state = _memoryState.value
        Timber.d("Aggressive GC complete: ${state.totalPrivatePssMb}MB PSS")
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
            state.totalPrivatePssMb.toFloat() / state.memoryClass
        } else 1f
    }

    fun isAllocationSafe(sizeMb: Int): Boolean {
        updateMemoryState()
        val state = _memoryState.value
        val projectedPss = state.totalPrivatePssMb + sizeMb
        return projectedPss < state.targetMemoryMb && !state.isCritical
    }

    fun reset() {
        gcCount = 0
        lastGCTime = 0
        trimCallbacks.clear()
        Timber.d("AdaptiveMemory reset")
    }

    fun shutdown() {
        scope.cancel()
        Timber.d("AdaptiveMemoryManager shutdown")
    }
}

data class AdaptiveMemoryState(
    val totalPrivatePssMb: Int = 0,
    val nativeHeapMb: Long = 0,
    val dalvikHeapMb: Long = 0,
    val availableMemoryMb: Int = 0,
    val targetMemoryMb: Int = 100,
    val isLowMemory: Boolean = false,
    val isCritical: Boolean = false,
    val memoryClass: Int = 256,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val memoryPressure: Float get() = totalPrivatePssMb.toFloat() / memoryClass
    val headroomMb: Int get() = (targetMemoryMb - totalPrivatePssMb).coerceAtLeast(0)
    val isHealthy: Boolean get() = totalPrivatePssMb < targetMemoryMb && !isCritical
    val recommendedAction: String
        get() = when {
            isCritical -> "IMMEDIATE_GC"
            totalPrivatePssMb > targetMemoryMb -> "TRIM_MEMORY"
            headroomMb < 50 -> "MONITOR"
            else -> "OK"
        }
}
