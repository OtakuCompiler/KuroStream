package com.kurostream.cache

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class DiskAsRamCache(context: Context) {
    companion object {
        private const val CACHE_SIZE_BYTES = 125L * 1024 * 1024
        private const val CACHE_FILE_NAME = "kurostream_ram_cache.bin"
    }

    private val cacheFile: File = File(context.cacheDir, CACHE_FILE_NAME)
    private val randomAccessFile: RandomAccessFile = RandomAccessFile(cacheFile, "rw")
    private val channel: FileChannel = randomAccessFile.channel
    private val mappedBuffer: MappedByteBuffer = channel.map(
        FileChannel.MapMode.READ_WRITE,
        0,
        CACHE_SIZE_BYTES
    )

    fun getBuffer(): MappedByteBuffer = mappedBuffer

    fun flush() {
        mappedBuffer.force()
    }

    fun clear() {
        mappedBuffer.clear()
        while (mappedBuffer.hasRemaining()) {
            mappedBuffer.put(0)
        }
        mappedBuffer.clear()
        flush()
    }

    fun release() {
        flush()
        channel.close()
        randomAccessFile.close()
    }
}
