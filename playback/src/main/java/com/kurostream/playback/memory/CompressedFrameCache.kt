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

import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class CompressedFrameCache @Inject constructor() {
    companion object {
        private const val TAG = "CompressedFrameCache"
        private const val DEFAULT_MAX_SIZE = 16 * 1024 * 1024 // 16MB
        private const val MIN_COMPRESSION_RATIO = 1.2 // Only keep if >20% savings
        private const val COMPRESSION_LEVEL = 3 // Fast compression
        private const val MAX_FRAMES = 120 // ~4 seconds at 30fps
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val frameCache = ConcurrentHashMap<Long, CompressedFrame>()
    private val accessOrder = ConcurrentLinkedDeque<Long>()
    private val readWriteLock = ReentrantReadWriteLock()
    
    private val totalCompressedSize = AtomicLong(0)
    private val totalOriginalSize = AtomicLong(0)
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val compressionTimeUs = AtomicLong(0)
    private val decompressionTimeUs = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    
    private var maxSize = DEFAULT_MAX_SIZE
    private var maxFrames = MAX_FRAMES
    private var compressionEnabled = true

    data class CompressedFrame(
        val frameId: Long,
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val format: Int,
        val originalSize: Int,
        val compressedData: ByteArray,
        val compressedSize: Int,
        val compressionRatio: Double,
        val isKeyFrame: Boolean,
        val dependencies: List<Long> // Frame IDs this frame depends on
    ) {
        fun getMemoryFootprint(): Long = compressedSize.toLong() + 256 // Overhead estimate
    }

    data class CacheConfig(
        val maxSizeBytes: Long = DEFAULT_MAX_SIZE,
        val maxFrames: Int = MAX_FRAMES,
        val minCompressionRatio: Double = MIN_COMPRESSION_RATIO,
        val compressionLevel: Int = COMPRESSION_LEVEL,
        val enableCompression: Boolean = true,
        val keepKeyFrames: Boolean = true,
        val keyFrameRatio: Double = 0.3 // 30% key frames
    )

    fun initialize(config: CacheConfig) {
        maxSize = config.maxSizeBytes
        maxFrames = config.maxFrames
        compressionEnabled = config.enableCompression
    }

    suspend fun putFrame(
        frameId: Long,
        timestamp: Long,
        width: Int,
        height: Int,
        format: Int,
        yuvFrame: YuvFramePool.YuvFrame,
        isKeyFrame: Boolean,
        dependencies: List<Long> = emptyList()
    ): Boolean {
        return scope.launch(Dispatchers.IO) {
            if (!compressionEnabled) return@launch false

            val startTime = System.nanoTime()
            
            try {
                val originalSize = yuvFrame.getSize().toInt()
                
                // Compress Y, U, V planes separately for better compression
                val compressed = compressYuvPlanes(yuvFrame)
                val compressedSize = compressed.size
                
                val compressionRatio = originalSize.toDouble() / compressedSize
                if (compressionRatio < MIN_COMPRESSION_RATIO) {
                    return@launch false // Not worth compressing
                }
                
                val frame = CompressedFrame(
                    frameId = frameId,
                    timestamp = timestamp,
                    width = width,
                    height = height,
                    format = format,
                    originalSize = originalSize,
                    compressedData = compressed,
                    compressedSize = compressedSize,
                    compressionRatio = compressionRatio,
                    isKeyFrame = isKeyFrame,
                    dependencies = dependencies
                )
                
                compressionTimeUs.addAndGet((System.nanoTime() - startTime) / 1000)
                
                // Evict if needed
                ensureSpace(frame.compressedSize)
                
                readWriteLock.writeLock().lock()
                try {
                    frameCache[frameId] = frame
                    accessOrder.addLast(frameId)
                    totalCompressedSize.addAndGet(compressedSize.toLong())
                    totalOriginalSize.addAndGet(originalSize.toLong())
                } finally {
                    readWriteLock.writeLock().unlock()
                }
                
                trimToMaxFrames()
                
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to compress frame $frameId", e)
                false
            }
        }.await()
    }

    suspend fun getFrame(frameId: Long): ByteArray? {
        return scope.launch(Dispatchers.IO) {
            val startTime = System.nanoTime()
            
            readWriteLock.readLock().lock()
            val frame = frameCache[frameId]
            readWriteLock.readLock().unlock()
            
            if (frame == null) {
                missCount.incrementAndGet()
                return@launch null
            }
            
            hitCount.incrementAndGet()
            
            // Update access order
            readWriteLock.writeLock().lock()
            try {
                accessOrder.remove(frameId)
                accessOrder.addLast(frameId)
            } finally {
                readWriteLock.writeLock().unlock()
            }
            
            val decompressed = decompressYuvPlanes(frame)
            decompressionTimeUs.addAndGet((System.nanoTime() - startTime) / 1000)
            
            decompressed
        }.await()
    }

    fun getFrameSync(frameId: Long): ByteArray? {
        val startTime = System.nanoTime()
        
        readWriteLock.readLock().lock()
        val frame = frameCache[frameId]
        readWriteLock.readLock().unlock()
        
        if (frame == null) {
            missCount.incrementAndGet()
            return null
        }
        
        hitCount.incrementAndGet()
        
        // Update access order
        readWriteLock.writeLock().lock()
        try {
            accessOrder.remove(frameId)
            accessOrder.addLast(frameId)
        } finally {
            readWriteLock.writeLock().unlock()
        }
        
        val decompressed = decompressYuvPlanes(frame)
        decompressionTimeUs.addAndGet((System.nanoTime() - startTime) / 1000)
        
        decompressed
    }

    fun hasFrame(frameId: Long): Boolean {
        readWriteLock.readLock().lock()
        try {
            return frameCache.containsKey(frameId)
        } finally {
            readWriteLock.readLock().unlock()
        }
    }

    fun removeFrame(frameId: Long): Boolean {
        readWriteLock.writeLock().lock()
        try {
            val frame = frameCache.remove(frameId)
            if (frame != null) {
                accessOrder.remove(frameId)
                totalCompressedSize.addAndGet(-frame.compressedSize.toLong())
                totalOriginalSize.addAndGet(-frame.originalSize.toLong())
                return true
            }
            return false
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    fun getKeyFrames(): List<CompressedFrame> {
        readWriteLock.readLock().lock()
        try {
            return frameCache.values.filter { it.isKeyFrame }.toList()
        } finally {
            readWriteLock.readLock().unlock()
        }
    }

    fun getFramesInRange(startTimestamp: Long, endTimestamp: Long): List<CompressedFrame> {
        readWriteLock.readLock().lock()
        try {
            return frameCache.values
                .filter { it.timestamp >= startTimestamp && it.timestamp <= endTimestamp }
                .sortedBy { it.timestamp }
                .toList()
        } finally {
            readWriteLock.readLock().unlock()
        }
    }

    private fun compressYuvPlanes(yuvFrame: YuvFramePool.YuvFrame): ByteArray {
        val baos = ByteArrayOutputStream()
        
        // Write header: width (4), height (4), format (4), ySize (4), uSize (4)
        baos.write(intToBytes(yuvFrame.width))
        baos.write(intToBytes(yuvFrame.height))
        baos.write(intToBytes(yuvFrame.format))
        
        val ySize = yuvFrame.getYPlane().remaining()
        val uSize = yuvFrame.getUPlane().remaining()
        val vSize = yuvFrame.getVPlane().remaining()
        
        baos.write(intToBytes(ySize))
        baos.write(intToBytes(uSize))
        // vSize is implicit
        
        // Compress each plane with Zstd
        val yCompressed = compressPlane(yuvFrame.getYPlane())
        val uCompressed = compressPlane(yuvFrame.getUPlane())
        val vCompressed = compressPlane(yuvFrame.getVPlane())
        
        // Write compressed sizes
        baos.write(intToBytes(yCompressed.size))
        baos.write(intToBytes(uCompressed.size))
        baos.write(intToBytes(vCompressed.size))
        
        // Write compressed data
        baos.write(yCompressed)
        baos.write(uCompressed)
        baos.write(vCompressed)
        
        return baos.toByteArray()
    }

    private fun decompressYuvPlanes(frame: CompressedFrame): ByteArray {
        val input = ByteArrayInputStream(frame.compressedData)
        
        // Read header
        val width = readInt(input)
        val height = readInt(input)
        val format = readInt(input)
        val ySize = readInt(input)
        val uSize = readInt(input)
        val yCompressedSize = readInt(input)
        val uCompressedSize = readInt(input)
        val vCompressedSize = readInt(input)
        
        // Read compressed data
        val yCompressed = readBytes(input, yCompressedSize)
        val uCompressed = readBytes(input, uCompressedSize)
        val vCompressed = readBytes(input, vCompressedSize)
        
        // Decompress planes
        val yData = decompressPlane(yCompressed, ySize)
        val uData = decompressPlane(uCompressed, uSize)
        val vData = decompressPlane(vCompressed, ySize / 4) // V same size as U
        
        // Combine into single buffer
        val totalSize = yData.size + uData.size + vData.size
        val result = ByteArray(totalSize)
        var pos = 0
        System.arraycopy(yData, 0, result, pos, yData.size)
        pos += yData.size
        System.arraycopy(uData, 0, result, pos, uData.size)
        pos += uData.size
        System.arraycopy(vData, 0, result, pos, vData.size)
        
        return result
    }

    private fun compressPlane(buffer: ByteBuffer): ByteArray {
        val input = ByteArrayInputStream(buffer.array(), buffer.position(), buffer.remaining())
        val output = ByteArrayOutputStream()
        
        // Use Zstd via JNI or fallback to Deflater
        try {
            // Try Zstd first (would need zstd-jni)
            // For now, use Deflater with fast compression
            val deflater = java.util.zip.Deflater(COMPRESSION_LEVEL)
            deflater.setInput(input.readAllBytes())
            deflater.finish()
            
            val buffer = ByteArray(4096)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, 0, count)
            }
            deflater.end()
        } catch (e: Exception) {
            Log.w(TAG, "Compression failed, storing uncompressed", e)
            return buffer.array().copyOfRange(buffer.position(), buffer.limit())
        }
        
        return output.toByteArray()
    }

    private fun decompressPlane(compressed: ByteArray, expectedSize: Int): ByteArray {
        val input = ByteArrayInputStream(compressed)
        val output = ByteArrayOutputStream(expectedSize)
        
        try {
            val inflater = java.util.zip.Inflater()
            inflater.setInput(compressed)
            
            val buffer = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                output.write(buffer, 0, count)
            }
            inflater.end()
        } catch (e: Exception) {
            Log.w(TAG, "Decompression failed", e)
            return compressed // Return as-is if uncompressed
        }
        
        return output.toByteArray()
    }

    private fun compressPlane(buffer: ByteBuffer): ByteArray {
        // Use fast LZ4-style compression for YUV planes
        // Simplified: just use Deflater for now
        return compressPlane(buffer)
    }

    private fun ensureSpace(neededBytes: Int) {
        while (totalCompressedSize.get() + neededBytes > maxSize && !accessOrder.isEmpty()) {
            evictOldest()
        }
    }

    private fun trimToMaxFrames() {
        while (frameCache.size > maxFrames && !accessOrder.isEmpty()) {
            // Don't evict key frames if configured to keep them
            var evicted = false
            val iterator = accessOrder.iterator()
            while (iterator.hasNext() && !evicted) {
                val frameId = iterator.next()
                val frame = frameCache[frameId]
                if (frame != null && (!frame.isKeyFrame || frameCache.values.count { it.isKeyFrame } > (maxFrames * 0.3))) {
                    removeFrame(frameId)
                    evicted = true
                }
            }
            if (!evicted) break // All remaining are key frames we want to keep
        }
    }

    private fun evictOldest() {
        var frameId = accessOrder.pollFirst()
        while (frameId != null) {
            val frame = frameCache[frameId]
            if (frame != null && (!frame.isKeyFrame || frameCache.values.count { it.isKeyFrame } > (maxFrames * 0.3))) {
                removeFrame(frameId)
                evictionCount.incrementAndGet()
                return
            }
            frameId = accessOrder.pollFirst()
        }
    }

    fun getStats(): Map<String, Any> {
        readWriteLock.readLock().lock()
        try {
            val totalRequests = hitCount.get() + missCount.get()
            val hitRate = if (totalRequests > 0) hitCount.get().toDouble() / totalRequests else 0.0
            val avgCompressionRatio = if (frameCache.isNotEmpty()) {
                frameCache.values.map { it.compressionRatio }.average()
            } else 0.0
            
            return mapOf(
                "framesCached" to frameCache.size,
                "maxFrames" to maxFrames,
                "compressedSizeMB" to totalCompressedSize.get() / 1024 / 1024,
                "originalSizeMB" to totalOriginalSize.get() / 1024 / 1024,
                "maxSizeMB" to maxSize / 1024 / 1024,
                "spaceUtilization" to String.format("%.1f%%", (totalCompressedSize.get().toDouble() / maxSize) * 100),
                "avgCompressionRatio" to String.format("%.2fx", avgCompressionRatio),
                "spaceSavings" to String.format("%.1f%%", (1.0 - totalCompressedSize.get().toDouble() / totalOriginalSize.get()) * 100),
                "hitRate" to String.format("%.2f%%", hitRate * 100),
                "totalRequests" to totalRequests,
                "evictions" to evictionCount.get(),
                "avgCompressionTimeUs" to (if (hitCount.get() > 0) compressionTimeUs.get() / hitCount.get() else 0L),
                "avgDecompressionTimeUs" to (if (hitCount.get() > 0) decompressionTimeUs.get() / hitCount.get() else 0L),
                "keyFramesCached" to frameCache.values.count { it.isKeyFrame },
                "compressionEnabled" to compressionEnabled
            )
        } finally {
            readWriteLock.readLock().unlock()
        }
    }

    fun clear() {
        readWriteLock.writeLock().lock()
        try {
            frameCache.clear()
            accessOrder.clear()
            totalCompressedSize.set(0)
            totalOriginalSize.set(0)
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        clear()
    }

    // Helper functions
    private fun intToBytes(value: Int): ByteArray = ByteArray(4) {
        (value shr (it * 8)).toByte()
    }

    private fun readInt(input: ByteArrayInputStream): Int {
        var value = 0
        for (i in 0..3) {
            val b = input.read()
            if (b == -1) break
            value = value or (b shl (i * 8))
        }
        return value
    }

    private fun readBytes(input: ByteArrayInputStream, count: Int): ByteArray {
        val bytes = ByteArray(count)
        var read = 0
        while (read < count) {
            val n = input.read(bytes, read, count - read)
            if (n == -1) break
            read += n
        }
        return bytes
    }
}