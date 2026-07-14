// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.common.pool

import android.os.Build
import android.util.Log
import java.lang.ref.Cleaner
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pool of DirectByteBuffers for zero-copy frame data handling.
 * 
 * DirectByteBuffers are allocated off-heap and don't count toward Java heap,
 * but they still consume native memory and need explicit cleanup.
 * 
 * This pool manages a fixed number of buffers with reference counting
 * to ensure they're returned to the pool after use.
 */
object BufferPool {
    
    private const val TAG = "BufferPool"
    
    private var lowRamMode = false
    private val defaultPoolSize = AtomicInteger(16)
    
    // Buffer sizes for different use cases
    private val bufferSizes = intArrayOf(
        256 * 1024,      // 256KB - small metadata
        1024 * 1024,     // 1MB - compressed frames
        4 * 1024 * 1024, // 4MB - 1080p frames
        8 * 1024 * 1024, // 8MB - 4K frames
        16 * 1024 * 1024 // 16MB - 4K HDR frames
    )
    
    // Pools per size class - init lazily to respect lowRamMode
    private val pools: Map<Int, BufferPoolImpl> by lazy {
        bufferSizes.map { size ->
            size to BufferPoolImpl(size, defaultPoolSize.get())
        }.toMap()
    }
    
    // Statistics
    private var totalAllocated = 0L
    private var totalReleased = 0L
    private var peakConcurrent = 0
    private val currentConcurrent = AtomicInteger(0)
    
    /**
     * Acquire a DirectByteBuffer of at least the requested size.
     * Returns a PooledBuffer that must be released via .release()
     */
    fun acquire(minSize: Int): PooledBuffer {
        val sizeClass = bufferSizes.firstOrNull { it >= minSize } 
            ?: throw IllegalArgumentException("Requested size $minSize exceeds max buffer size")
        
        val pool = pools[sizeClass]!!
        val buffer = pool.acquire()
        
        val cc = currentConcurrent.incrementAndGet()
        synchronized(this) {
            totalAllocated++
            if (cc > peakConcurrent) peakConcurrent = cc
        }
        
        return PooledBuffer(buffer, pool, sizeClass)
    }
    
    /**
     * Acquire a buffer for a specific size class (avoids search).
     */
    fun acquireExact(sizeClass: Int): PooledBuffer {
        val pool = pools[sizeClass]!!
        val buffer = pool.acquire()
        
        val cc = currentConcurrent.incrementAndGet()
        synchronized(this) {
            totalAllocated++
            if (cc > peakConcurrent) peakConcurrent = cc
        }
        
        return PooledBuffer(buffer, pool, sizeClass)
    }
    
    /**
     * Get total native memory allocated across all pools (bytes).
     */
    fun getAllocatedBytes(): Long {
        return pools.values.sumOf { it.allocatedBytes }
    }
    
    /**
     * Get pool statistics.
     */
    fun getStats(): BufferPoolStats {
        return BufferPoolStats(
            totalAllocated = totalAllocated,
            totalReleased = totalReleased,
            peakConcurrent = peakConcurrent,
            currentConcurrent = currentConcurrent.get(),
            allocatedBytes = getAllocatedBytes(),
            pools = pools.mapValues { (_, pool) ->
                pool.stats
            }
        )
    }
    
    /**
     * Clear all pools (for memory pressure).
     */
    fun clearAll() {
        pools.values.forEach { it.clear() }
        synchronized(this) {
            currentConcurrent.set(0)
        }
    }
    
    /**
     * Shrink pools to minimum (keep 2 buffers per size).
     */
    fun shrinkAll() {
        pools.values.forEach { it.shrink() }
    }
    
    /**
     * Pre-allocate buffers for a specific size class.
     */
    fun preallocate(sizeClass: Int, count: Int) {
        pools[sizeClass]?.preallocate(count)
    }
    
    fun setLowRamMode(enabled: Boolean) {
        lowRamMode = enabled
        val newMax = if (enabled) 4 else 16
        defaultPoolSize.set(newMax)
        if (pools.isNotEmpty()) {
            pools.values.forEach { it.setMaxBuffers(newMax) }
            if (enabled) {
                shrinkAll()
            }
        }
    }
    
    // ===== Internal Pool Implementation =====
    
