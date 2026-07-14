package com.kurostream.torrent.engine

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WriteCoalescer @Inject constructor() {

    private val TAG = "WriteCoalescer"

    data class WriteRequest(
        val file: File,
        val offset: Long,
        val data: ByteArray,
        val timestamp: Long = System.currentTimeMillis(),
    )

    data class CoalescerStats(
        val totalWrites: Long,
        val coalescedWrites: Long,
        val bytesWritten: Long,
        val flushCount: Long,
    )

    private val writeQueue = ConcurrentLinkedQueue<WriteRequest>()
    private val totalWrites = AtomicLong(0)
    private val coalescedWrites = AtomicLong(0)
    private val bytesWritten = AtomicLong(0)
    private val flushCount = AtomicLong(0)

    private val flushIntervalMs = 100L
    private val maxBufferSize = 1024 * 1024 // 1MB
    private val isRunning = AtomicBoolean(false)
    private var flushJob: Job? = null

    fun enqueueWrite(file: File, offset: Long, data: ByteArray) {
        writeQueue.offer(WriteRequest(file, offset, data))
        totalWrites.incrementAndGet()
    }

    fun start(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        flushJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    flushPendingWrites()
                } catch (e: Exception) {
                    Log.w(TAG, "Flush failed", e)
                }
                delay(flushIntervalMs)
            }
        }

        Log.i(TAG, "Write coalescer started (interval: ${flushIntervalMs}ms, buffer: ${maxBufferSize / 1024}KB)")
    }

    fun stop() {
        isRunning.set(false)
        flushJob?.cancel()
        flushJob = null
        Log.i(TAG, "Write coalescer stopped. Stats: ${getStats()}")
    }

    suspend fun flushPendingWrites() = withContext(Dispatchers.IO) {
        val pendingWrites = mutableListOf<WriteRequest>()
        while (writeQueue.isNotEmpty()) {
            val write = writeQueue.poll() ?: break
            pendingWrites.add(write)
        }

        if (pendingWrites.isEmpty()) return@withContext

        val groupedByFile = pendingWrites.groupBy { it.file }

        for ((file, writes) in groupedByFile) {
            try {
                val sortedWrites = writes.sortedBy { it.offset }
                val mergedWrites = mergeAdjacentWrites(sortedWrites)

                RandomAccessFile(file, "rw").use { raf ->
                    for (write in mergedWrites) {
                        raf.seek(write.offset)
                        raf.write(write.data)
                        bytesWritten.addAndGet(write.data.size.toLong())
                    }
                }

                coalescedWrites.addAndGet(mergedWrites.size.toLong())
                flushCount.incrementAndGet()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to ${file.name}", e)
            }
        }
    }

    private fun mergeAdjacentWrites(writes: List<WriteRequest>): List<WriteRequest> {
        if (writes.size <= 1) return writes

        val merged = mutableListOf<WriteRequest>()
        var current = writes[0]

        for (i in 1 until writes.size) {
            val next = writes[i]
            if (current.offset + current.data.size == next.offset) {
                val combinedData = current.data + next.data
                current = current.copy(data = combinedData)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged
    }

    fun getStats(): CoalescerStats = CoalescerStats(
        totalWrites = totalWrites.get(),
        coalescedWrites = coalescedWrites.get(),
        bytesWritten = bytesWritten.get(),
        flushCount = flushCount.get(),
    )

    fun getPendingCount(): Int = writeQueue.size
}
