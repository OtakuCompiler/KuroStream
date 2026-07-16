package com.kurostream.players.buffer

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectByteBufferPool @Inject constructor() {
    
    companion object {
        private const val TAG = "DirectByteBufferPool"
        private const val DEFAULT_POOL_SIZE = 32
        private const val MAX_POOL_SIZE = 128
        private const val FRAME_ALIGNMENT = 4096
    }
    
    private val pools = mutableMapOf<Int, Pool>()
    private val poolSizes = mutableMapOf<Int, AtomicInteger>()
    private val _poolStats = MutableStateFlow(PoolStats())
    val poolStats: StateFlow<PoolStats> = _poolStats.asStateFlow()
    
    fun acquire(capacity: Int): DirectByteBuffer {
        val alignedCapacity = alignCapacity(capacity)
        
        return pools.getOrPut(alignedCapacity) { createPool(alignedCapacity) }.acquire()
    }
    
    fun acquireForFormat(format: MediaFormat): DirectByteBuffer {
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        val colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
        
        val capacity = calculateBufferSize(width, height, colorFormat)
        return acquire(capacity)
    }
    
    fun acquireForVideoFrame(
        width: Int,
        height: Int,
        colorFormat: Int = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    ): DirectByteBuffer {
        val capacity = calculateBufferSize(width, height, colorFormat)
        return acquire(capacity)
    }
    
    fun release(buffer: DirectByteBuffer) {
        val pool = pools[buffer.capacity]
        pool?.release(buffer)
    }
    
    fun releaseToPool(buffer: ByteBuffer) {
        if (buffer.isDirect) {
            val capacity = buffer.capacity()
            val pool = pools[capacity]
            pool?.releaseDirect(buffer)
        }
    }
    
    fun getPoolSize(capacity: Int): Int = poolSizes[alignCapacity(capacity)]?.get() ?: 0
    
    fun getTotalAllocatedBytes(): Long = _poolStats.value.totalAllocatedBytes
    fun getTotalPooledBytes(): Long = _poolStats.value.totalPooledBytes
    fun getHitRate(): Float = _poolStats.value.hitRate
    
    private fun createPool(capacity: Int): Pool {
        val poolSize = calculateInitialPoolSize(capacity)
        poolSizes[capacity] = AtomicInteger(poolSize)
        
        return Pool(capacity, poolSize)
    }
    
    private fun calculateInitialPoolSize(capacity: Int): Int {
        return when {
            capacity <= 1024 * 1024 -> 16
            capacity <= 4 * 1024 * 1024 -> 8
            capacity <= 16 * 1024 * 1024 -> 4
            else -> 2
        }.coerceAtMost(MAX_POOL_SIZE)
    }
    
    private fun alignCapacity(capacity: Int): Int {
        return (capacity + FRAME_ALIGNMENT - 1) and (FRAME_ALIGNMENT - 1).inv()
    }
    
    private fun calculateBufferSize(width: Int, height: Int, colorFormat: Int): Int {
        return when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible -> {
                val ySize = width * height
                val uvSize = (width / 2) * (height / 2) * 2
                ySize + uvSize
            }
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar -> {
                val ySize = width * height
                val uvSize = (width / 2) * height * 2
                ySize + uvSize
            }
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444SemiPlanar -> {
                val ySize = width * height
                val uvSize = width * height * 2
                ySize + uvSize
            }
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar -> {
                val alignedWidth = (width + 127) and (-128)
                val alignedHeight = (height + 15) and (-16)
                alignedWidth * alignedHeight * 3 / 2
            }
            else -> width * height * 3 / 2
        }
    }
    
    fun trim(targetReduction: Float = 0.5f) {
        pools.values.forEach { pool ->
            pool.trim(targetReduction)
        }
        updateStats()
    }
    
    fun clearAll() {
        pools.values.forEach { it.clear() }
        pools.clear()
        poolSizes.clear()
        updateStats()
    }
    
    private fun updateStats() {
        var totalAllocated = 0L
        var totalPooled = 0L
        var totalAcquires = 0L
        var totalHits = 0L
        
        pools.values.forEach { pool ->
            totalAllocated += pool.totalAllocatedBytes
            totalPooled += pool.currentPooledBytes
            totalAcquires += pool.acquireCount
            totalHits += pool.hitCount
        }
        
        _poolStats.value = PoolStats(
            totalAllocatedBytes = totalAllocated,
            totalPooledBytes = totalPooled,
            activePools = pools.size,
            hitRate = if (totalAcquires > 0) totalHits.toFloat() / totalAcquires else 0f,
            totalAcquires = totalAcquires,
        )
    }
    
    inner class Pool(private val capacity: Int, initialSize: Int) {
        private val availableBuffers = Channel<DirectByteBuffer>(initialSize)
        private val allBuffers = mutableListOf<DirectByteBuffer>()
        
        var totalAllocatedBytes: Long = 0
        var currentPooledBytes: Long = 0
        var acquireCount: Long = 0
        var hitCount: Long = 0
        
        init {
            repeat(initialSize) {
                val buffer = allocateBuffer()
                availableBuffers.trySend(buffer)
            }
        }
        
        fun acquire(): DirectByteBuffer {
            acquireCount++
            
            return availableBuffers.poll()?.apply {
                hitCount++
                currentPooledBytes -= capacity.toLong()
                position(0)
                limit(capacity)
            } ?: allocateBuffer()
        }
        
        private fun allocateBuffer(): DirectByteBuffer {
            val byteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
            val directBuffer = DirectByteBuffer(byteBuffer, capacity)
            
            allBuffers.add(directBuffer)
            totalAllocatedBytes += capacity.toLong()
            
            Log.d(TAG, "Allocated new direct buffer: ${capacity / 1024}KB (pool: ${allBuffers.size})")
            
            return directBuffer
        }
        
        fun release(buffer: DirectByteBuffer) {
            if (buffer.capacity != capacity) return
            
            buffer.position(0)
            buffer.limit(capacity)
            
            if (!availableBuffers.trySend(buffer)) {
                Log.w(TAG, "Pool full, dropping buffer: ${capacity / 1024}KB")
            } else {
                currentPooledBytes += capacity.toLong()
            }
        }
        
        fun releaseDirect(byteBuffer: ByteBuffer) {
            val directBuffer = DirectByteBuffer(byteBuffer, capacity)
            release(directBuffer)
        }
        
        fun trim(targetReduction: Float) {
            val targetSize = (allBuffers.size * (1 - targetReduction)).toInt()
            val toRemove = allBuffers.size - targetSize
            
            repeat(toRemove) {
                availableBuffers.poll()?.let { buffer ->
                    allBuffers.remove(buffer)
                    buffer.cleaner?.clean()
                    totalAllocatedBytes -= capacity.toLong()
                    currentPooledBytes -= capacity.toLong()
                }
            }
        }
        
        fun clear() {
            availableBuffers.close()
            allBuffers.forEach { it.cleaner?.clean() }
            allBuffers.clear()
            totalAllocatedBytes = 0
            currentPooledBytes = 0
        }
    }
}

