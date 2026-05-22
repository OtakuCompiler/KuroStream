package com.kurostream.core.player

import android.app.ActivityManager
import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory manager optimized for 1GB RAM Fire TV devices.
 * Monitors heap usage and triggers cleanup before OOM events.
 */
@Singleton
class MemoryManager @Inject constructor(
    private val context: Context
) {
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val totalRamMb: Long
        get() {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            return info.totalMem / (1024 * 1024)
        }

    val availableRamMb: Long
        get() {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            return info.availMem / (1024 * 1024)
        }

    val isLowMemory: Boolean
        get() {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            return info.lowMemory
        }

    val memoryPressureLevel: MemoryPressure
        get() {
            val available = availableRamMb
            val total = totalRamMb
            val ratio = available.toFloat() / total
            return when {
                ratio < 0.10f -> MemoryPressure.CRITICAL
                ratio < 0.20f -> MemoryPressure.HIGH
                ratio < 0.35f -> MemoryPressure.MODERATE
                else -> MemoryPressure.LOW
            }
        }

    /**
     * Suggests optimal max buffer size in bytes based on available RAM.
     * On 1GB devices, we stay conservative to leave headroom for the OS.
     */
    fun recommendedPlayerBufferBytes(): Int {
        return when (memoryPressureLevel) {
            MemoryPressure.CRITICAL -> 8 * 1024 * 1024   // 8 MB
            MemoryPressure.HIGH     -> 12 * 1024 * 1024  // 12 MB
            MemoryPressure.MODERATE -> 16 * 1024 * 1024  // 16 MB
            MemoryPressure.LOW      -> 32 * 1024 * 1024  // 32 MB
        }
    }

    fun recommendedMaxBufferMs(): Int {
        return when (memoryPressureLevel) {
            MemoryPressure.CRITICAL -> 8_000
            MemoryPressure.HIGH     -> 12_000
            MemoryPressure.MODERATE -> 16_000
            MemoryPressure.LOW      -> 30_000
        }
    }

    fun logMemoryStatus() {
        Timber.d(
            "Memory: available=${availableRamMb}MB / total=${totalRamMb}MB " +
            "| pressure=${memoryPressureLevel} | lowMemory=$isLowMemory"
        )
    }

    fun trimMemory() {
        System.gc()
        Timber.d("Memory trim requested — available after: ${availableRamMb}MB")
    }

    enum class MemoryPressure { LOW, MODERATE, HIGH, CRITICAL }
}
