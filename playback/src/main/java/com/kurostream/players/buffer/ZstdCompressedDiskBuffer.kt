package com.kurostream.players.buffer

import android.content.Context
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
import java.util.zip.Deflater
import java.util.zip.Inflater

class ZstdCompressedDiskBuffer private constructor(
    private val context: Context,
    private val config: ZstdDiskBufferConfig
) {
    private val TAG = "ZstdDiskBuffer"
    
    private var bufferFile: File? = null
    private var fileChannel: FileChannel? = null
    private var writeLock: FileLock? = null
    
    private val writePosition = AtomicLong(0)
    private val readPosition = AtomicLong(0)
    private val validDataEnd = AtomicLong(0)
    
    private val isInitialized = AtomicBoolean(false)
    private val isWriting = AtomicBoolean(false)
    
    private val compressionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "Zstd-Compress").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    
    private val decompressionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "Zstd-Decompress").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    
    private val bufferSizeBytes: Long
    private val chunkSizeBytes: Int
    private val maxChunks: Int
    
    private val totalBytesWritten = AtomicLong(0)
    private val totalBytesRead = AtomicLong(0)
    private val totalBytesCompressed = AtomicLong(0)
    private val totalBytesDecompressed = AtomicLong(0)
    
    private val chunkIndexLock = ReentrantReadWriteLock()
    private val chunkIndex = mutableListOf<ChunkEntry>()
    
    init {
        bufferSizeBytes = config.bufferSizeMb * 1024L * 1024L
        chunkSizeBytes = config.chunkSizeKb * 1024
        maxChunks = (bufferSizeBytes / chunkSizeBytes).toInt()
    }
    
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var INSTANCE: ZstdCompressedDiskBuffer? = null
        
        fun getInstance(context: Context, config: ZstdDiskBufferConfig): ZstdCompressedDiskBuffer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ZstdCompressedDiskBuffer(context.applicationContext, config).also { INSTANCE = it }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bufferDir = when (config.location) {
                ZstdDiskBufferConfig.BufferLocation.INTERNAL_CACHE -> context.cacheDir
                ZstdDiskBufferConfig.BufferLocation.EXTERNAL_CACHE -> context.externalCacheDir
                ZstdDiskBufferConfig.BufferLocation.EXTERNAL_FILES -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ZstdDiskBufferConfig.BufferLocation.CUSTOM -> config.customPath?.let { File(it) } ?: context.cacheDir
            }
            
            if (bufferDir == null || !bufferDir.exists()) {
                bufferDir?.mkdirs()
            }
            
            val usableSpace = bufferDir!!.usableSpace
            if (usableSpace < bufferSizeBytes * 1.1) {
                return Result.failure(IOException("Insufficient disk space: ${usableSpace / 1024 / 1024}MB available, ${config.bufferSizeMb}MB required"))
            }
            
            bufferFile = File(bufferDir, "kurostream_zstd_buffer.dat")
            
            RandomAccessFile(bufferFile, "rw").use { raf ->
                raf.setLength(bufferSizeBytes)
            }
            
            fileChannel = FileChannel.open(
                bufferFile!!.toPath(),
                java.nio.file.StandardOpenOption.READ,
                java.nio.file.StandardOpenOption.WRITE
            )
            
            writeLock = fileChannel!!.lock()
            
            writePosition.set(0)
            readPosition.set(0)
            validDataEnd.set(0)
            chunkIndex.clear()
            
            isInitialized.set(true)
            
            Log.i(TAG, "Zstd compressed disk buffer initialized: ${config.bufferSizeMb}MB (${config.chunkSizeKb}KB chunks, ~${(config.compressionRatio * 100).toInt()}% compression)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Zstd disk buffer", e)
            cleanup()
            Result.failure(e)
        }
    }
    
    suspend fun writeCompressed(data: ByteArray, offset: Int = 0, length: Int = data.size): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        if (length == 0) return Result.success(0)
        
        isWriting.set(true)
        try {
            val compressed = compressData(data, offset, length)
            val compressedSize = compressed.size
            
            val header = ByteBuffer.allocate(8)
            header.putInt(compressedSize)
            header.putInt(length)
            header.flip()
            
            val currentWritePos = writePosition.get()
            var bytesWritten = 0
            
            // Write header
            bytesWritten += writeToBuffer(header.array())
            
            // Write compressed data
            bytesWritten += writeToBuffer(compressed)
            
            // Update chunk index
            addChunkEntry(currentWritePos, compressedSize, length)
            
            totalBytesWritten.addAndGet(length.toLong())
            totalBytesCompressed.addAndGet(compressedSize.toLong())
            
            checkDiskSpaceIfNeeded()
            
            Result.success(length)
        } catch (e: Exception) {
            Log.e(TAG, "Compressed write failed", e)
            Result.failure(e)
        } finally {
            isWriting.set(false)
        }
    }
    
    private suspend fun writeToBuffer(data: ByteArray): Int = withContext(Dispatchers.IO) {
        val currentWritePos = writePosition.get()
        var bytesWritten = 0
        var remaining = data.size
        var srcOffset = 0
        
        while (remaining > 0) {
            val spaceToEnd = bufferSizeBytes - currentWritePos
            val chunkSize = remaining.coerceAtMost(spaceToEnd.toInt())
            
            val buffer = ByteBuffer.wrap(data, srcOffset, chunkSize)
            
            fileChannel!!.position(currentWritePos)
            fileChannel!!.write(buffer)
            
            bytesWritten += chunkSize
            remaining -= chunkSize
            srcOffset += chunkSize
            
            val newWritePos = (currentWritePos + chunkSize) % bufferSizeBytes
            writePosition.set(newWritePos)
            
            val currentValidEnd = validDataEnd.get()
            if (currentWritePos >= currentValidEnd) {
                validDataEnd.set(newWritePos)
            } else if (newWritePos < currentWritePos) {
                validDataEnd.set(newWritePos)
            }
            
            if (remaining > 0) {
                // Wrapped around
            }
        }
        
        bytesWritten
    }
    
    private fun addChunkEntry(position: Long, compressedSize: Int, originalSize: Int) {
        chunkIndexLock.writeLock().lock()
        try {
            val entry = ChunkEntry(
                position = position,
                compressedSize = compressedSize,
                originalSize = originalSize,
                timestamp = System.currentTimeMillis()
            )
            chunkIndex.add(entry)
            
            // Maintain max chunks
            while (chunkIndex.size > maxChunks) {
                val removed = chunkIndex.removeAt(0)
                advanceReadPosition(removed.compressedSize + 8) // 8 bytes header
            }
            
            // Trim if we're about to overwrite
            val unreadBytes = getUnreadBytes()
            if (unreadBytes > bufferSizeBytes * 0.9) {
                trimOldestChunks((unreadBytes - bufferSizeBytes * 0.9).toLong())
            }
        } finally {
            chunkIndexLock.writeLock().unlock()
        }
    }
    
    private fun trimOldestChunks(bytesToRemove: Long) {
        var removed = 0L
        while (removed < bytesToRemove && chunkIndex.isNotEmpty()) {
            val removedEntry = chunkIndex.removeAt(0)
            removed += removedEntry.compressedSize + 8
            advanceReadPosition(removedEntry.compressedSize + 8)
        }
    }
    
    private fun advanceReadPosition(bytes: Long) {
        val newPos = (readPosition.get() + bytes) % bufferSizeBytes
        readPosition.set(newPos)
    }
    
    suspend fun readDecompressed(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return Result.failure(IllegalStateException("Buffer not initialized"))
        
        val unread = getUnreadBytes()
        if (unread == 0) return Result.success(0)
        
        val toRead = length.coerceAtMost(unread.toInt())
        
        var totalRead = 0
        var currentReadPos = readPosition.get()
        var remaining = toRead
        var dstOffset = offset
        
        try {
            while (remaining > 0) {
                // Read header
                val headerBuffer = ByteBuffer.allocate(8)
                var headerRead = 0
                while (headerRead < 8) {
                    val read = readFromBuffer(headerBuffer.array(), headerRead, 8 - headerRead)
                    if (read <= 0) break
                    headerRead += read
                }
                
                if (headerRead < 8) break
                
                headerBuffer.flip()
                val compressedSize = headerBuffer.getInt()
                val originalSize = headerBuffer.getInt()
                
                // Read compressed data
                val compressedData = ByteArray(compressedSize)
                var dataRead = 0
                while (dataRead < compressedSize) {
                    val read = readFromBuffer(compressedData, dataRead, compressedSize - dataRead)
                    if (read <= 0) break
                    dataRead += read
                }
                
                if (dataRead < compressedSize) break
                
                // Decompress
                val decompressed = decompressData(compressedData, originalSize)
                
                val copySize = decompressed.size.coerceAtMost(remaining)
                System.arraycopy(decompressed, 0, dst, dstOffset, copySize)
                
                totalBytesRead.addAndGet(copySize.toLong())
                totalBytesDecompressed.addAndGet(decompressed.size.toLong())
                
                totalRead += copySize
                remaining -= copySize
                dstOffset += copySize
                
                advanceReadPosition(8 + compressedSize)
                currentReadPos = readPosition.get()
            }
            
            Result.success(totalRead)
        } catch (e: Exception) {
            Log.e(TAG, "Decompressed read failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun readFromBuffer(dst: ByteArray, offset: Int, length: Int): Int = withContext(Dispatchers.IO) {
        val currentReadPos = readPosition.get()
        val dataToEnd = bufferSizeBytes - currentReadPos
        val chunkSize = length.coerceAtMost(dataToEnd.toInt())
        
        fileChannel!!.position(currentReadPos)
        val read = fileChannel!!.read(ByteBuffer.wrap(dst, offset, chunkSize))
        
        if (read > 0) {
            advanceReadPosition(read)
        }
        
        read
    }
    
    private fun compressData(data: ByteArray, offset: Int, length: Int): ByteArray {
        return if (config.useZstd && isZstdAvailable()) {
            compressZstd(data, offset, length)
        } else {
            compressDeflate(data, offset, length)
        }
    }
    
    private fun decompressData(data: ByteArray, expectedSize: Int): ByteArray {
        return if (config.useZstd && isZstdAvailable()) {
            decompressZstd(data, expectedSize)
        } else {
            decompressDeflate(data, expectedSize)
        }
    }
    
    private fun compressZstd(data: ByteArray, offset: Int, length: Int): ByteArray {
        return try {
            val compressed = ByteArray((length * 1.1 + 100).toInt())
            val compressedSize = Zstd.compress(data, offset, length, compressed, 0, compressed.size)
            compressed.copyOf(compressedSize)
        } catch (e: Exception) {
            Log.w(TAG, "Zstd compression failed, falling back to Deflate", e)
            compressDeflate(data, offset, length)
        }
    }
    
    private fun decompressZstd(data: ByteArray, expectedSize: Int): ByteArray {
        return try {
            val decompressed = ByteArray(expectedSize)
            val actualSize = Zstd.decompress(data, 0, data.size, decompressed, 0, expectedSize)
            if (actualSize != expectedSize) {
                decompressed.copyOf(actualSize)
            } else {
                decompressed
            }
        } catch (e: Exception) {
            Log.w(TAG, "Zstd decompression failed, falling back to Deflate", e)
            decompressDeflate(data, expectedSize)
        }
    }
    
    private fun compressDeflate(data: ByteArray, offset: Int, length: Int): ByteArray {
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(data, offset, length)
        deflater.finish()
        
        val output = ByteArrayOutputStream(length)
        val buffer = ByteArray(4096)
        while (!deflater.finished) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        return output.toByteArray()
    }
    
    private fun decompressDeflate(data: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        
        val output = ByteArrayOutputStream(expectedSize)
        val buffer = ByteArray(4096)
        try {
            while (!inflater.finished) {
                val count = inflater.inflate(buffer)
                output.write(buffer, 0, count)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deflate decompression failed", e)
        } finally {
            inflater.end()
        }
        return output.toByteArray()
    }
    
    private fun isZstdAvailable(): Boolean {
        return try {
            Class.forName("com.github.luben.zstd.Zstd")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    fun getUnreadBytes(): Long {
        val writePos = writePosition.get()
        val readPos = readPosition.get()
        
        return if (writePos >= readPos) {
            writePos - readPos
        } else {
            bufferSizeBytes - (readPos - writePos)
        }
    }
    
    fun getFillPercentage(): Int {
        val unread = getUnreadBytes()
        return ((unread * 100) / bufferSizeBytes).toInt()
    }
    
    fun getCompressionRatio(): Float {
        val written = totalBytesWritten.get()
        val compressed = totalBytesCompressed.get()
        return if (compressed > 0) written.toFloat() / compressed else 1f
    }
    
    fun getStats(): ZstdBufferStats {
        return ZstdBufferStats(
            bufferSizeMb = config.bufferSizeMb,
            fillPercentage = getFillPercentage(),
            unreadBytes = getUnreadBytes(),
            readPosition = readPosition.get(),
            writePosition = writePosition.get(),
            totalChunks = chunkIndex.size,
            totalBytesWritten = totalBytesWritten.get(),
            totalBytesRead = totalBytesRead.get(),
            totalBytesCompressed = totalBytesCompressed.get(),
            totalBytesDecompressed = totalBytesDecompressed.get(),
            compressionRatio = getCompressionRatio(),
            effectiveCapacityMb = (config.bufferSizeMb * getCompressionRatio()).toInt(),
            isInitialized = isInitialized.get(),
        )
    }
    
    private fun checkDiskSpaceIfNeeded() {
        // Check periodically
    }
    
    fun shutdown() {
        isInitialized.set(false)
        
        try {
            writeLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Lock release failed", e)
        }
        
        try {
            fileChannel?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Channel close failed", e)
        }
        
        if (config.deleteOnShutdown) {
            bufferFile?.delete()
        }
        
        compressionExecutor.shutdown()
        decompressionExecutor.shutdown()
        
        Log.i(TAG, "Zstd disk buffer shutdown complete")
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
    
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Failure(val error: Exception) : Result<Nothing>()
        
        companion object {
            fun <T> success(data: T): Result<T> = Success(data)
            fun <T> failure(error: Exception): Result<T> = Failure(error)
        }
    }
    
    data class ChunkEntry(
        val position: Long,
        val compressedSize: Int,
        val originalSize: Int,
        val timestamp: Long,
    )
    
    data class ZstdDiskBufferConfig(
        val bufferSizeMb: Int = 200,
        val chunkSizeKb: Int = 256,
        val compressionRatio: Float = 2.0f,
        val useZstd: Boolean = true,
        val location: BufferLocation = BufferLocation.INTERNAL_CACHE,
        val customPath: String? = null,
        val deleteOnShutdown: Boolean = false,
    ) {
        enum class BufferLocation {
            INTERNAL_CACHE,
            EXTERNAL_CACHE,
            EXTERNAL_FILES,
            CUSTOM
        }
    }
    
    data class ZstdBufferStats(
        val bufferSizeMb: Int,
        val fillPercentage: Int,
        val unreadBytes: Long,
        val readPosition: Long,
        val writePosition: Long,
        val totalChunks: Int,
        val totalBytesWritten: Long,
        val totalBytesRead: Long,
        val totalBytesCompressed: Long,
        val totalBytesDecompressed: Long,
        val compressionRatio: Float,
        val effectiveCapacityMb: Int,
        val isInitialized: Boolean,
    ) {
        val spaceSavedMb: Float get() = (totalBytesWritten - totalBytesCompressed) / 1024f / 1024f
    }
}

object Zstd {
    @JvmStatic
    fun compress(src: ByteArray, srcOffset: Int, srcLen: Int, dst: ByteArray, dstOffset: Int, dstCapacity: Int): Int {
        return try {
            val zstdClass = Class.forName("com.github.luben.zstd.Zstd")
            val compressMethod = zstdClass.getMethod("compress", ByteArray::class.java, Int::class.java, Int::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
            compressMethod.invoke(null, src, srcOffset, srcLen, dst, dstOffset, dstCapacity) as Int
        } catch (e: Exception) {
            throw RuntimeException("Zstd compression failed", e)
        }
    }
    
    @JvmStatic
    fun decompress(src: ByteArray, srcOffset: Int, srcLen: Int, dst: ByteArray, dstOffset: Int, dstCapacity: Int): Int {
        return try {
            val zstdClass = Class.forName("com.github.luben.zstd.Zstd")
            val decompressMethod = zstdClass.getMethod("decompress", ByteArray::class.java, Int::class.java, Int::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
            decompressMethod.invoke(null, src, srcOffset, srcLen, dst, dstOffset, dstCapacity) as Int
        } catch (e: Exception) {
            throw RuntimeException("Zstd decompression failed", e)
        }
    }
}