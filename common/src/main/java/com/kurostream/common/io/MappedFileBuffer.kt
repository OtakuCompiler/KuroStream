package com.kurostream.common.io

import android.os.Build
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

object MappedFileBuffer {
    private const val TAG = "MappedFileBuffer"
    private val activeBuffers = ConcurrentHashMap<String, MappedByteBuffer>()
    private const val MAX_CACHED_SIZE = 100L * 1024 * 1024

    fun map(file: File, mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY): MappedByteBuffer? {
        val key = file.absolutePath
        activeBuffers[key]?.let { return it }

        return try {
            RandomAccessFile(file, if (mode == FileChannel.MapMode.READ_WRITE) "rw" else "r").use { raf ->
                val size = raf.length().coerceAtMost(MAX_CACHED_SIZE)
                if (size <= 0) return null
                val channel = raf.channel
                val buffer = channel.map(mode, 0, size)
                activeBuffers[key] = buffer
                buffer
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to map file: ${file.name}", e)
            null
        }
    }

    fun unmap(file: File) {
        val key = file.absolutePath
        activeBuffers.remove(key)?.let { buffer ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val invokeCleaner = MappedByteBuffer::class.java.getMethod("invokeCleaner")
                    invokeCleaner.invoke(null, buffer)
                } else {
                    val cleanerMethod = MappedByteBuffer::class.java.getDeclaredMethod("cleaner")
                    cleanerMethod.isAccessible = true
                    val cleaner = cleanerMethod.invoke(buffer) ?: return@let
                    val cleanMethod = cleaner.javaClass.getDeclaredMethod("clean")
                    cleanMethod.isAccessible = true
                    cleanMethod.invoke(cleaner)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean MappedByteBuffer", e)
            }
        }
    }

    fun clear() {
        activeBuffers.keys.toList().forEach { unmap(File(it)) }
        activeBuffers.clear()
    }
}
