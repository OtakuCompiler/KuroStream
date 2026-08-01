package com.kurostream.cache

import android.content.Context
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

class VodCacheManager(context: Context) {

    val cache: Cache by lazy {
        val cacheDir = File(context.cacheDir, "vod_cache").apply { mkdirs() }
        SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(500L * 1024 * 1024))
    }

    fun getCacheSize(): Long {
        return cache.cacheSpace
    }

    fun clearCache() {
        cache.keys.forEach { cache.removeResource(it) }
    }

    fun release() {
        cache.release()
    }
}