    internal class BufferPoolImpl(
        private val bufferSize: Int,
        private var maxBuffers: Int = 16
    ) {
        private val available = java.util.concurrent.ConcurrentLinkedQueue<PooledByteBuffer>()
        private val cleaner = Cleaner.create()
        private val lock = Any()
        private var created = 0
        
        var allocatedBytes: Long = 0
        
        fun acquire(): PooledByteBuffer {
            val buffer = available.poll() ?: synchronized(lock) {
                if (created < maxBuffers) {
                    created++
                    createBuffer()
                } else {
                    createBuffer()
                }
            }
            allocatedBytes += bufferSize.toLong()
            return buffer
        }
        
        private fun createBuffer(): PooledByteBuffer {
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
            val pooled = PooledByteBuffer(byteBuffer, this)
            pooled.cleaner = cleaner.register(pooled) { releaseNative(byteBuffer) }
            return pooled
        }
        
        fun release(buffer: PooledByteBuffer) {
            buffer.buffer.clear() // Reset position/limit
            available.offer(buffer)
            allocatedBytes -= bufferSize.toLong()
        }
        
        fun clear() {
            synchronized(lock) {
                available.forEach { it.cleaner?.clean() }
                available.clear()
                created = 0
                allocatedBytes = 0
            }
        }
        
        fun shrink() {
            synchronized(lock) {
                while (available.size > 2) {
                    val buf = available.poll()
                    buf?.cleaner?.clean()
                }
                created = available.size
            }
        }
        
        fun setMaxBuffers(max: Int) {
            synchronized(lock) {
                maxBuffers = max
                if (created > max) {
                    while (available.size > 0 && created > max) {
                        val buf = available.poll()
                        buf?.cleaner?.clean()
                        created--
                    }
                }
            }
        }

        fun preallocate(count: Int) {
            synchronized(lock) {
                repeat(count) {
                    if (created < maxBuffers) {
                        created++
                        val buf = createBuffer()
                        available.offer(buf)
                    }
                }
            }
        }
        
        val stats: PoolStats
            get() = PoolStats(
                bufferSize = bufferSize,
                created = created,
                available = available.size,
                allocatedBytes = allocatedBytes
            )
    }
    
    /**
     * Wrapper for DirectByteBuffer with pool reference and reference counting.
     */
    internal class PooledByteBuffer(
        val buffer: ByteBuffer,
        private val pool: BufferPoolImpl
    ) {
        var cleaner: Cleaner.Cleanable? = null
        private var refCount = 1
        
        fun retain(): Int {
            return synchronized(this) { refCount++ }
        }
        
        fun release(): Int {
            return synchronized(this) {
                refCount--
                if (refCount <= 0) {
                    pool.release(this)
                    0
                } else {
                    refCount
                }
            }
        }
    }
    
    /**
     * Public handle for acquired buffer with automatic release.
     */
    class PooledBuffer internal constructor(
        internal val pooled: PooledByteBuffer,
        internal val pool: BufferPoolImpl,
        val sizeClass: Int
    ) {
        private var released = false
        
        val buffer: ByteBuffer
            get() = pooled.buffer
        
        /**
         * Retain this buffer (increment ref count).
         * Must call release() for each retain().
         */
        fun retain(): PooledBuffer {
            pooled.retain()
            return this
        }
        
        /**
         * Release this buffer back to the pool.
         */
        fun release() {
            if (!released) {
                released = true
                pooled.release()
                currentConcurrent.decrementAndGet()
                synchronized(BufferPool) {
                    totalReleased++
                }
            }
        }
        
        /**
         * Get remaining capacity.
         */
        val remaining: Int
            get() = buffer.remaining()
        
        /**
         * Get buffer capacity.
         */
        val capacity: Int
            get() = buffer.capacity()
        
        /**
         * Use buffer with automatic position management.
         */
        fun use(block: (ByteBuffer) -> Unit) {
            val oldPos = buffer.position()
            val oldLimit = buffer.limit()
            try {
                block(buffer)
            } finally {
                buffer.position(oldPos)
                buffer.limit(oldLimit)
            }
        }
        
        /**
         * Write data to buffer.
         */
        fun write(src: ByteArray, offset: Int = 0, length: Int = src.size - offset): Int {
            val toWrite = length.coerceAtMost(buffer.remaining())
            buffer.put(src, offset, toWrite)
            return toWrite
        }
        
        /**
         * Read data from buffer.
         */
        fun read(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset): Int {
            val toRead = length.coerceAtMost(buffer.remaining())
            buffer.get(dst, offset, toRead)
            return toRead
        }
        
        override fun toString(): String {
            return "PooledBuffer(sizeClass=${sizeClass/1024}KB, pos=${buffer.position()}, rem=${buffer.remaining()})"
        }
    }
    
    /**
     * Release native memory for DirectByteBuffer.
     */
    private fun releaseNative(buffer: ByteBuffer) {
        if (buffer.isDirect) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val invokeCleaner = ByteBuffer::class.java.getMethod("invokeCleaner")
                    invokeCleaner.invoke(null, buffer)
                } else {
                    val cleanerMethod = ByteBuffer::class.java.getDeclaredMethod("cleaner")
                    cleanerMethod.isAccessible = true
                    val cleaner = cleanerMethod.invoke(buffer)
                    if (cleaner != null) {
                        val cleanMethod = cleaner.javaClass.getDeclaredMethod("clean")
                        cleanMethod.isAccessible = true
                        cleanMethod.invoke(cleaner)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean DirectByteBuffer", e)
            }
        }
    }
    
    data class PoolStats(
        val bufferSize: Int,
        val created: Int,
        val available: Int,
        val allocatedBytes: Long
    )
    
    data class BufferPoolStats(
        val totalAllocated: Long,
        val totalReleased: Long,
        val peakConcurrent: Int,
        val currentConcurrent: Int,
        val allocatedBytes: Long,
        val pools: Map<Int, PoolStats>
    ) {
        override fun toString(): String {
            return "BufferPoolStats(allocated=${allocatedBytes/1024/1024}MB, " +
                   "current=$currentConcurrent, peak=$peakConcurrent, " +
                   "pools=${pools.map { "${it.key/1024}KB:${it.value.available}" }.joinToString(", ")})"
        }
    }
}