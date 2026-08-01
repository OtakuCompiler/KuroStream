package com.kurostream.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object VodDiskCache {
    private const val MAX_CACHE_BYTES = 500L * 1024 * 1024
    private const val CACHE_DIR_NAME = "kurostream_vod_cache"

    @Volatile
    private var cache: Cache? = null

    @Synchronized
    fun getCache(context: Context): Cache {
        return cache ?: createCache(context).also { cache = it }
    }

    private fun createCache(context: Context): Cache {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
        val dbProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, dbProvider)
    }

    fun release() {
        cache?.release()
        cache = null
    }
}
