package com.kurostream.torrent.cache

import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPieceCache @Inject constructor() {

    private val TAG = "TorrentPieceCache"

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val totalSizeBytes = AtomicLong(0)

    data class CacheEntry(
        val data: ByteBuffer,
        val infoHash: String,
        val pieceIndex: Int,
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 0,
    )

    private var maxSizeBytes: Long = 64 * 1024 * 1024 // 64MB default

    fun configure(maxSizeMb: Int = 64) {
        maxSizeBytes = maxSizeMb.toLong() * 1024 * 1024
        Log.i(TAG, "Piece cache configured: ${maxSizeMb}MB")
    }

    fun put(infoHash: String, pieceIndex: Int, data: ByteArray) {
        val key = "$infoHash:$pieceIndex"

        val buffer = ByteBuffer.allocateDirect(data.size)
        buffer.put(data)
        buffer.flip()

        val entry = CacheEntry(
            data = buffer,
            infoHash = infoHash,
            pieceIndex = pieceIndex,
        )

        cache[key] = entry
        totalSizeBytes.addAndGet(data.size.toLong())

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
        }
    }
}
