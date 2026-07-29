package com.kurostream.common.pool

import timber.log.Timber
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Universal object pool for memory-critical objects.
 * Reduces GC pressure and memory fragmentation.
 * ByteBuffer pooling is delegated to BufferPool.
 */
@Singleton
class ObjectPoolManager @Inject constructor() {

    private val paintPool = ObjectPool(
        factory = { android.graphics.Paint().apply {
            isAntiAlias = true
            flags = android.graphics.Paint.FILTER_BITMAP_FLAG
        }},
        reset = { },
        maxSize = 10
    )

    suspend fun acquireByteBuffer(): ByteBuffer {
        return BufferPool.acquire(1024 * 1024).buffer
    }

    suspend fun releaseByteBuffer(buffer: ByteBuffer) {
        // BufferPool manages its own release cycle via PooledBuffer
    }

    fun acquirePaint(): android.graphics.Paint {
        return paintPool.acquire()
    }

    fun releasePaint(paint: android.graphics.Paint) {
        paintPool.release(paint)
    }

    fun acquireCanvas(bitmap: android.graphics.Bitmap): android.graphics.Canvas {
        return android.graphics.Canvas(bitmap)
    }

    fun releaseCanvas(canvas: android.graphics.Canvas) {
        // Canvas doesn't need pooling, just let it be GC'd
    }

    suspend fun <T> useByteBuffer(size: Int, block: (ByteBuffer) -> T): T {
        val pooled = BufferPool.acquire(size)
        val buffer = pooled.buffer
        try {
            buffer.limit(size.coerceAtMost(buffer.capacity()))
            return block(buffer)
        } finally {
            pooled.release()
        }
    }

    fun <T> usePaint(block: (android.graphics.Paint) -> T): T {
        val paint = acquirePaint()
        try {
            return block(paint)
        } finally {
            releasePaint(paint)
        }
    }

    fun getStats(): PoolStats {
        val paintStats = paintPool.getStats()
        return PoolStats(
            maxSize = paintStats.maxSize,
            available = paintStats.available,
            created = paintStats.created
        )
    }

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "ObjectPoolManager"
    }
}