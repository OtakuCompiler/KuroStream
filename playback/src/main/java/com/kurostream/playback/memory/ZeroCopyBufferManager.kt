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

package com.kurostream.playback.memory

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroCopyBufferManager @Inject constructor() {
    companion object {
        private const val TAG = "ZeroCopyBufferManager"
        private const val MAX_POOL_SIZE = 10
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB chunks
        private const val MAX_MAPPED_SIZE = 64 * 1024 * 1024 // 64MB max mapped
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bufferPools = ConcurrentHashMap<Int, BufferPool>()
    private val mappedBuffers = ConcurrentHashMap<Int, MappedBuffer>()
    private val fileCache = ConcurrentHashMap<String, FileCacheEntry>()
    private val totalAllocated = AtomicLong(0)
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)

    data class BufferPool(
        val chunkSize: Int,
        private val buffers: java.util.concurrent.ArrayDeque<ByteBuffer> = java.util.concurrent.ArrayDeque(),
        private val capacity: Int = MAX_POOL_SIZE
    ) {
        fun acquire(): ByteBuffer {
            val buffer = buffers.pollLast()
            if (buffer != null) {
                buffer.clear()
                hitCount.incrementAndGet()
                return buffer
            }
            missCount.incrementAndGet()
            return ByteBuffer.allocateDirect(chunkSize)
        }

        fun release(buffer: ByteBuffer) {
            if (buffers.size < capacity) {
                buffer.clear()
                buffers.addLast(buffer)
            }
        }

        fun getStats(): Map<String, Any> = mapOf(
            "poolSize" to buffers.size,
            "capacity" to capacity,
            "chunkSizeKB" to chunkSize / 1024
        )

        fun clear() {
            buffers.clear()
        }
    }

    data class MappedBuffer(
        val file: File,
        val size: Long,
        val buffer: MappedByteBuffer,
        val channel: FileChannel,
        private val refCount: AtomicLong = AtomicLong(1)
    ) : Closeable {
        fun acquire(): MappedByteBuffer {
            refCount.incrementAndGet()
            return buffer
        }

        fun release() {
            if (refCount.decrementAndGet() == 0) {
                close()
            }
        }

        override fun close() {
            try {
                buffer.force()
                channel.close()
                totalAllocated.addAndGet(-size)
            } catch (e: IOException) {
                Log.w(TAG, "Error closing mapped buffer", e)
            }
        }

        fun getStats(): Map<String, Any> = mapOf(
            "file" to file.name,
            "sizeMB" to size / 1024 / 1024,
            "refCount" to refCount.get()
        )
    }

    data class FileCacheEntry(
        val file: File,
        val size: Long,
        val lastAccess: Long,
        val accessCount: AtomicLong = AtomicLong(0)
    )

    fun initializeBufferPool(chunkSize: Int = CHUNK_SIZE, capacity: Int = MAX_POOL_SIZE): BufferPool {
        val pool = BufferPool(chunkSize, capacity = capacity)
        bufferPools[chunkSize] = pool
        return pool
    }

    fun acquireDirectBuffer(size: Int): ByteBuffer {
        val poolSize = ((size + CHUNK_SIZE - 1) / CHUNK_SIZE) * CHUNK_SIZE
        val pool = bufferPools.getOrPut(poolSize) { BufferPool(poolSize) }
        return pool.acquire()
    }

    fun releaseDirectBuffer(buffer: ByteBuffer) {
        val pool = bufferPools[buffer.capacity()]
        pool?.release(buffer)
    }

    fun acquireMappedBuffer(file: File, offset: Long = 0, size: Long = file.length()): MappedByteBuffer? {
        val key = "${file.absolutePath}:$offset:$size"
        return try {
            val existing = mappedBuffers[key]
            if (existing != null) {
                hitCount.incrementAndGet()
                return existing.acquire()
            }

            missCount.incrementAndGet()

            if (size > MAX_MAPPED_SIZE) {
                Log.w(TAG, "Requested mapped buffer size ${size / 1024 / 1024}MB exceeds max ${MAX_MAPPED_SIZE / 1024 / 1024}MB")
                return null
            }

            val raf = RandomAccessFile(file, "r")
            val channel = raf.channel
            val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, size)

            val mapped = MappedBuffer(file, size, mappedBuffer, channel)
            mappedBuffers[key] = mapped
            totalAllocated.addAndGet(size)

            updateFileCache(file)
            mapped.acquire()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create mapped buffer for ${file.name}", e)
            null
        }
    }

    fun releaseMappedBuffer(buffer: MappedByteBuffer) {
        mappedBuffers.values.forEach { it.release() }
    }

    fun createTempMappedBuffer(size: Int): MappedByteBuffer? {
        return try {
            val tempFile = File.createTempFile("kurostream_buffer_", ".tmp")
            tempFile.deleteOnExit()
            
            val raf = RandomAccessFile(tempFile, "rw")
            raf.setLength(size.toLong())
            val channel = raf.channel
            val mappedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size.toLong())

            val mapped = MappedBuffer(tempFile, size.toLong(), mappedBuffer, channel)
            val key = "temp:${tempFile.absolutePath}"
            mappedBuffers[key] = mapped
            totalAllocated.addAndGet(size.toLong())

            mapped.acquire()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create temp mapped buffer", e)
            null
        }
    }

    private fun updateFileCache(file: File) {
        val key = file.absolutePath
        fileCache.compute(key) { _, existing ->
            if (existing != null) {
                FileCacheEntry(
                    file = existing.file,
                    size = existing.size,
                    lastAccess = System.currentTimeMillis(),
                    accessCount = existing.accessCount.apply { incrementAndGet() }
                )
            } else {
                FileCacheEntry(file, file.length(), System.currentTimeMillis())
            }
        }
    }

    fun getCacheStats(): Map<String, Any> {
        val totalDirect = bufferPools.values.sumOf { it.buffers.size * it.chunkSize }
        val totalMapped = mappedBuffers.values.sumOf { it.size }
        val cacheFiles = fileCache.size
        val totalRequests = hitCount.get() + missCount.get()
        val hitRate = if (totalRequests > 0) hitCount.get().toDouble() / totalRequests else 0.0

        return mapOf(
            "directBufferPoolMB" to totalDirect / 1024 / 1024,
            "mappedBufferMB" to totalMapped / 1024 / 1024,
            "totalAllocatedMB" to totalAllocated.get() / 1024 / 1024,
            "activePools" to bufferPools.size,
            "activeMappedBuffers" to mappedBuffers.size,
            "cachedFiles" to cacheFiles,
            "hitRate" to String.format("%.2f%%", hitRate * 100),
            "totalRequests" to totalRequests
        )
    }

    fun trimMemory(level: Int) {
        val reduction = when (level) {
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> 0.25f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.5f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> 0.75f
            android.app.ActivityManager.TRIM_MEMORY_UI_HIDDEN -> 0.3f
            android.app.ActivityManager.TRIM_MEMORY_BACKGROUND -> 0.6f
            android.app.ActivityManager.TRIM_MEMORY_COMPLETE -> 0.9f
            else -> 0f
        }

        if (reduction > 0) {
            bufferPools.values.forEach { pool ->
                val toRemove = (pool.buffers.size * reduction).toInt()
                repeat(toRemove) { pool.buffers.pollLast() }
            }

            val now = System.currentTimeMillis()
            fileCache.entries.forEach { entry ->
                val cacheEntry = entry.value
                if (now - cacheEntry.lastAccess > 30000 && cacheEntry.accessCount.get() < 3) {
                    try {
                        cacheEntry.file.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete cache file", e)
                    }
                    fileCache.remove(entry.key)
                }
            }
        }
    }

    fun forceCleanup() {
        bufferPools.values.forEach { it.clear() }
        mappedBuffers.values.forEach { it.close() }
        mappedBuffers.clear()
        fileCache.values.forEach { it.file.delete() }
        fileCache.clear()
        totalAllocated.set(0)
        hitCount.set(0)
        missCount.set(0)
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        forceCleanup()
    }
}