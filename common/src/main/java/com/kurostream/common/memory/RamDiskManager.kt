package com.kurostream.common.memory

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class RamDiskManager(context: Context, maxSizeBytes: Long = 125L * 1024 * 1024) {

    private val ramDiskDir = File(context.cacheDir, "ramdisk").apply { mkdirs() }
    private val maxSize = maxSizeBytes
    private val lock = Any()

    fun allocate(name: String, size: Int): MappedByteBuffer? {
        synchronized(lock) {
            if (getTotalUsed() + size > maxSize) {
                evictLRU(size)
            }
            val file = File(ramDiskDir, name)
            return try {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.setLength(size.toLong())
                    raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, size.toLong())
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun delete(name: String) {
        File(ramDiskDir, name).delete()
    }

    fun clear() {
        ramDiskDir.listFiles()?.forEach { it.delete() }
    }

    private fun getTotalUsed(): Long {
        return ramDiskDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    private fun evictLRU(requiredBytes: Int) {
        val files = ramDiskDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var freed = 0L
        for (file in files) {
            if (freed >= requiredBytes) break
            freed += file.length()
            file.delete()
        }
    }
}