class DirectByteBuffer(
    val buffer: ByteBuffer,
    val capacity: Int
) {
    val cleaner: java.lang.ref.Cleaner.Cleanable? = if (buffer.isDirect) {
        java.lang.ref.Cleaner.create().register(this, java.lang.ref.Cleaner.create().register(this) { buffer.cleaner()?.clean() })
    } else null
    
    operator fun get(index: Int): Byte = buffer.get(index)
    operator fun set(index: Int, value: Byte) { buffer.put(index, value) }
    
    fun position(newPosition: Int): DirectByteBuffer {
        buffer.position(newPosition)
        return this
    }
    
    fun limit(newLimit: Int): DirectByteBuffer {
        buffer.limit(newLimit)
        return this
    }
    
    fun remaining(): Int = buffer.remaining()
    fun hasRemaining(): Boolean = buffer.hasRemaining()
    
    fun put(src: ByteBuffer): DirectByteBuffer {
        buffer.put(src)
        return this
    }
    
    fun put(src: ByteArray, offset: Int, length: Int): DirectByteBuffer {
        buffer.put(src, offset, length)
        return this
    }
    
    fun get(dst: ByteArray, offset: Int, length: Int): DirectByteBuffer {
        buffer.get(dst, offset, length)
        return this
    }
    
    fun asReadOnly(): ByteBuffer = buffer.asReadOnlyBuffer()
    
    fun slice(): ByteBuffer = buffer.slice()
    
    fun duplicate(): ByteBuffer = buffer.duplicate()
    
    companion object {
        fun wrap(array: ByteArray): DirectByteBuffer {
            return DirectByteBuffer(ByteBuffer.wrap(array), array.size)
        }
    }
}

data class PoolStats(
    val totalAllocatedBytes: Long = 0,
    val totalPooledBytes: Long = 0,
    val activePools: Int = 0,
    val hitRate: Float = 0f,
    val totalAcquires: Long = 0,
) {
    val totalAllocatedMb: Float get() = totalAllocatedBytes / 1024f / 1024f
    val totalPooledMb: Float get() = totalPooledBytes / 1024f / 1024f
}


