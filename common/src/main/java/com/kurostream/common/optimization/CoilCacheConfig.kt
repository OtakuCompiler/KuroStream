package com.kurostream.common.optimization

import android.content.Context
import com.kurostream.common.memory.LowRamDevice

object CoilCacheConfig {
    fun memoryCacheSize(context: Context): Long {
        return if (LowRamDevice.isLowRamDevice()) 32L * 1024 * 1024 else 128L * 1024 * 1024
    }

    fun diskCacheSize(context: Context): Long {
        return if (LowRamDevice.isLowRamDevice()) 64L * 1024 * 1024 else 256L * 1024 * 1024
    }
}
