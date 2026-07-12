package com.kurostream.common.pool

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Universal object pool for memory-critical objects.
 * Reduces GC pressure and memory fragmentation.
 */
@Singleton
class ObjectPoolManager @Inject constructor() {

    private val byteBufferPool = ObjectPool<ByteBuffer>(maxSize = 50) { capacity ->
        ByteBuffer.allocateDirect(capacity).apply { order(java.nio.ByteOrder.nativeOrder()) }
    }

    private val bitmapPool = ObjectPool<android.graphics.Bitmap>(maxSize = 20) { config ->
        // Config contains width:height:config ordinal
        val (width, height, bitmapConfig) = config.split(":").let {
            Triple(it[0].toInt(), it[1].toInt(), android.graphics.Bitmap.Config.valueOf(it[2]))
        }
        android.graphics.Bitmap.createBitmap(width, height, bitmapConfig)
    }

    private val paintPool = ObjectPool<android.graphics.Paint>(maxSize = 10) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            isFiltering = true
            flags = android.graphics.Paint.FILTER_BITMAP_FLAG
        }
    }

    private val canvasPool = ObjectPool<android.graphics.Canvas>(maxSize = 10) { bitmap ->
        android.graphics.Canvas(bitmap as android.graphics.Bitmap)
    }

    suspend fun acquireByteBuffer(size: Int): ByteBuffer {
        return byteBufferPool.acquire(size)
    }

    suspend fun releaseByteBuffer(buffer: ByteBuffer) {
        buffer.clear()
        byteBufferPool.release(buffer)
    }

    suspend fun acquireBitmap(width: Int, height: Int, config: android.graphics.Bitmap.Config = android.graphics.Bitmap.Config.RGB_565): android.graphics.Bitmap {
        return bitmapPool.acquire("$width:$height:${config.name}")
    }

    suspend fun releaseBitmap(bitmap: android.graphics.Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        }
        bitmapPool.release(bitmap)
    }

    suspend fun acquirePaint(): android.graphics.Paint {
        return paintPool.acquire(0)
    }

    suspend fun releasePaint(paint: android.graphics.Paint) {
        paint.reset()
        paintPool.release(paint)
    }

    suspend fun acquireCanvas(bitmap: android.graphics.Bitmap): android.graphics.Canvas {
        return canvasPool.acquire(bitmap)
    }

    suspend fun releaseCanvas(canvas: android.graphics.Canvas) {
        canvasPool.release(canvas)
    }

    suspend fun clearAll() {
        byteBufferPool.clear()
        bitmapPool.clear()
        paintPool.clear()
        canvasPool.clear()
        Timber.d("ObjectPoolManager cleared all pools")
    }

    fun getStats(): PoolStats {
        return PoolStats(
            byteBufferActive = byteBufferPool.activeCount,
            bitmapActive = bitmapPool.activeCount,
            paintActive = paintPool.activeCount,
            canvasActive = canvasPool.activeCount,
        )
    }
}

data class PoolStats(
    val byteBufferActive: Int,
    val bitmapActive: Int,
    val paintActive: Int,
    val canvasActive: Int,
)

/**
 * Generic object pool with LRU eviction.
 */
class ObjectPool<T>(
    private val maxSize: Int,
    private val factory: suspend (Any) -> T
) {
    private val pool = ArrayBlockingQueue<Pair<Any, T>>(maxSize)
    private val active = mutableMapOf<Any, T>()
    private val mutex = Mutex()

    val activeCount: Int get() = active.size

    suspend fun acquire(key: Any): T {
        return mutex.withLock {
            val existing = pool.firstOrNull { it.first == key }
            if (existing != null) {
                pool.remove(existing)
                active[key] = existing.second
                existing.second
            } else {
                val newObj = factory(key)
                active[key] = newObj
                newObj
            }
        }
    }

    suspend fun release(item: T, key: Any? = null) {
        mutex.withLock {
            val mapKey = key ?: active.entries.firstOrNull { it.value === item }?.key
            if (mapKey != null) {
                active.remove(mapKey)
                if (pool.size < maxSize) {
                    pool.offer(mapKey to item)
                }
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            pool.clear()
            active.clear()
        }
    }
}