@Singleton
class ZeroCopyDecoderOutput @Inject constructor(
    private val bufferPool: DirectByteBufferPool,
) {
    
    companion object {
        private const val TAG = "ZeroCopyDecoderOutput"
    }
    
    private val _frameOutputChannel = Channel<DecodedFrame>(64)
    val frameOutputChannel: ReceiveChannel<DecodedFrame> = _frameOutputChannel
    
    private val _outputStats = MutableStateFlow(OutputStats())
    val outputStats: StateFlow<OutputStats> = _outputStats.asStateFlow()
    
    private var currentCodec: MediaCodec? = null
    private var currentFormat: MediaFormat? = null
    private var frameIndex = 0L
    
    fun configure(codec: MediaCodec, format: MediaFormat) {
        currentCodec = codec
        currentFormat = format
        frameIndex = 0
    }
    
    fun processOutputBuffer(
        bufferIndex: Int,
        bufferInfo: MediaCodec.BufferInfo,
        releaseBuffer: Boolean = true
    ): DecodedFrame? {
        val codec = currentCodec ?: return null
        val format = currentFormat ?: return null
        
        val outputBuffer = codec.getOutputBuffer(bufferIndex)
            ?: return null
        
        val frame = acquireFrame(bufferIndex, bufferInfo, outputBuffer, format)
        
        if (frame != null) {
            _frameOutputChannel.trySend(frame)
            updateStats(frame)
        }
        
        if (releaseBuffer) {
            codec.releaseOutputBuffer(bufferIndex, false)
        }
        
        return frame
    }
    
    private fun acquireFrame(
        bufferIndex: Int,
        bufferInfo: MediaCodec.BufferInfo,
        sourceBuffer: ByteBuffer,
        format: MediaFormat
    ): DecodedFrame? {
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        val colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
        
        val directBuffer = bufferPool.acquireForFormat(format)
        
        val sourcePosition = sourceBuffer.position()
        val sourceLimit = sourceBuffer.limit()
        
        val dataSize = bufferInfo.size
        if (dataSize <= 0) {
            bufferPool.release(directBuffer)
            return null
        }
        
        sourceBuffer.limit(sourcePosition + dataSize)
        directBuffer.buffer.put(sourceBuffer)
        sourceBuffer.position(sourcePosition)
        sourceBuffer.limit(sourceLimit)
        
        directBuffer.limit(dataSize)
        directBuffer.position(0)
        
        return DecodedFrame(
            buffer = directBuffer,
            presentationTimeUs = bufferInfo.presentationTimeUs,
            bufferIndex = bufferIndex,
            frameIndex = frameIndex++,
            width = width,
            height = height,
            colorFormat = colorFormat,
            flags = bufferInfo.flags,
            offset = bufferInfo.offset,
            size = dataSize,
        )
    }
    
    fun processOutputFrame(
        frame: MediaCodec.OutputBufferInfo,
        sourceBuffer: ByteBuffer
    ): DecodedFrame? {
        val format = currentFormat ?: return null
        
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        val colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
        
        val directBuffer = bufferPool.acquireForFormat(format)
        
        val dataSize = frame.size
        if (dataSize <= 0) {
            bufferPool.release(directBuffer)
            return null
        }
        
        sourceBuffer.limit(frame.offset + dataSize)
        sourceBuffer.position(frame.offset)
        directBuffer.buffer.put(sourceBuffer)
        
        directBuffer.limit(dataSize)
        directBuffer.position(0)
        
        val decodedFrame = DecodedFrame(
            buffer = directBuffer,
            presentationTimeUs = frame.presentationTimeUs,
            bufferIndex = frame.index,
            frameIndex = frameIndex++,
            width = width,
            height = height,
            colorFormat = colorFormat,
            flags = frame.flags,
            offset = frame.offset,
            size = dataSize,
        )
        
        _frameOutputChannel.trySend(decodedFrame)
        updateStats(decodedFrame)
        
        return decodedFrame
    }
    
    private fun updateStats(frame: DecodedFrame) {
        val current = _outputStats.value
        _outputStats.value = OutputStats(
            framesOutput = current.framesOutput + 1,
            totalBytesOutput = current.totalBytesOutput + frame.size,
            currentFrameRate = calculateFrameRate(current.framesOutput + 1),
            lastFrameTimeUs = frame.presentationTimeUs,
        )
    }
    
    private fun calculateFrameRate(frames: Long): Float {
        // Simplified - in reality would track time window
        return 30f
    }
    
    fun releaseFrame(frame: DecodedFrame) {
        bufferPool.release(frame.buffer)
    }
    
    fun shutdown() {
        _frameOutputChannel.close()
        bufferPool.clearAll()
    }
    
    data class DecodedFrame(
        val buffer: DirectByteBuffer,
        val presentationTimeUs: Long,
        val bufferIndex: Int,
        val frameIndex: Long,
        val width: Int,
        val height: Int,
        val colorFormat: Int,
        val flags: Int,
        val offset: Int,
        val size: Int,
    ) {
        val yPlane: ByteBuffer
            get() = buffer.buffer.slice()
        
        val uPlane: ByteBuffer
            get() = buffer.buffer.slice()
        
        val vPlane: ByteBuffer
            get() = buffer.buffer.slice()
        
        fun copyTo(target: DirectByteBuffer) {
            val src = buffer.buffer.duplicate()
            val dst = target.buffer
            src.position(0)
            src.limit(size)
            dst.position(0)
            dst.put(src)
        }
    }
    
    data class OutputStats(
        val framesOutput: Long = 0,
        val totalBytesOutput: Long = 0,
        val currentFrameRate: Float = 0f,
        val lastFrameTimeUs: Long = 0,
    ) {
        val totalOutputMb: Float get() = totalBytesOutput / 1024f / 1024f
    }
}


