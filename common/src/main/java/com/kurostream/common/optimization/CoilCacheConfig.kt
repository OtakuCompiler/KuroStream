package com.kurostream.common.optimization

import android.content.Context
import com.kurostream.common.memory.LowRamDevice

object CoilCacheConfig {
    fun memoryCacheSize(context: Context): Long {
        return LowRamDevice.coilMemoryCacheSize.toLong()
    }

    fun diskCacheSize(context: Context): Long {
        return LowRamDevice.coilDiskCacheSize
    }
}
