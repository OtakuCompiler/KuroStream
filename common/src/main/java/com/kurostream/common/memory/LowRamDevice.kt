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
        get() = 2 * 1024 * 1024 // Fixed 2MB — small enough for TV UI

    val coilDiskCacheSize: Long
        get() = 25L * 1024 * 1024 // Fixed 25MB disk cache

    val bufferPoolMaxPerClass: Int
        get() = if (isLowRam) 4 else 8

    val heroBannerOffscreenPages: Int
        get() = if (isLowRam) 0 else 0

    val contentRowOffscreenPages: Int
        get() = if (isLowRam) 0 else 1

    val memoryPollIntervalMs: Long
        get() = if (isLowRam) 15_000L else 10_000L

    val bufferPoolPreallocate: Boolean
        get() = false

    val memoryCautionThresholdMb: Int
        get() = if (isLowRam) 80 else 120

    val memoryWarningThresholdMb: Int
        get() = if (isLowRam) 50 else 80

    val memoryCriticalThresholdMb: Int
        get() = if (isLowRam) 30 else 50

    val maxDecoderFrameBuffers: Int
        get() = if (isLowRam) 2 else 3

    val maxGpuPoolTextures: Int
        get() = if (isLowRam) 4 else 8

    val upscaleRingBufferSeconds: Int
        get() = if (isLowRam) 0 else 1 // Disable ring buffer on low RAM

    val p2pMaxPeers: Int
        get() = if (isLowRam) 2 else 4
}