@Singleton
class FramePoolManager @Inject constructor(
    private val bufferPool: DirectByteBufferPool,
    private val zeroCopyOutput: ZeroCopyDecoderOutput,
) {
    
    private val _framePoolStats = MutableStateFlow(FramePoolStats())
    val framePoolStats: StateFlow<FramePoolStats> = _framePoolStats.asStateFlow()
    
    private val framePools = mutableMapOf<Int, FramePool>()
    
    fun getOrCreatePool(format: MediaFormat): FramePool {
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        val key = (width shl 16) or height
        
        return framePools.getOrPut(key) {
            FramePool(width, height, format)
        }
    }
    
    fun acquireFrame(format: MediaFormat): PooledFrame? {
        val pool = getOrCreatePool(format)
        return pool.acquire()
    }
    
    fun releaseFrame(frame: PooledFrame) {
        val key = (frame.width shl 16) or frame.height
        framePools[key]?.release(frame)
    }
    
    fun trimAll(ratio: Float = 0.5f) {
        framePools.values.forEach { it.trim(ratio) }
        updateStats()
    }
    
    private fun updateStats() {
        var totalFrames = 0
        var availableFrames = 0
        var totalMemoryMb = 0f
        
        framePools.values.forEach { pool ->
            totalFrames += pool.totalFrames
            availableFrames += pool.availableFrames
            totalMemoryMb += pool.memoryUsageMb
        }
        
        _framePoolStats.value = FramePoolStats(
            totalFrames = totalFrames,
            availableFrames = availableFrames,
            totalMemoryMb = totalMemoryMb,
            activePools = framePools.size,
        )
    }
    
    inner class FramePool(
        val width: Int,
        val height: Int,
        private val format: MediaFormat,
    ) {
        private val frames = mutableListOf<PooledFrame>()
        private val availableFrames = Channel<PooledFrame>(16)
        
        var totalFrames = 0
        var createdFrames = 0
        
        init {
            preallocate(4)
        }
        
        fun acquire(): PooledFrame? {
            return availableFrames.poll()?.apply {
                isInUse = true
            } ?: createFrame()
        }
        
        private fun createFrame(): PooledFrame {
            val buffer = bufferPool.acquireForFormat(format)
            val frame = PooledFrame(
                buffer = buffer,
                width = width,
                height = height,
                colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT),
            )
            frames.add(frame)
            createdFrames++
            totalFrames++
            frame.isInUse = true
            return frame
        }
        
        fun release(frame: PooledFrame) {
            if (frame.width != width || frame.height != height) return
            
            frame.isInUse = false
            frame.buffer.position(0)
            frame.buffer.limit(frame.buffer.capacity)
            
            if (!availableFrames.trySend(frame)) {
                bufferPool.release(frame.buffer)
                frames.remove(frame)
                totalFrames--
            }
        }
        
        fun preallocate(count: Int) {
            repeat(count) {
                val frame = createFrame()
                frame.isInUse = false
                availableFrames.trySend(frame)
            }
        }
        
        fun trim(ratio: Float) {
            val targetSize = (frames.size * (1 - ratio)).toInt()
            val toRemove = frames.size - targetSize
            
            repeat(toRemove) {
                availableFrames.poll()?.let { frame ->
                    bufferPool.release(frame.buffer)
                    frames.remove(frame)
                    totalFrames--
                }
            }
        }
        
        val availableFramesCount: Int
            get() = availableFrames.size
        
        val memoryUsageMb: Float
            get() = frames.sumOf { it.buffer.capacity.toFloat() / 1024 / 1024 }
    }
    
    data class PooledFrame(
        val buffer: DirectByteBuffer,
        val width: Int,
        val height: Int,
        val colorFormat: Int,
        var isInUse: Boolean = false,
        var presentationTimeUs: Long = 0,
        var frameIndex: Long = 0,
    )
    
    data class FramePoolStats(
        val totalFrames: Int = 0,
        val availableFrames: Int = 0,
        val totalMemoryMb: Float = 0f,
        val activePools: Int = 0,
    )
}