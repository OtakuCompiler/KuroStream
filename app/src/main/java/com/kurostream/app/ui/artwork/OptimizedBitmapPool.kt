package com.kurostream.app.ui.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Optimized bitmap pool with inBitmap reuse for artwork/thumbnails.
 * Size-bucketed pools with LRU eviction using LinkedHashMap.
 * Target: 2-5 MB savings for artwork caching.
 */
class OptimizedBitmapPool(
    private val maxSizeBytes: Long = 20 * 1024 * 1024 // 20MB default
) {
    private val pools = ConcurrentHashMap<PoolKey, LruPool>()
    private val totalSize = AtomicLong(0)
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val puts = AtomicLong(0)
    private val evictions = AtomicLong(0)

    /**
     * Simple LRU pool backed by LinkedHashMap.
     */
    private inner class LruPool(private val maxSize: Long) {
        private val map = object : LinkedHashMap<PoolKey, Bitmap>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PoolKey, Bitmap>): Boolean {
                if (size > maxSize) {
                    evictions.incrementAndGet()
                    totalSize.addAndGet(-estimateSize(eldest.value))
                    return true
                }
                return false
            }
        }

        @Synchronized
        fun get(key: PoolKey): Bitmap? = map[key]

        @Synchronized
        fun put(key: PoolKey, bitmap: Bitmap) {
            map[key] = bitmap
        }

        @Synchronized
        fun remove(key: PoolKey): Bitmap? = map.remove(key)

        @Synchronized
        fun snapshot(): Map<PoolKey, Bitmap> = LinkedHashMap(map)

        @Synchronized
        fun isNotEmpty(): Boolean = map.isNotEmpty()

        @Synchronized
        fun evictAll() {
            map.clear()
        }
    }

    /**
     * Gets a reusable bitmap for the given dimensions and config.
     * Returns null if no suitable bitmap available.
     */
    fun getBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        val key = PoolKey(width, height, config)
        val pool = pools[key]
        if (pool != null) {
            val bitmap = pool.get(key)
            if (bitmap != null && !bitmap.isRecycled && bitmap.width == width && bitmap.height == height) {
                hits.incrementAndGet()
                totalSize.addAndGet(-estimateSize(bitmap))
                return bitmap
            }
        }
        misses.incrementAndGet()
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Fallback 1x1
    }

    /**
     * Returns a bitmap to the pool for future reuse.
     */
    fun putBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        val key = PoolKey(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val pool = getOrCreatePool(key)

        // Ensure bitmap is mutable for reuse
        if (!bitmap.isMutable) {
            return // Can't reuse immutable bitmap
        }

        pool.put(key, bitmap)
        totalSize.addAndGet(estimateSize(bitmap))
        puts.incrementAndGet()

        // Trim if over budget
        trimToSize(maxSizeBytes)
    }

    private fun estimateSize(bitmap: Bitmap): Long {
        return (bitmap.byteCount).toLong()
    }

    private fun getOrCreatePool(key: PoolKey): LruPool {
        return pools.getOrPut(key) {
            LruPool(maxSizeBytes / 4) // Each pool gets 1/4 of total budget
        }
    }

    /**
     * Trims pool to target size - optimized single pass
     */
    fun trimToSize(targetSize: Long) {
        if (totalSize.get() <= targetSize) return
        
        // Single pass eviction - remove oldest entries until under budget
        for ((poolKey, pool) in pools) {
            while (totalSize.get() > targetSize) {
                val snapshot = pool.snapshot()
                if (snapshot.isEmpty()) break
                // Get oldest entry (first in access-order LRU)
                val oldest = snapshot.entries.firstOrNull() ?: break
                pool.remove(oldest.key)
                totalSize.addAndGet(-estimateSize(oldest.value))
                evictions.incrementAndGet()
            }
            if (totalSize.get() <= targetSize) break
        }
        
        // Clean up empty pools
        pools.entries.removeIf { (_, pool) -> !pool.isNotEmpty() }
    }

    /**
     * Clears all pools.
     */
    fun clear() {
        for (pool in pools.values) {
            pool.evictAll()
        }
        pools.clear()
        totalSize.set(0)
    }

    /**
     * Returns a bitmap from the pool or decodes one from the given options.
     * This is a convenience method for inBitmap reuse.
     */
    fun getBitmapWithOptions(options: BitmapFactory.Options): Bitmap? {
        val reusable = getBitmap(
            width = options.outWidth,
            height = options.outHeight,
            config = options.inPreferredConfig ?: Bitmap.Config.ARGB_8888
        )
        if (reusable != null) {
            options.inBitmap = reusable
        }
        return reusable
    }

    /**
     * Pool statistics for debugging.
     */
    fun getStats(): String {
        return "hits=${hits.get()}, misses=${misses.get()}, puts=${puts.get()}, " +
                "evictions=${evictions.get()}, totalSize=${totalSize.get() / (1024 * 1024)}MB, " +
                "poolCount=${pools.size}"
    }

    private data class PoolKey(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config
    )
}