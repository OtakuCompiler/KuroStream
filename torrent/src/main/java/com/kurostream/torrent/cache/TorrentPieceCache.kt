package com.kurostream.torrent.cache

import android.util.Log
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPieceCache @Inject constructor() {

    private val TAG = "TorrentPieceCache"

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val totalSizeBytes = AtomicLong(0)

    private val bufferPool = ArrayDeque<ByteBuffer>()
    private val poolLock = Any()
    private var poolSize = 0
    private val maxPoolSize = 64

    data class CacheEntry(
        val data: ByteBuffer,
        val infoHash: String,
        val pieceIndex: Int,
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 0,
    )

    private var maxSizeBytes: Long = 64 * 1024 * 1024

    fun configure(maxSizeMb: Int = 64) {
        maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        Log.i(TAG, "Piece cache configured: ${maxSizeMb}MB")
    }

    private fun obtainBuffer(capacity: Int): ByteBuffer {
        synchronized(poolLock) {
            while (bufferPool.isNotEmpty()) {
                val buf = bufferPool.pollFirst() ?: break
                poolSize--
                if (buf.capacity() >= capacity) {
                    buf.clear()
                    buf.limit(capacity)
                    return buf
                }
            }
        }
        return ByteBuffer.allocateDirect(capacity)
    }

    private fun recycleBuffer(buffer: ByteBuffer) {
        synchronized(poolLock) {
            if (poolSize < maxPoolSize) {
                buffer.clear()
                bufferPool.addLast(buffer)
                poolSize++
            }
        }
    }

    fun put(infoHash: String, pieceIndex: Int, data: ByteArray) {
        val key = "$infoHash:$pieceIndex"

        val buffer = obtainBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        val entry = CacheEntry(
            data = buffer,
            infoHash = infoHash,
            pieceIndex = pieceIndex,
        )

        val old = cache.put(key, entry)
        if (old != null) {
            recycleBuffer(old.data)
        } else {
            totalSizeBytes.addAndGet(data.size.toLong())
        }

        evictIfNeeded()
    }

    fun get(infoHash: String, pieceIndex: Int): ByteArray? {
        val key = "$infoHash:$pieceIndex"
        val entry = cache[key] ?: return null

        entry.accessCount++
        entry.data.position(0)

        val data = ByteArray(entry.data.remaining())
        entry.data.get(data)
        entry.data.position(0)

        return data
    }

    fun contains(infoHash: String, pieceIndex: Int): Boolean {
        return cache.containsKey("$infoHash:$pieceIndex")
    }

    fun remove(infoHash: String, pieceIndex: Int) {
        val key = "$infoHash:$pieceIndex"
        val entry = cache.remove(key)
        if (entry != null) {
            totalSizeBytes.addAndGet(-entry.data.capacity().toLong())
            recycleBuffer(entry.data)
        }
    }

    fun clear() {
        cache.clear()
        totalSizeBytes.set(0)
    }

    fun clearForTorrent(infoHash: String) {
        val keysToRemove = cache.keys.filter { it.startsWith("$infoHash:") }
        keysToRemove.forEach { key ->
            val entry = cache.remove(key)
            if (entry != null) {
                totalSizeBytes.addAndGet(-entry.data.capacity().toLong())
                recycleBuffer(entry.data)
            }
        }
    }

    fun getSizeBytes(): Long = totalSizeBytes.get()

    fun getEntryCount(): Int = cache.size

    private fun evictIfNeeded() {
        while (totalSizeBytes.get() > maxSizeBytes && cache.isNotEmpty) {
            val oldestEntry = cache.values.minByOrNull { it.timestamp } ?: break
            val key = "${oldestEntry.infoHash}:${oldestEntry.pieceIndex}"
            cache.remove(key)
            totalSizeBytes.addAndGet(-oldestEntry.data.capacity().toLong())
            recycleBuffer(oldestEntry.data)
        }
    }
}
