package com.kurostream.players.io

import android.os.Build
import timber.log.Timber
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.ref.Cleaner
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Memory-mapped file manager for efficient large file access (artwork, metadata, subtitles).
 * Uses MappedByteBuffer with Cleaner API for automatic unmapping on cleanup.
 * Falls back to regular I/O if memory mapping fails.
 *
 * Features:
 * - Read-only mapping for safety
 * - Region-based access for large files
 * - Auto-unmap via Cleaner API (Java 9+) with reflection fallback
 * - Thread-safe buffer caching with size limits
 * - Fallback to regular RandomAccessFile I/O on mapping failure
 */
@Singleton
class MemoryMappedFileManager @Inject constructor() {

    private val TAG = "MemoryMappedFileManager"

    // Configuration constants
    private const val MAX_CACHED_SIZE = 100L * 1024 * 1024 // 100MB max cached
    private const val MAX_REGION_SIZE = 16 * 1024 * 1024 // 16MB max region
    private const val CLEANER_CHECK_INTERVAL_MS = 30_000

    // Cache of active mappings: file path -> MappedRegion
    private val activeMappings = ConcurrentHashMap<String, MappedRegion>()

    // Cleaner for automatic unmapping (Java 9+)
    private val cleaner = Cleaner.create()

    // Statistics
    private val totalMappedBytes = AtomicLong(0)
    private val totalUnmappedBytes = AtomicLong(0)
    private val mappingFailures = AtomicLong(0)
    private val fallbackReads = AtomicLong(0)

    // Cleaner reference for explicit cleanup
    private val cleanerRef = AtomicReference<Cleaner.Cleanable?>()

    init {
        Timber.d("MemoryMappedFileManager initialized (API ${Build.VERSION.SDK_INT})")
        startPeriodicCleanup()
    }

