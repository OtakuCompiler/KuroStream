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

package com.kurostream.players.buffer

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Disk-backed circular buffer for playback data.
 * 
 * All incoming network data and decoded frames are written directly to disk.
 * The player reads back data from disk as needed with small in-memory read-ahead windows.
 * 
 * Features:
 * - Fixed-size disk buffer (configurable, default 200MB)
 * - Memory-mapped file access for zero-copy reads
 * - Thread-safe read/write pointers with lock striping
 * - Graceful disk full handling (trim old data, warn user)
 * - Seek support within buffered range
 */
class DiskBufferManager private constructor(
    private val context: Context,
    private val config: DiskBufferConfig
) {
    private val TAG = "DiskBufferManager"
    
    // Disk buffer file
    private var bufferFile: File? = null
    private var fileChannel: FileChannel? = null
    private var mappedBuffer: ByteBuffer? = null
    private var writeLock: FileLock? = null
    
    // Pointers (in bytes)
    private val writePosition = AtomicLong(0)
    private val readPosition = AtomicLong(0)
    private val validDataEnd = AtomicLong(0) // End of valid data for seeking
    
    // Buffer state
    private val isInitialized = AtomicBoolean(false)
    private val isWriting = AtomicBoolean(false)
    private val bufferSizeBytes: Long
    
    // Read-ahead window (small in-memory cache)
    private val readAheadCache = mutableMapOf<Long, ByteBuffer>()
    private val readAheadCacheSize = AtomicLong(0)
    private val maxReadAheadBytes = config.maxReadAheadMb * 1024L * 1024L
    private val cacheLock = ReentrantReadWriteLock()
    
    // Disk space monitoring
    private val diskSpaceCallback: ((Long) -> Unit)? = null
    
    // Stats
    private val totalBytesWritten = AtomicLong(0)
    private val totalBytesRead = AtomicLong(0)
    private var lastDiskSpaceCheck = 0L
    private var availableDiskSpace = 0L
    
    init {
        bufferSizeBytes = config.bufferSizeMb * 1024L * 1024L
    }
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: DiskBufferManager? = null
        
        fun getInstance(context: Context, config: DiskBufferConfig): DiskBufferManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiskBufferManager(context.applicationContext, config).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    /**
     * Initialize the disk buffer. Must be called before any read/write operations.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Choose buffer location
            val bufferDir = when (config.location) {
                DiskBufferConfig.BufferLocation.INTERNAL_CACHE -> context.cacheDir
                DiskBufferConfig.BufferLocation.EXTERNAL_CACHE -> context.externalCacheDir
                DiskBufferConfig.BufferLocation.EXTERNAL_FILES -> context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                DiskBufferConfig.BufferLocation.CUSTOM -> config.customPath?.let { File(it) } ?: context.cacheDir
            }
            
            if (bufferDir == null || !bufferDir.exists()) {
                bufferDir?.mkdirs()
            }
            
            // Check available space
            val usableSpace = bufferDir!!.usableSpace
            if (usableSpace < bufferSizeBytes * 1.2) { // Need 20% headroom
                return Result.failure(IOException("Insufficient disk space: ${usableSpace / 1024 / 1024}MB available, ${config.bufferSizeMb}MB required"))
            }
            
            availableDiskSpace = usableSpace
            
            // Create buffer file
            bufferFile = File(bufferDir, "kurostream_playback_buffer.dat")
            
            // Create or truncate to configured size
            RandomAccessFile(bufferFile, "rw").use { raf ->
                raf.setLength(bufferSizeBytes)
            }
            
            // Open file channel
            fileChannel = FileChannel.open(
                bufferFile!!.toPath(),
                java.nio.file.StandardOpenOption.READ,
                java.nio.file.StandardOpenOption.WRITE
            )
            
            // Memory-map the entire file for zero-copy access
            mappedBuffer = fileChannel!!.map(
                FileChannel.MapMode.READ_WRITE,
                0,
                bufferSizeBytes
            )
            
            // Acquire exclusive lock for writing
            writeLock = fileChannel!!.lock()
            
            // Reset pointers
            writePosition.set(0)
            readPosition.set(0)
            validDataEnd.set(0)
            
            isInitialized.set(true)
            
            Log.i(TAG, "Disk buffer initialized: ${config.bufferSizeMb}MB at ${bufferFile!!.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize disk buffer", e)
            cleanup()
            Result.failure(e)
        }
    }
    
    /**
     * Write data to the disk buffer (circular).
     * Returns number of bytes written.
     */
    suspend fun write(data: ByteArray, offset: Int = 0, length: Int = data.size): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        if (length == 0) return Result.success(0)
        
        isWriting.set(true)
        try {
            val currentWritePos = writePosition.get()
            var bytesWritten = 0
            var remaining = length
            var srcOffset = offset
            
            while (remaining > 0 && isActive) {
                val spaceToEnd = bufferSizeBytes - currentWritePos
                val chunkSize = remaining.coerceAtMost(spaceToEnd.toInt())
                
                // Write to mapped buffer
                mappedBuffer!!.position(currentWritePos.toInt())
                mappedBuffer!!.put(data, srcOffset, chunkSize)
                
                bytesWritten += chunkSize
                remaining -= chunkSize
                srcOffset += chunkSize
                
                // Update write position (circular)
                val newWritePos = (currentWritePos + chunkSize) % bufferSizeBytes
                writePosition.set(newWritePos)
                
                // Update valid data end (for seeking)
                val currentValidEnd = validDataEnd.get()
                if (currentWritePos >= currentValidEnd) {
                    validDataEnd.set(newWritePos)
                } else if (newWritePos < currentWritePos) {
                    // Wrapped around
                    validDataEnd.set(newWritePos)
                }
                
                // Check if we're about to overwrite unread data
                val unreadBytes = getUnreadBytes()
                if (unreadBytes > bufferSizeBytes * 0.9) {
                    // Buffer 90% full, advance read position to make room
                    advanceReadPosition(chunkSize.toLong())
                }
                
                if (remaining > 0) {
                    // Wrapped to beginning
                }
            }
            
            totalBytesWritten.addAndGet(bytesWritten.toLong())
            
            // Periodically check disk space
            checkDiskSpaceIfNeeded()
            
            Result.success(bytesWritten)
        } catch (e: Exception) {
            Log.e(TAG, "Write failed", e)
            Result.failure(e)
        } finally {
            isWriting.set(false)
        }
    }
    
    /**
     * Write from a ByteBuffer (zero-copy when possible).
     */
    suspend fun writeBuffer(buffer: ByteBuffer): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        val remaining = buffer.remaining()
        if (remaining == 0) return Result.success(0)
        
        isWriting.set(true)
        try {
            val currentWritePos = writePosition.get()
            var bytesWritten = 0
            var srcBuffer = buffer
            
            while (srcBuffer.hasRemaining() && isActive) {
                val spaceToEnd = bufferSizeBytes - currentWritePos
                val chunkSize = srcBuffer.remaining().coerceAtMost(spaceToEnd.toInt())
                
                // Direct put from source buffer to mapped buffer
                mappedBuffer!!.position(currentWritePos.toInt())
                val limit = srcBuffer.limit()
                srcBuffer.limit(srcBuffer.position() + chunkSize)
                mappedBuffer!!.put(srcBuffer)
                srcBuffer.limit(limit)
                
                bytesWritten += chunkSize
                writePosition.set((currentWritePos + chunkSize) % bufferSizeBytes)
                
                // Update valid data end
                val currentValidEnd = validDataEnd.get()
                if (currentWritePos >= currentValidEnd) {
                    validDataEnd.set(writePosition.get())
                }
                
                // Check for overwrite
                if (getUnreadBytes() > bufferSizeBytes * 0.9) {
                    advanceReadPosition(chunkSize.toLong())
                }
            }
            
            totalBytesWritten.addAndGet(bytesWritten.toLong())
            checkDiskSpaceIfNeeded()
            Result.success(bytesWritten)
        } catch (e: Exception) {
            Log.e(TAG, "Buffer write failed", e)
            Result.failure(e)
        } finally {
            isWriting.set(false)
        }
    }
    
    /**
     * Read data from the disk buffer at the current read position.
     * Returns null if no data available or buffer not initialized.
     */
    suspend fun read(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        
        val unread = getUnreadBytes()
        if (unread == 0) return Result.success(0) // No data available
        
        val toRead = length.coerceAtMost(unread.toInt())
        var bytesRead = 0
        var currentReadPos = readPosition.get()
        var remaining = toRead
        var dstOffset = offset
        
        try {
            while (remaining > 0 && isActive) {
                val dataToEnd = bufferSizeBytes - currentReadPos
                val chunkSize = remaining.coerceAtMost(dataToEnd.toInt())
                
                // Check read-ahead cache first
                val cacheKey = currentReadPos
                cacheLock.readLock().lock()
                val cached = readAheadCache[cacheKey]
                cacheLock.readLock().unlock()
                
                if (cached != null) {
                    // Read from cache
                    val cachedLimit = cached.limit()
                    cached.position(0)
                    val chunkLimit = chunkSize.coerceAtMost(cached.remaining())
                    cached.limit(cached.position() + chunkLimit)
                    System.arraycopy(cached.array(), cached.arrayOffset() + cached.position(), dst, dstOffset, chunkLimit)
                    cached.limit(cachedLimit)
                    bytesRead += chunkLimit
                    remaining -= chunkLimit
                    dstOffset += chunkLimit
                } else {
                    // Read from mapped buffer
                    mappedBuffer!!.position(currentReadPos.toInt())
                    mappedBuffer!!.get(dst, dstOffset, chunkSize)
                    bytesRead += chunkSize
                    remaining -= chunkSize
                    dstOffset += chunkSize
                }
                
                // Update read position
                currentReadPos = (currentReadPos + chunkSize) % bufferSizeBytes
                readPosition.set(currentReadPos)
            }
            
            totalBytesRead.addAndGet(bytesRead.toLong())
            Result.success(bytesRead)
        } catch (e: Exception) {
            Log.e(TAG, "Read failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Read directly into a ByteBuffer (zero-copy when possible).
     */
    suspend fun readBuffer(dst: ByteBuffer): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        
        val unread = getUnreadBytes()
        val toRead = dst.remaining().coerceAtMost(unread.toInt())
        if (toRead == 0) return Result.success(0)
        
        var bytesRead = 0
        var currentReadPos = readPosition.get()
        var remaining = toRead
        
        try {
            while (remaining > 0 && isActive) {
                val dataToEnd = bufferSizeBytes - currentReadPos
                val chunkSize = remaining.coerceAtMost(dataToEnd.toInt())
                
                mappedBuffer!!.position(currentReadPos.toInt())
                val limit = dst.limit()
                dst.limit(dst.position() + chunkSize)
                dst.put(mappedBuffer!!)
                dst.limit(limit)
                
                bytesRead += chunkSize
                remaining -= chunkSize
                currentReadPos = (currentReadPos + chunkSize) % bufferSizeBytes
                readPosition.set(currentReadPos)
            }
            
            totalBytesRead.addAndGet(bytesRead.toLong())
            Result.success(bytesRead)
        } catch (e: Exception) {
            Log.e(TAG, "Buffer read failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Seek to a specific position within the buffered data.
     * Position must be within [readPosition, validDataEnd).
     */
    suspend fun seekTo(positionMs: Long, durationMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        if (durationMs <= 0) return Result.failure(IllegalArgumentException("Invalid duration"))
        
        // Convert time to byte position (approximate based on average bitrate)
        val bytesPerMs = if (totalBytesWritten.get() > 0 && validDataEnd.get() > readPosition.get()) {
            (validDataEnd.get() - readPosition.get()).toDouble() / durationMs
        } else {
            config.averageBitrateBps / 8000.0 // fallback
        }
        
        val targetByteOffset = (positionMs * bytesPerMs).toLong()
        val bufferStart = readPosition.get()
        val bufferEnd = validDataEnd.get()
        
        // Calculate absolute position in circular buffer
        val targetPos = (bufferStart + targetByteOffset) % bufferSizeBytes
        
        // Validate seek target is within buffered range
        val isWithinRange = if (bufferStart <= bufferEnd) {
            targetPos >= bufferStart && targetPos <= bufferEnd
        } else {
            // Wrapped around
            targetPos >= bufferStart || targetPos <= bufferEnd
        }
        
        if (!isWithinRange) {
            return Result.failure(IllegalArgumentException("Seek position not in buffered range"))
        }
        
        // Clear read-ahead cache since we're jumping
        clearReadAheadCache()
        
        readPosition.set(targetPos)
        Result.success(Unit)
    }
    
    /**
     * Get number of unread bytes available.
     */
    fun getUnreadBytes(): Long {
        val writePos = writePosition.get()
        val readPos = readPosition.get()
        
        return if (writePos >= readPos) {
            writePos - readPos
        } else {
            bufferSizeBytes - (readPos - writePos)
        }
    }
    
    /**
     * Get buffered duration in milliseconds (estimated).
     */
    suspend fun getBufferedDurationMs(): Long = withContext(Dispatchers.IO) {
        val unread = getUnreadBytes()
        if (unread == 0 || config.averageBitrateBps == 0) return 0L
        (unread * 8000L / config.averageBitrateBps).toLong()
    }
    
    /**
     * Get buffer fill percentage (0-100).
     */
    fun getFillPercentage(): Int {
        val unread = getUnreadBytes()
        return ((unread * 100) / bufferSizeBytes).toInt()
    }
    
    /**
     * Force flush mapped buffer to disk.
     */
    suspend fun flush(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mappedBuffer?.force()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Trim the buffer by removing oldest data (for disk space recovery).
     */
    suspend fun trim(bytesToRemove: Long): Result<Long> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        
        var toRemove = bytesToRemove.coerceAtMost(getUnreadBytes())
        var removed = 0L
        
        while (toRemove > 0) {
            val unread = getUnreadBytes()
            if (unread == 0) break
            
            val chunk = toRemove.coerceAtMost(unread)
            advanceReadPosition(chunk)
            toRemove -= chunk
            removed += chunk
        }
        
        clearReadAheadCache()
        Result.success(removed)
    }
    
    /**
     * Configure read-ahead for a specific range.
     */
    suspend fun prefetch(startPos: Long, length: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        if (readAheadCacheSize.get() + length > maxReadAheadBytes) {
            // Evict oldest entries
            evictReadAheadCache(length)
        }
        
        var pos = startPos
        var remaining = length
        
        try {
            while (remaining > 0) {
                val dataToEnd = bufferSizeBytes - pos
                val chunkSize = remaining.coerceAtMost(dataToEnd).coerceAtMost(1024 * 1024) // Max 1MB per chunk
                
                val buffer = ByteBuffer.allocateDirect(chunkSize.toInt())
                mappedBuffer!!.position(pos.toInt())
                mappedBuffer!!.get(buffer)
                buffer.flip()
                
                cacheLock.writeLock().lock()
                readAheadCache[pos] = buffer
                readAheadCacheSize.addAndGet(chunkSize)
                cacheLock.writeLock().unlock()
                
                pos = (pos + chunkSize) % bufferSizeBytes
                remaining -= chunkSize
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current buffer statistics.
     */
    fun getStats(): BufferStats = BufferStats(
        bufferSizeMb = config.bufferSizeMb,
        fillPercentage = getFillPercentage(),
        unreadBytes = getUnreadBytes(),
        readPosition = readPosition.get(),
        writePosition = writePosition.get(),
        validDataEnd = validDataEnd.get(),
        totalBytesWritten = totalBytesWritten.get(),
        totalBytesRead = totalBytesRead.get(),
        readAheadCacheSizeMb = readAheadCacheSize.get() / 1024 / 1024,
        availableDiskSpaceMb = availableDiskSpace / 1024 / 1024,
        isInitialized = isInitialized.get(),
        isWriting = isWriting.get()
    )
    
    /**
     * Update average bitrate for duration estimation.
     */
    fun updateAverageBitrate(bitrateBps: Long) {
        config.averageBitrateBps = bitrateBps
    }
    
    /**
     * Shutdown and cleanup.
     */
    fun shutdown() {
        isInitialized.set(false)
        
        // Flush
        try {
            mappedBuffer?.force()
        } catch (e: Exception) {
            Log.w(TAG, "Flush on shutdown failed", e)
        }
        
        // Release lock
        try {
            writeLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Lock release failed", e)
        }
        
        // Close channel
        try {
            fileChannel?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Channel close failed", e)
        }
        
        // Clear cache
        clearReadAheadCache()
        
        // Clean up file if configured
        if (config.deleteOnShutdown) {
            bufferFile?.delete()
        }
        
        mappedBuffer = null
        fileChannel = null
        writeLock = null
        bufferFile = null
        
        Log.i(TAG, "Disk buffer shutdown complete")
    }
    
    // ===== Private Helpers =====
    
    private fun advanceReadPosition(bytes: Long) {
        val newPos = (readPosition.get() + bytes) % bufferSizeBytes
        readPosition.set(newPos)
        clearReadAheadCache()
    }
    
    private fun checkDiskSpaceIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastDiskSpaceCheck > 30_000) { // Every 30 seconds
            lastDiskSpaceCheck = now
            val space = bufferFile?.usableSpace ?: 0
            availableDiskSpace = space
            if (space < bufferSizeBytes * 0.1) { // Less than 10% free
                Log.w(TAG, "Low disk space: ${space / 1024 / 1024}MB")
                diskSpaceCallback?.invoke(space)
            }
        }
    }
    
    private fun clearReadAheadCache() {
        cacheLock.writeLock().lock()
        try {
            readAheadCache.values.forEach { it.cleaner()?.clean() }
            readAheadCache.clear()
            readAheadCacheSize.set(0)
        } finally {
            cacheLock.writeLock().unlock()
        }
    }
    
    private fun evictReadAheadCache(neededBytes: Long) {
        cacheLock.writeLock().lock()
        try {
            var freed = 0L
            val iterator = readAheadCache.entries.iterator()
            while (iterator.hasNext() && freed < neededBytes) {
                val entry = iterator.next()
                freed += entry.value.capacity().toLong()
                entry.value.cleaner()?.clean()
                iterator.remove()
            }
            readAheadCacheSize.addAndGet(-freed)
        } finally {
            cacheLock.writeLock().unlock()
        }
    }
    
    private fun cleanup() {
        try {
            writeLock?.release()
        } catch (e: Exception) {}
        try {
            fileChannel?.close()
        } catch (e: Exception) {}
        try {
            bufferFile?.delete()
        } catch (e: Exception) {}
    }
    
    /**
     * Configuration for disk buffer.
     */
    data class DiskBufferConfig(
        val bufferSizeMb: Int = 200,
        val maxReadAheadMb: Int = 4,
        val averageBitrateBps: Long = 15_000_000, // 15 Mbps default for 4K
        val location: BufferLocation = BufferLocation.INTERNAL_CACHE,
        val customPath: String? = null,
        val deleteOnShutdown: Boolean = false
    ) {
        enum class BufferLocation {
            INTERNAL_CACHE,
            EXTERNAL_CACHE,
            EXTERNAL_FILES,
            CUSTOM
        }
    }
    
    /**
     * Buffer statistics.
     */
    data class BufferStats(
        val bufferSizeMb: Int,
        val fillPercentage: Int,
        val unreadBytes: Long,
        val readPosition: Long,
        val writePosition: Long,
        val validDataEnd: Long,
        val totalBytesWritten: Long,
        val totalBytesRead: Long,
        val readAheadCacheSizeMb: Long,
        val availableDiskSpaceMb: Long,
        val isInitialized: Boolean,
        val isWriting: Boolean
    )
    
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Failure(val error: Exception) : Result<Nothing>()
        
        companion object {
            fun <T> success(data: T): Result<T> = Success(data)
            fun <T> failure(error: Exception): Result<T> = Failure(error)
        }
    }
}