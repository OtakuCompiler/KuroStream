package com.kurostream.players.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue

class DirectByteBufferPool(private val maxPoolSize: Int = 8) {
    private val pool = ConcurrentLinkedQueue<ByteBuffer>()

    fun acquire(size: Int): ByteBuffer {
        return pool.poll()?.takeIf { it.capacity() >= size }?.apply { clear() }
            ?: ByteBuffer.allocateDirect(size).also { it.order(ByteOrder.nativeOrder()) }
    }

    fun release(buffer: ByteBuffer) {
        if (pool.size < maxPoolSize) {
            pool.offer(buffer)
        }
    }

    fun trim() {
        while (pool.size > maxPoolSize / 2) {
            pool.poll()
        }
    }
}