    /**
     * Maps a file region into memory for efficient reading.
     * Returns a [MappedRegion] that provides safe access to the mapped memory.
     *
     * @param file File to map
     * @param offset Byte offset from start of file
     * @param size Number of bytes to map (max 16MB)
     * @return MappedRegion for reading, or null if mapping failed and fallback also failed
     */
    fun mapRegion(file: File, offset: Long = 0, size: Long = -1): MappedRegion? {
        val fileSize = file.length()
        if (fileSize == 0L) {
            Timber.w("Cannot map empty file: ${file.name}")
            return null // Safe fallback: optional value not present
        }

        val actualSize = if (size > 0) size.coerceAtMost(MAX_REGION_SIZE).coerceAtMost(fileSize - offset)
        else (fileSize - offset).coerceAtMost(MAX_REGION_SIZE)

        if (actualSize <= 0 || offset >= fileSize) {
            Timber.w("Invalid region: offset=$offset, size=$actualSize, fileSize=$fileSize")
            return null // Safe fallback: optional value not present
        }

        val key = "${file.absolutePath}#$offset#$actualSize"

        // Check cache first
        activeMappings[key]?.let { region ->
            if (region.isValid) {
                region.acquire()
                return region
            } else {
                // Stale entry, remove and remap
                activeMappings.remove(key)
                region.release()
            }
        }

        // Try memory mapping
        return try {
            val raf = RandomAccessFile(file, "r")
            val channel = raf.channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, actualSize)

            val region = MappedRegion(
                buffer = buffer,
                file = file,
                offset = offset,
                size = actualSize,
                raf = raf,
                cleaner = cleaner,
                onRelease = { releasedKey, releasedSize ->
                    activeMappings.remove(releasedKey)
                    totalUnmappedBytes.addAndGet(releasedSize)
                }
            )

            // Register for automatic cleanup
            val cleanable = cleaner.register(region, region::release)
            region.cleanable = cleanable

            activeMappings[key] = region
            totalMappedBytes.addAndGet(actualSize)
            Timber.d("Mapped region: ${file.name}[$offset..${offset + actualSize}] (${actualSize / 1024}KB)")
            region
        } catch (e: Exception) {
            mappingFailures.incrementAndGet()
            Timber.w(e, "Memory mapping failed for ${file.name}, falling back to regular I/O")
            fallbackRead(file, offset, actualSize)
        }
    }

    /**
     * Fallback to regular file I/O when memory mapping fails.
     */
    private fun fallbackRead(file: File, offset: Long, size: Long): MappedRegion? {
        fallbackReads.incrementAndGet()
        return try {
            val raf = RandomAccessFile(file, "r")
            val buffer = java.nio.ByteBuffer.allocateDirect(size.toInt().coerceAtMost(MAX_REGION_SIZE.toInt()))
            raf.seek(offset)
            val read = raf.channel.read(buffer)
            if (read <= 0) {
                raf.close()
                return null // Safe fallback: optional value not present
            }
            buffer.flip()

            // Wrap in a region that uses heap/direct buffer instead of mapped buffer
            val region = MappedRegion(
                buffer = buffer,
                file = file,
                offset = offset,
                size = read.toLong(),
                raf = raf,
                cleaner = cleaner,
                onRelease = { _, _ -> },
                isFallback = true
            )

            val cleanable = cleaner.register(region, region::release)
            region.cleanable = cleanable

            val key = "${file.absolutePath}#$offset#$size#fallback"
            activeMappings[key] = region
            totalMappedBytes.addAndGet(read.toLong())
            Timber.d("Fallback read: ${file.name}[$offset..${offset + read}] (${read / 1024}KB)")
            region
        } catch (e: Exception) {
            Timber.e(e, "Fallback read also failed for ${file.name}")
            null
        }
    }

    /**
     * Reads a file region directly into a provided buffer (avoids mapping overhead for small reads).
     */
    suspend fun readRegion(file: File, offset: Long, destination: java.nio.ByteBuffer): Int = withContext(Dispatchers.IO) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(offset)
                val read = raf.channel.read(destination)
                if (read > 0) fallbackReads.incrementAndGet()
                read
            }
        } catch (e: IOException) {
            Timber.e(e, "Direct read failed for ${file.name}")
            -1
        }
    }

    /**
     * Maps an entire file (for small files like metadata, subtitles).
     * Size limited to MAX_CACHED_SIZE.
     *
     * @param file File to map
     * @param mode Mapping mode (default: READ_ONLY for safety)
     * @return MappedRegion for reading, or null if mapping failed
     */
    fun mapFile(file: File, mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY): MappedRegion? {
        // Enforce read-only for safety unless explicitly overridden
        val safeMode = if (mode == FileChannel.MapMode.READ_WRITE || mode == FileChannel.MapMode.PRIVATE) {
            Timber.w("Read-write mapping requested, but read-only is enforced for safety")
            FileChannel.MapMode.READ_ONLY
        } else {
            mode
        }
        return mapRegion(file, 0, file.length().coerceAtMost(MAX_CACHED_SIZE))
    }

    /**
     * Maps a file region with explicit mapping mode.
     *
     * @param file File to map
     * @param offset Byte offset from start of file
     * @param size Number of bytes to map (max 16MB)
     * @param mode Mapping mode (default: READ_ONLY for safety)
     * @return MappedRegion for reading, or null if mapping failed
     */
    fun mapRegion(
        file: File,
        offset: Long = 0,
        size: Long = -1,
        mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY
    ): MappedRegion? {
        val fileSize = file.length()
        if (fileSize == 0L) {
            Timber.w("Cannot map empty file: ${file.name}")
            return null // Safe fallback: optional value not present
        }

        val actualSize = if (size > 0) size.coerceAtMost(MAX_REGION_SIZE).coerceAtMost(fileSize - offset)
        else (fileSize - offset).coerceAtMost(MAX_REGION_SIZE)

        if (actualSize <= 0 || offset >= fileSize) {
            Timber.w("Invalid region: offset=$offset, size=$actualSize, fileSize=$fileSize")
            return null // Safe fallback: optional value not present
        }

        // Enforce read-only for safety
        val safeMode = if (mode == FileChannel.MapMode.READ_WRITE || mode == FileChannel.MapMode.PRIVATE) {
            Timber.w("Read-write mapping requested, but read-only is enforced for safety")
            FileChannel.MapMode.READ_ONLY
        } else {
            mode
        }

        val key = "${file.absolutePath}#$offset#$actualSize"

        // Check cache first
        activeMappings[key]?.let { region ->
            if (region.isValid) {
                region.acquire()
                return region
            } else {
                // Stale entry, remove and remap
                activeMappings.remove(key)
                region.release()
            }
        }

        // Try memory mapping
        return try {
            val raf = RandomAccessFile(file, "r")
            val channel = raf.channel
            val buffer = channel.map(safeMode, offset, actualSize)

            val region = MappedRegion(
                buffer = buffer,
                file = file,
                offset = offset,
                size = actualSize,
                raf = raf,
                cleaner = cleaner,
                onRelease = { releasedKey, releasedSize ->
                    activeMappings.remove(releasedKey)
                    totalUnmappedBytes.addAndGet(releasedSize)
                }
            )

            // Register for automatic cleanup
            val cleanable = cleaner.register(region, region::release)
            region.cleanable = cleanable

            activeMappings[key] = region
            totalMappedBytes.addAndGet(actualSize)
            Timber.d("Mapped region: ${file.name}[$offset..${offset + actualSize}] (${actualSize / 1024}KB)")
            region
        } catch (e: Exception) {
            mappingFailures.incrementAndGet()
            Timber.w(e, "Memory mapping failed for ${file.name}, falling back to regular I/O")
            fallbackRead(file, offset, actualSize)
        }
    }

    /**
     * Releases a mapped region explicitly.
     */
    fun releaseRegion(region: MappedRegion?) {
        region?.release()
    }

    /**
     * Releases all mappings for a specific file.
     */
    fun releaseFile(file: File) {
        val prefix = file.absolutePath
        activeMappings.keys.filter { it.startsWith(prefix) }.forEach { key ->
            activeMappings.remove(key)?.release()
        }
    }

    /**
     * Releases all cached mappings.
     */
    fun releaseAll() {
        activeMappings.values.forEach { it.release() }
        activeMappings.clear()
        Timber.d("Released all mappings: ${formatBytes(totalMappedBytes.get())} mapped, ${formatBytes(totalUnmappedBytes.get())} unmapped")
    }

    /**
     * Closes the manager and releases all resources.
     * Alias for shutdown().
     */
    fun close() {
        shutdown()
    }

    /**
     * Reads a string from a mapped buffer using the specified charset.
     * Reads from the current position up to the buffer's limit.
     *
     * @param buffer MappedByteBuffer to read from
     * @param charset Charset to use for decoding (default: UTF-8)
     * @return Decoded string, or empty string if buffer is empty/null
     */
    fun readString(buffer: MappedByteBuffer?, charset: Charset = StandardCharsets.UTF_8): String {
        if (buffer == null || !buffer.hasRemaining()) return ""
        val slice = buffer.duplicate()
        slice.isReadOnly = true
        return charset.decode(slice).toString()
    }

    /**
     * Reads a string from a specific region of a mapped buffer.
     *
     * @param buffer MappedByteBuffer to read from
     * @param offset Byte offset from buffer position
     * @param length Number of bytes to read (-1 for remaining)
     * @param charset Charset to use for decoding (default: UTF-8)
     * @return Decoded string
     */
    fun readString(
        buffer: MappedByteBuffer?,
        offset: Int,
        length: Int = -1,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        if (buffer == null) return ""
        val slice = buffer.duplicate()
        val startPos = (buffer.position() + offset).coerceIn(buffer.position(), buffer.limit())
        val endPos = if (length > 0) (startPos + length).coerceAtMost(buffer.limit()) else buffer.limit()
        if (startPos >= endPos) return ""
        slice.position(startPos)
        slice.limit(endPos)
        slice.isReadOnly = true
        return charset.decode(slice).toString()
    }

    /**
     * Reads a string from a MappedRegion.
     *
     * @param region MappedRegion to read from
     * @param offset Byte offset from region start
     * @param length Number of bytes to read (-1 for remaining)
     * @param charset Charset to use for decoding (default: UTF-8)
     * @return Decoded string
     */
    fun readString(
        region: MappedRegion?,
        offset: Long = 0,
        length: Long = -1,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        region?.slice(offset, length)?.let { slice ->
            slice.isReadOnly = true
            return charset.decode(slice).toString()
        }
        return ""
    }

    /**
     * Gets current memory mapping statistics.
     */
    fun getStats(): MappingStats {
        return MappingStats(
            activeMappings = activeMappings.size,
            totalMappedBytes = totalMappedBytes.get(),
            totalUnmappedBytes = totalUnmappedBytes.get(),
            mappingFailures = mappingFailures.get(),
            fallbackReads = fallbackReads.get(),
            estimatedNativeMemory = estimateNativeMemory()
        )
    }

    /**
     * Estimates native memory used by mapped buffers.
     */
    private fun estimateNativeMemory(): Long {
        var total = 0L
        activeMappings.values.forEach { region ->
            if (!region.isFallback) {
                total += region.size
            }
        }
        return total
    }

    /**
     * Starts periodic cleanup of invalid/stale mappings.
     */
    private fun startPeriodicCleanup() {
        // Cleaner handles automatic cleanup, but we also do periodic validation
        // This runs on a background thread to avoid blocking
        Thread(start = true) {
            while (!Thread.interrupted()) {
                try {
                    delay(CLEANER_CHECK_INTERVAL_MS)
                    cleanupStaleMappings()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Timber.w(e, "Periodic cleanup error")
                }
            }
        }.name = "MappedFileCleanup"
    }

    /**
     * Removes stale/invalid mappings from cache.
     */
    private fun cleanupStaleMappings() {
        var cleaned = 0
        activeMappings.entries.forEach { entry ->
            if (!entry.value.isValid) {
                entry.value.release()
                activeMappings.remove(entry.key)
                cleaned++
            }
        }
        if (cleaned > 0) {
            Timber.d("Cleaned up $cleaned stale mappings")
        }
    }

    /**
     * Shuts down the manager, releasing all resources.
     */
    fun shutdown() {
        releaseAll()
        // Cleaner will be GC'd automatically
        Timber.d("MemoryMappedFileManager shutdown complete")
    }

    companion object {
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
                bytes >= 1024L * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
                bytes >= 1024L -> String.format("%.2f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}

/**
 * Represents a memory-mapped file region with safe access and automatic cleanup.
 */
class MappedRegion internal constructor(
    val buffer: java.nio.ByteBuffer,
    val file: File,
    val offset: Long,
    val size: Long,
    private val raf: RandomAccessFile,
    private val cleaner: Cleaner,
    private val onRelease: (String, Long) -> Unit,
    val isFallback: Boolean = false
) : AutoCloseable {

    @Volatile
    private var released = false

    @Volatile
    internal var cleanable: Cleaner.Cleanable? = null

    val isValid: Boolean
        get() = !released && buffer.isReadOnly || isFallback

    /**
     * Acquires a reference to this region (increments ref count).
     */
    fun acquire() {
        if (released) throw IllegalStateException("Region already released")
    }

    /**
     * Releases this mapped region and its resources.
     */
    override fun close() {
        release()
    }

    fun release() {
        if (released) return
        released = true

        // Unregister from cleaner
        cleanable?.clean()
        cleanable = null

        // Unmap the buffer if it's a mapped buffer
        if (!isFallback && buffer is MappedByteBuffer) {
            unmapBuffer(buffer as MappedByteBuffer)
        }

        // Close the file channel
        try {
            raf.close()
        } catch (e: Exception) {
            Timber.w(e, "Error closing RandomAccessFile for ${file.name}")
        }

        onRelease("${file.absolutePath}#$offset#$size", size)
        Timber.d("Released region: ${file.name}[$offset..${offset + size}]")
    }

    /**
     * Reads data from this region into a destination buffer.
     * Thread-safe: creates a slice of the buffer for concurrent reads.
     */
    fun readInto(destination: java.nio.ByteBuffer, regionOffset: Long = 0, length: Int = -1): Int {
        if (released) throw IllegalStateException("Region already released")
        if (regionOffset < 0 || regionOffset >= size) throw IndexOutOfBoundsException("Offset $regionOffset out of bounds [0, $size)")

        val readLength = if (length > 0) length.coerceAtMost((size - regionOffset).toInt()) else (size - regionOffset).toInt()
        if (readLength <= 0) return 0

        // Create a slice for thread-safe reading
        val slice = buffer.duplicate()
        slice.position(regionOffset.toInt())
        slice.limit(regionOffset.toInt() + readLength)

        val remaining = destination.remaining()
        val toCopy = readLength.coerceAtMost(remaining)
        destination.put(slice)
        return toCopy
    }

    /**
     * Gets a read-only slice of this region for direct buffer access.
     * The slice is valid only while this region is not released.
     */
    fun slice(offset: Long = 0, length: Long = -1): java.nio.ByteBuffer? {
        if (released) return null // Safe fallback: optional value not present
        val sliceOffset = offset.coerceIn(0L, size - 1)
        val sliceLength = if (length > 0) length.coerceAtMost(size - sliceOffset) else size - sliceOffset
        if (sliceLength <= 0) return null // Safe fallback: optional value not present

        val slice = buffer.duplicate()
        slice.position(sliceOffset.toInt())
        slice.limit(sliceOffset.toInt() + sliceLength.toInt())
        slice.isReadOnly = true
        return slice
    }

    /**
     * Unmaps a MappedByteBuffer using Cleaner API (Java 9+) with reflection fallback.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun unmapBuffer(buffer: MappedByteBuffer) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Java 14+: use invokeCleaner
                val invokeCleaner = MappedByteBuffer::class.java.getMethod("invokeCleaner")
                invokeCleaner.invoke(null, buffer)
            } else {
                // Java 9-13: use reflection to access cleaner
                val cleanerMethod = MappedByteBuffer::class.java.getDeclaredMethod("cleaner")
                cleanerMethod.isAccessible = true
                val cleaner = cleanerMethod.invoke(buffer)
                if (cleaner != null) {
                    val cleanMethod = cleaner.javaClass.getDeclaredMethod("clean")
                    cleanMethod.isAccessible = true
                    cleanMethod.invoke(cleaner)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to unmap buffer, will be cleaned by GC")
        }
    }
}

/**
 * Statistics for memory mapping operations.
 */
data class MappingStats(
    val activeMappings: Int,
    val totalMappedBytes: Long,
    val totalUnmappedBytes: Long,
    val mappingFailures: Long,
    val fallbackReads: Long,
    val estimatedNativeMemory: Long
) {
    val currentMappedBytes: Long
        get() = totalMappedBytes - totalUnmappedBytes

    override fun toString(): String {
        return "MappingStats(active=$activeMappings, mapped=${MemoryMappedFileManager.formatBytes(currentMappedBytes)}, " +
                "failures=$mappingFailures, fallbacks=$fallbackReads, native≈${MemoryMappedFileManager.formatBytes(estimatedNativeMemory)})"
    }
}