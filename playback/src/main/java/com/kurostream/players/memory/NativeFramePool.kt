package com.kurostream.players.memory

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class NativeFramePool(
    private val bufferSize: Int = 1024 * 1024,
    private val maxBuffers: Int = 12,
) {
    private val pool = ConcurrentLinkedQueue<ByteBuffer>()
    private val allocated = AtomicInteger(0)

    init {
        repeat(maxBuffers) {
            pool.offer(ByteBuffer.allocateDirect(bufferSize))
            allocated.incrementAndGet()
        }
    }

    fun acquire(): ByteBuffer {
        val buffer = pool.poll()
        return if (buffer != null) {
            buffer.clear()
            buffer
        } else {
            ByteBuffer.allocateDirect(bufferSize)
        }
    }

    fun release(buffer: ByteBuffer) {
        if (buffer.capacity() == bufferSize && pool.size < maxBuffers) {
            buffer.clear()
            pool.offer(buffer)
        }
    }

    fun available(): Int = pool.size
    fun totalAllocated(): Int = allocated.get()

    fun destroy() {
        pool.clear()
    }
}
