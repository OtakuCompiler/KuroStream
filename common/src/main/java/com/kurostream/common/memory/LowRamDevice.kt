package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object LowRamDevice {

    private var isLowRam: Boolean = false
    private var totalRamMb: Long = 0
    private var initialized = false

    private const val LOW_RAM_THRESHOLD_MB = 1536L

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            totalRamMb = memInfo.totalMem / (1024 * 1024)
        } else {
            totalRamMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        }

        isLowRam = totalRamMb <= LOW_RAM_THRESHOLD_MB
    }

    fun isLowRamDevice(): Boolean = isLowRam

    fun totalRamMb(): Long = totalRamMb

    val coilMemoryCacheSize: Int
        get() = if (isLowRam) 4 * 1024 * 1024 else 10 * 1024 * 1024

    val coilDiskCacheSize: Long
        get() = if (isLowRam) 50L * 1024 * 1024 else 100L * 1024 * 1024

    val bufferPoolMaxPerClass: Int
        get() = if (isLowRam) 8 else 16

    val heroBannerOffscreenPages: Int
        get() = if (isLowRam) 0 else 1

    val contentRowOffscreenPages: Int
        get() = if (isLowRam) 0 else 2

    val memoryPollIntervalMs: Long
        get() = if (isLowRam) 10000L else 5000L

    val bufferPoolPreallocate: Boolean
        get() = !isLowRam

    val memoryCautionThresholdMb: Int
        get() = if (isLowRam) 120 else 200

    val memoryWarningThresholdMb: Int
        get() = if (isLowRam) 80 else 150

    val memoryCriticalThresholdMb: Int
        get() = if (isLowRam) 50 else 100
}
