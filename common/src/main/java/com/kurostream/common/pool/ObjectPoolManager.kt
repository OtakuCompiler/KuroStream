package com.kurostream.common.pool

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Universal object pool for memory-critical objects.
 * Reduces GC pressure and memory fragmentation.
 */
@Singleton
class ObjectPoolManager @Inject constructor() {

    private val byteBufferPool = ObjectPool(
        factory = { ByteBuffer.allocateDirect(1024 * 1024).apply { order(java.nio.ByteOrder.nativeOrder()) } },
        reset = { it.clear() },
        maxSize = 50
    )

    private val paintPool = ObjectPool(
        factory = { android.graphics.Paint().apply {
            isAntiAlias = true
            flags = android.graphics.Paint.FILTER_BITMAP_FLAG
        }},
        reset = { },
        maxSize = 10
    )

    private val canvasPool = ObjectPool<android.graphics.Canvas>(
        factory = { error("Canvas requires bitmap, use acquireCanvas instead") },
        reset = { },
        maxSize = 10
    )

    suspend fun acquireByteBuffer(): ByteBuffer {
        return byteBufferPool.acquire()
    }

    suspend fun releaseByteBuffer(buffer: ByteBuffer) {
        buffer.clear()
        byteBufferPool.release(buffer)
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
        val buffer = acquireByteBuffer()
        try {
            buffer.limit(size.coerceAtMost(buffer.capacity()))
            return block(buffer)
        } finally {
            releaseByteBuffer(buffer)
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
        val byteBufferStats = byteBufferPool.getStats()
        val paintStats = paintPool.getStats()
        return PoolStats(
            maxSize = byteBufferStats.maxSize + paintStats.maxSize,
            available = byteBufferStats.available + paintStats.available,
            created = byteBufferStats.created + paintStats.created
        )
    }

    companion object {
        private const val TAG = "ObjectPoolManager"
    }
}