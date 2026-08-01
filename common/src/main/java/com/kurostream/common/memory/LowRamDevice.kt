package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object LowRamDevice {
    private var _context: Context? = null

    fun init(context: Context) {
        _context = context.applicationContext
    }

    val isLowRamDevice: Boolean by lazy {
        val ctx = _context ?: return@lazy true
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@lazy true
        am.isLowRamDevice || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && am.memoryClass <= 192)
    }

    val totalMemoryMb: Int by lazy {
        val ctx = _context ?: return@lazy 1024
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@lazy 1024
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        (mi.totalMem / (1024 * 1024)).toInt()
    }

    val maxImageCacheBytes: Int
        get() = if (isLowRamDevice) 2 * 1024 * 1024 else 8 * 1024 * 1024

    val coilMemoryCacheSize: Int
        get() = if (isLowRamDevice) 2 * 1024 * 1024 else 8 * 1024 * 1024

    val coilDiskCacheSize: Long
        get() = if (isLowRamDevice) 25L * 1024 * 1024 else 50L * 1024 * 1024

    val maxSimultaneousImages: Int
        get() = if (isLowRamDevice) 3 else 6

    val maxDecoderFrameBuffers: Int
        get() = 1

    val maxGpuPoolTextures: Int
        get() = if (isLowRamDevice) 2 else 4

    val upscaleRingBufferSeconds: Int
        get() = 0

    val maxSubtitleCacheBytes: Int
        get() = if (isLowRamDevice) 2 * 1024 * 1024 else 5 * 1024 * 1024
}
