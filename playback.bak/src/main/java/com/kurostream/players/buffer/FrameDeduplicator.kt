package com.kurostream.players.buffer

import android.graphics.ImageFormat
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Detects and skips duplicate frames using perceptual hash (dHash).
 * Simple, fast, zero quality loss - only skips visually identical frames.
 * Target: 5-15 MB savings by avoiding duplicate frame processing.
 */
class FrameDeduplicator(
    private val similarityThreshold: Int = 3,
    private val maxHistorySize: Int = 30,
    private val ttlMs: Long = 5000
) {
    private val recentHashes = ConcurrentHashMap<Long, Long>() // hash -> timestamp
    private val accessTimes = ConcurrentHashMap<Long, Long>() // hash -> last access
    private val accessCounter = AtomicLong(0)
    
    /**
     * Checks if frame is duplicate of recent frame.
     * Returns true if frame should be SKIPPED (duplicate detected).
     */
    fun isDuplicate(frameData: ByteArray, width: Int, height: Int, format: Int): Boolean {
        val hash = computeDHash(frameData, width, height, format)
        val now = System.currentTimeMillis()
        
        // Check against recent hashes
        for ((recentHash, timestamp) in recentHashes) {
            val distance = hammingDistance(hash, recentHash)
            if (distance <= similarityThreshold) {
                // Duplicate found - update access time and return true
                accessTimes[hash] = now
                cleanupOldEntries(now)
                return true
            }
        }
        
        // Not duplicate - add to history
        recentHashes[hash] = now
        accessTimes[hash] = now
        cleanupOldEntries(now)
        return false
    }
    
    /**
     * Computes difference hash (dHash) for frame.
     * Fast, perceptual hash - detects visual similarity.
     */
    private fun computeDHash(data: ByteArray, width: Int, height: Int, format: Int): Long {
        // Downsample to 9x8 for dHash (64 bits)
        val targetWidth = 9
        val targetHeight = 8
        
        // Simple average pooling for downsample
        val pixels = IntArray(targetWidth * targetHeight)
        val scaleX = maxOf(1, width / targetWidth)
        val scaleY = maxOf(1, height / targetHeight)
        
        var idx = 0
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val srcX = minOf(x * scaleX, width - 1)
                val srcY = minOf(y * scaleY, height - 1)
                val pixelIdx = when (format) {
                    ImageFormat.YUV_420_888 -> (srcY * width + srcX) * 3 // YUV420 - use Y plane
                    ImageFormat.YUV_422_888 -> (srcY * width + srcX) * 2 // YUV422
                    ImageFormat.YUV_444_888 -> (srcY * width + srcX) * 3 // YUV444
                    ImageFormat.YV12 -> (srcY * width + srcX) // YV12 - Y plane
                    ImageFormat.NV21 -> (srcY * width + srcX) // NV21 - Y plane
                    ImageFormat.NV16 -> (srcY * width + srcX) // NV16 - Y plane
                    ImageFormat.RGBA_8888 -> (srcY * width + srcX) * 4 // RGBA
                    ImageFormat.RGB_565 -> (srcY * width + srcX) * 2 // RGB565
                    else -> (srcY * width + srcX) * 4 // Default RGBA
                }
                pixels[idx] = if (pixelIdx < data.size) data[pixelIdx].toInt() and 0xFF else 0
                idx++
            }
        }
        
        // Compute dHash: compare adjacent pixels
        var hash = 0L
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth - 1) {
                val left = pixels[y * targetWidth + x]
                val right = pixels[y * targetWidth + x + 1]
                hash = (hash shl 1) or (if (left > right) 1L else 0L)
            }
        }
        return hash
    }
    
    private fun hammingDistance(a: Long, b: Long): Int {
        return (a xor b).toInt().countOneBits()
    }
    
    private fun cleanupOldEntries(now: Long) {
        val cutoff = now - ttlMs
        recentHashes.entries.removeIf { (_, timestamp) -> timestamp < cutoff }
        accessTimes.entries.removeIf { (_, timestamp) -> timestamp < cutoff }
        
        if (recentHashes.size > maxHistorySize) {
            // Remove oldest by access time
            val oldest = accessTimes.entries.minByOrNull { it.value }?.key
            oldest?.let { 
                recentHashes.remove(it)
                accessTimes.remove(it)
            }
        }
    }
    
    fun reset() {
        recentHashes.clear()
        accessTimes.clear()
    }
    
    fun getHistorySize(): Int = recentHashes.size
    
    companion object {
        fun createDefault(): FrameDeduplicator = FrameDeduplicator()
    }
}