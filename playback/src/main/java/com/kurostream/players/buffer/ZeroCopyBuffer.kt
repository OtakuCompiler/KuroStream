package com.kurostream.players.buffer

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroCopyBuffer @Inject constructor() {

    private val _bufferState = MutableStateFlow(BufferState())
    val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    private val bufferQueue = LinkedBlockingDeque<ByteBuffer>(100)
    private val maxBufferSize = 50 * 1024 * 1024 // 50MB adaptive
    private var currentBufferSize = 0L
    private var totalBytesRead = 0L
    private var totalBytesWritten = 0L

    @Volatile private var isPlaying = false
    @Volatile private var targetBufferMs = 30_000L
    @Volatile private var minBufferMs = 10_000L

    fun configure(targetBufferMs: Long = 30_000L, minBufferMs: Long = 10_000L) {
        this.targetBufferMs = targetBufferMs
        this.minBufferMs = minBufferMs
        Log.d("ZeroCopyBuffer", "Configured: target=$targetBufferMs ms, min=$minBufferMs ms")
    }

    fun allocate(size: Int): ByteBuffer {
        val directBuffer = ByteBuffer.allocateDirect(size)
        currentBufferSize += size
        updateState()
        return directBuffer
    }

    fun queue(buffer: ByteBuffer): Boolean {
        if (currentBufferSize + buffer.remaining() > maxBufferSize) {
            Log.w("ZeroCopyBuffer", "Buffer full, dropping ${buffer.remaining()} bytes")
            return false
        }

        bufferQueue.offer(buffer)
        totalBytesWritten += buffer.remaining()
        updateState()
        return true
    }

    fun dequeue(): ByteBuffer? {
        val buffer = bufferQueue.poll()
        buffer?.let {
            currentBufferSize -= it.capacity()
            totalBytesRead += it.remaining()
        }
        updateState()
        return buffer
    }

    fun peek(): ByteBuffer? = bufferQueue.peek()

    fun availableBytes(): Int = bufferQueue.sumOf { it.remaining() }

    fun availableMs(bitrateBps: Long): Long {
        if (bitrateBps <= 0) return 0
        return (availableBytes() * 8L * 1000 / bitrateBps).coerceAtLeast(0)
    }

    fun isBufferHealthy(bitrateBps: Long): Boolean {
        return availableMs(bitrateBps) >= minBufferMs
    }

    fun isBufferFull(): Boolean {
        return currentBufferSize >= maxBufferSize * 0.9
    }

    fun clear() {
        bufferQueue.clear()
        currentBufferSize = 0
        updateState()
        Log.d("ZeroCopyBuffer", "Buffer cleared (RAM saved: ${currentBufferSize / 1024 / 1024}MB)")
    }

    fun trim(keepMs: Long = minBufferMs) {
        while (bufferQueue.size > 10 && currentBufferSize > maxBufferSize * 0.5) {
            val buffer = bufferQueue.pollFirst()
            buffer?.let {
                currentBufferSize -= it.capacity()
            }
        }
        updateState()
        Log.d("ZeroCopyBuffer", "Buffer trimmed, remaining: ${bufferQueue.size} chunks")
    }

    private fun updateState() {
        _bufferState.value = BufferState(
            queuedChunks = bufferQueue.size,
            maxChunks = 100,
            currentBytes = currentBufferSize,
            maxBytes = maxBufferSize,
            totalBytesRead = totalBytesRead,
            totalBytesWritten = totalBytesWritten,
            isPlaying = isPlaying,
            timestamp = System.currentTimeMillis(),
        )
    }

    fun setIsPlaying(playing: Boolean) {
        isPlaying = playing
        updateState()
    }

    fun getMemoryUsageMb(): Float = currentBufferSize / 1024f / 1024f

    fun release() {
        clear()
        Log.d("ZeroCopyBuffer", "Released (final RAM: ${getMemoryUsageMb()}MB)")
    }
}

data class BufferState(
    val queuedChunks: Int = 0,
    val maxChunks: Int = 100,
    val currentBytes: Long = 0,
    val maxBytes: Int = 50 * 1024 * 1024,
    val totalBytesRead: Long = 0,
    val totalBytesWritten: Long = 0,
    val isPlaying: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val utilizationPercent: Float get() = currentBytes.toFloat() / maxBytes * 100
    val currentMb: Float get() = currentBytes / 1024f / 1024f
    val maxMb: Float get() = maxBytes / 1024f / 1024f
}
