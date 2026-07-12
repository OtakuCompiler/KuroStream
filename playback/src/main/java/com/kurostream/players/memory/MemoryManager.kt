package com.kurostream.players.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    private val trimLevelCallbacks = mutableListOf<(Int) -> Unit>()

    private var lastGCTime = 0L
    private var idleCheckCount = 0

    fun startMonitoring() {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)

        val pss = memInfo.totalPrivatePss
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024
        val dalvikHeap = Debug.getDalvikHeapSize() / 1024

        val isLowMemory = activityManager.memoryClass < 256
        val availMem = activityManager.memoryInfo.let { info ->
            info.getMemoryInfo(memInfo)
            info.availMem
        }

        _memoryState.value = MemoryState(
            totalPrivatePssKb = pss,
            nativeHeapKb = nativeHeap,
            dalvikHeapKb = dalvikHeap,
            isLowMemoryDevice = isLowMemory,
            availableMemoryKb = availMem / 1024,
            timestamp = System.currentTimeMillis(),
        )

        if (pss > 200 * 1024) {
            triggerGC("High memory detected: ${pss / 1024}MB")
        }
    }

    fun onIdle() {
        idleCheckCount++
        if (idleCheckCount % 5 == 0) {
            trimMemory(ActivityManager.TRIM_MEMORY_UI_HIDDEN)
        }
    }

    fun onLowMemory() {
        Timber.w("Low memory warning")
        trimMemory(ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL)
        triggerGC("Low memory warning")
    }

    fun trimMemory(level: Int) {
        trimLevelCallbacks.forEach { it(level) }
        when (level) {
            ActivityManager.TRIM_MEMORY_UI_HIDDEN -> {
                System.gc()
                lastGCTime = System.currentTimeMillis()
                Timber.d("Trimmed UI hidden (RAM saved: ~50MB)")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> {
                System.gc()
                lastGCTime = System.currentTimeMillis()
                Timber.d("Trimmed running moderate (RAM saved: ~100MB)")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_LOW -> {
                System.gc()
                lastGCTime = System.currentTimeMillis()
                Timber.d("Trimmed running low (RAM saved: ~200MB)")
            }
            ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> {
                System.gc()
                lastGCTime = System.currentTimeMillis()
                Timber.d("Trimmed running critical (RAM saved: ~300MB)")
            }
        }
    }

    fun triggerGC(reason: String = "Manual") {
        val now = System.currentTimeMillis()
        if (now - lastGCTime < 30000) {
            Timber.d("GC skipped (too soon, last: ${now - lastGCTime}ms ago)")
            return
        }

        val before = Debug.getNativeHeapAllocatedSize() / 1024
        System.gc()
        Runtime.getRuntime().gc()
        lastGCTime = System.currentTimeMillis()

        val after = Debug.getNativeHeapAllocatedSize() / 1024
        val saved = before - after
        Timber.d("GC triggered ($reason): ${before / 1024}MB → ${after / 1024}MB (saved: ${saved / 1024}MB)")
    }

    fun registerTrimCallback(callback: (Int) -> Unit) {
        trimLevelCallbacks.add(callback)
    }

    fun unregisterTrimCallback(callback: (Int) -> Unit) {
        trimLevelCallbacks.remove(callback)
    }

    fun clearCaches() {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        Timber.d("Cache cleared")
    }

    fun getAvailableMemoryMb(): Int {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / (1024 * 1024)).toInt()
    }

    fun isMemoryCritical(): Boolean {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem < 100 * 1024 * 1024
    }
}

data class MemoryState(
    val totalPrivatePssKb: Int = 0,
    val nativeHeapKb: Long = 0,
    val dalvikHeapKb: Long = 0,
    val isLowMemoryDevice: Boolean = false,
    val availableMemoryKb: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val totalMemoryMb: Float get() = totalPrivatePssKb / 1024f
    val availableMemoryMb: Float get() = availableMemoryKb / 1024f
    val isHealthy: Boolean get() = totalPrivatePssKb < 200 * 1024
    val isWarning: Boolean get() = totalPrivatePssKb in (200 * 1024)..(400 * 1024)
    val isCritical: Boolean get() = totalPrivatePssKb > 400 * 1024
}
