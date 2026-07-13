package com.kurostream.players.engine

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.kurostream.players.buffer.DirectByteBuffer
import com.kurostream.players.buffer.DirectByteBufferPool
import com.kurostream.players.buffer.ZeroCopyDecoderOutput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroCopyFrameProcessor @Inject constructor(
    private val bufferPool: DirectByteBufferPool,
    private val decoderOutput: ZeroCopyDecoderOutput,
) {
    
    companion object {
        private const val TAG = "ZeroCopyFrameProcessor"
        private const val MAX_PENDING_FRAMES = 64
    }
    
    private val _frameProcessorStats = MutableStateFlow(FrameProcessorStats())
    val frameProcessorStats: StateFlow<FrameProcessorStats> = _frameProcessorStats.asStateFlow()
    
    private val frameProcessingChannel = Channel<FrameProcessingTask>(MAX_PENDING_FRAMES)
    private val outputFramesChannel = Channel<ProcessedFrame>(MAX_PENDING_FRAMES)
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
    )
    
    private var isProcessing = false
    private var currentFormat: MediaFormat? = null
    private var frameCount = 0L
    private var droppedFrames = 0L
    private var totalProcessingTimeUs = 0L
    
    init {
        startProcessing()
    }
    
    fun configure(format: MediaFormat) {
        currentFormat = format
        Log.d(TAG, "Configured for format: ${format.toString()}")
    }
    
    fun processFrame(
        inputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTimeUs: Long
    ): Boolean {
        if (!isProcessing) return false
        
        val frame = acquireFrame(bufferInfo.size)
        if (frame == null) {
            droppedFrames++
            updateStats()
            return false
        }
        
        val task = FrameProcessingTask(
            frame = frame,
            bufferInfo = bufferInfo,
            presentationTimeUs = presentationTimeUs,
            inputBuffer = inputBuffer,
        )
        
        return frameProcessingChannel.trySend(task)
    }
    
    fun processFrameZeroCopy(
        codec: MediaCodec,
        bufferIndex: Int,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTimeUs: Long
    ): Boolean {
        if (!isProcessing) return false
        
        val outputBuffer = codec.getOutputBuffer(bufferIndex)
        if (outputBuffer == null) {
            droppedFrames++
            updateStats()
            return false
        }
        
        val frame = bufferPool.acquireForFormat(currentFormat!!)
        
        val task = FrameProcessingTask(
            frame = frame,
            bufferInfo = bufferInfo,
            presentationTimeUs = presentationTimeUs,
            inputBuffer = outputBuffer,
            isZeroCopy = true,
            bufferIndex = bufferIndex,
            codec = codec,
        )
        
        return frameProcessingChannel.trySend(task)
    }
    
    private fun acquireFrame(size: Int): DirectByteBuffer? {
        return try {
            bufferPool.acquire(size)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire frame buffer", e)
            null
        }
    }
    
    private fun startProcessing() {
        isProcessing = true
        scope.launch {
            while (isProcessing) {
                val task = frameProcessingChannel.receiveOrNull()
                if (task == null) break
                
                processTask(task)
            }
        }
    }
    
    private fun processTask(task: FrameProcessingTask) {
        val startTime = System.nanoTime()
        
        try {
            if (task.isZeroCopy && task.codec != null && task.bufferIndex != null) {
                processZeroCopyFrame(task)
            } else {
                processCopyFrame(task)
            }
            
            frameCount++
            totalProcessingTimeUs += (System.nanoTime() - startTime) / 1000
            
            updateStats()
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed", e)
            droppedFrames++
            bufferPool.release(task.frame)
            updateStats()
        }
    }
    
    private fun processZeroCopyFrame(task: FrameProcessingTask) {
        val frame = task.frame
        val inputBuffer = task.inputBuffer!!
        val bufferInfo = task.bufferInfo
        
        frame.position(0)
        frame.limit(bufferInfo.size)
        
        inputBuffer.position(bufferInfo.offset)
        inputBuffer.limit(bufferInfo.offset + bufferInfo.size)
        
        frame.put(inputBuffer)
        
        frame.position(0)
        frame.limit(bufferInfo.size)
        
        val processedFrame = ProcessedFrame(
            buffer = frame,
            presentationTimeUs = task.presentationTimeUs,
            bufferInfo = bufferInfo,
            frameNumber = frameCount,
            isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
            isEndOfStream = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0,
        )
        
        if (!outputFramesChannel.trySend(processedFrame)) {
            Log.w(TAG, "Output channel full, dropping frame")
            droppedFrames++
            bufferPool.release(frame)
        }
        
        task.codec!!.releaseOutputBuffer(task.bufferIndex!!, false)
    }
    
    private fun processCopyFrame(task: FrameProcessingTask) {
        val frame = task.frame
        val inputBuffer = task.inputBuffer
        val bufferInfo = task.bufferInfo
        
        frame.position(0)
        frame.limit(bufferInfo.size)
        
        inputBuffer.position(bufferInfo.offset)
        inputBuffer.limit(bufferInfo.offset + bufferInfo.size)
        
        frame.put(inputBuffer)
        
        frame.position(0)
        frame.limit(bufferInfo.size)
        
        val processedFrame = ProcessedFrame(
            buffer = frame,
            presentationTimeUs = task.presentationTimeUs,
            bufferInfo = bufferInfo,
            frameNumber = frameCount,
            isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
            isEndOfStream = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0,
        )
        
        if (!outputFramesChannel.trySend(processedFrame)) {
            Log.w(TAG, "Output channel full, dropping frame")
            droppedFrames++
            bufferPool.release(frame)
        }
    }
    
    val processedFrames: ReceiveChannel<ProcessedFrame> = outputFramesChannel
    
    fun sendToDecoder(frame: ProcessedFrame) {
        decoderOutput.sendFrame(frame)
    }
    
    fun getStats(): FrameProcessorStats {
        return _frameProcessorStats.value
    }
    
    private fun updateStats() {
        _frameProcessorStats.value = FrameProcessorStats(
            framesProcessed = frameCount,
            framesDropped = droppedFrames,
            avgProcessingTimeUs = if (frameCount > 0) totalProcessingTimeUs / frameCount else 0,
            pendingInputTasks = frameProcessingChannel.size,
            pendingOutputFrames = outputFramesChannel.size,
            poolStats = bufferPool.poolStats.value,
        )
    }
    
    fun shutdown() {
        isProcessing = false
        frameProcessingChannel.close()
        outputFramesChannel.close()
        scope.cancel()
        
        bufferPool.clearAll()
        
        Log.d(TAG, "ZeroCopyFrameProcessor shutdown")
    }
    
    data class FrameProcessingTask(
        val frame: DirectByteBuffer,
        val bufferInfo: MediaCodec.BufferInfo,
        val presentationTimeUs: Long,
        val inputBuffer: ByteBuffer,
        val isZeroCopy: Boolean = false,
        val bufferIndex: Int? = null,
        val codec: MediaCodec? = null,
    )
    
    data class ProcessedFrame(
        val buffer: DirectByteBuffer,
        val presentationTimeUs: Long,
        val bufferInfo: MediaCodec.BufferInfo,
        val frameNumber: Long,
        val isKeyFrame: Boolean,
        val isEndOfStream: Boolean,
    ) {
        fun release() {
            bufferPool.release(buffer)
        }
    }
    
    data class FrameProcessorStats(
        val framesProcessed: Long = 0,
        val framesDropped: Long = 0,
        val avgProcessingTimeUs: Long = 0,
        val pendingInputTasks: Int = 0,
        val pendingOutputFrames: Int = 0,
        val poolStats: com.kurostream.players.buffer.PoolStats = com.kurostream.players.buffer.PoolStats(),
    ) {
        val dropRate: Float get() = if (framesProcessed + framesDropped > 0) {
            framesDropped.toFloat() / (framesProcessed + framesDropped)
        } else 0f
        
        val throughputFps: Float get() = if (avgProcessingTimeUs > 0) {
            1_000_000f / avgProcessingTimeUs
        } else 0f
    }
}

@Singleton
class ZeroCopyRendererOutput @Inject constructor(
    private val bufferPool: DirectByteBufferPool,
) {
    
    companion object {
        private const val TAG = "ZeroCopyRendererOutput"
    }
    
    private val _renderStats = MutableStateFlow(RenderStats())
    val renderStats: StateFlow<RenderStats> = _renderStats.asStateFlow()
    
    private val pendingFrames = java.util.concurrent.ConcurrentLinkedQueue<ZeroCopyRendererOutput.RenderFrame>()
    private var currentFrame: RenderFrame? = null
    private var frameQueueSize = 4
    
    fun queueFrame(
        buffer: DirectByteBuffer,
        presentationTimeUs: Long,
        isKeyFrame: Boolean = false,
    ) {
        if (pendingFrames.size >= frameQueueSize) {
            val dropped = pendingFrames.poll()
            dropped?.buffer?.let { bufferPool.release(it) }
        }
        
        pendingFrames.add(RenderFrame(buffer, presentationTimeUs, isKeyFrame))
    }
    
    fun dequeueFrame(): RenderFrame? {
        currentFrame?.let { bufferPool.release(it.buffer) }
        currentFrame = pendingFrames.poll()
        return currentFrame
    }
    
    fun getCurrentFrame(): RenderFrame? = currentFrame
    
    fun clear() {
        pendingFrames.forEach { bufferPool.release(it.buffer) }
        pendingFrames.clear()
        currentFrame?.let { bufferPool.release(it.buffer) }
        currentFrame = null
    }
    
    fun setQueueSize(size: Int) {
        frameQueueSize = size.coerceAtLeast(1).coerceAtMost(16)
    }
    
    data class RenderFrame(
        val buffer: DirectByteBuffer,
        val presentationTimeUs: Long,
        val isKeyFrame: Boolean,
    )
    
    data class RenderStats(
        val queuedFrames: Int = 0,
        val droppedFrames: Long = 0,
        val currentQueueSize: Int = 0,
    )
}

@Singleton
class DirectByteBufferFramePool @Inject constructor(
    private val bufferPool: DirectByteBufferPool,
) {
    
    companion object {
        private const val TAG = "DirectByteBufferFramePool"
    }
    
    private val videoFramePools = mutableMapOf<FrameSpec, FramePool>()
    private val audioFramePools = mutableMapOf<AudioFrameSpec, FramePool>()
    
    fun acquireVideoFrame(width: Int, height: Int, colorFormat: Int): DirectByteBuffer {
        val spec = FrameSpec(width, height, colorFormat)
        return videoFramePools.getOrPut(spec) { FramePool(spec.capacity) }.acquire()
    }
    
    fun acquireAudioFrame(sampleRate: Int, channelCount: Int, sampleFormat: Int, frameSize: Int): DirectByteBuffer {
        val spec = AudioFrameSpec(sampleRate, channelCount, sampleFormat, frameSize)
        return audioFramePools.getOrPut(spec) { FramePool(spec.capacity) }.acquire()
    }
    
    fun releaseVideoFrame(buffer: DirectByteBuffer, width: Int, height: Int, colorFormat: Int) {
        val spec = FrameSpec(width, height, colorFormat)
        videoFramePools[spec]?.release(buffer)
    }
    
    fun releaseAudioFrame(buffer: DirectByteBuffer, sampleRate: Int, channelCount: Int, sampleFormat: Int, frameSize: Int) {
        val spec = AudioFrameSpec(sampleRate, channelCount, sampleFormat, frameSize)
        audioFramePools[spec]?.release(buffer)
    }
    
    fun trimAll(targetReduction: Float = 0.5f) {
        videoFramePools.values.forEach { it.trim(targetReduction) }
        audioFramePools.values.forEach { it.trim(targetReduction) }
    }
    
    fun getStats(): FramePoolStats {
        var totalVideoFrames = 0
        var totalAudioFrames = 0
        var totalAllocatedMb = 0f
        
        videoFramePools.forEach { (spec, pool) ->
            totalVideoFrames += pool.size
            totalAllocatedMb += spec.capacity / 1024f / 1024f * pool.size
        }
        
        audioFramePools.forEach { (spec, pool) ->
            totalAudioFrames += pool.size
            totalAllocatedMb += spec.capacity / 1024f / 1024f * pool.size
        }
        
        return FramePoolStats(
            videoFramePools = videoFramePools.size,
            audioFramePools = audioFramePools.size,
            totalVideoFrames = totalVideoFrames,
            totalAudioFrames = totalAudioFrames,
            totalAllocatedMb = totalAllocatedMb,
        )
    }
    
    data class FrameSpec(
        val width: Int,
        val height: Int,
        val colorFormat: Int,
    ) {
        val capacity: Int
            get() = calculateCapacity(width, height, colorFormat)
    }
    
    data class AudioFrameSpec(
        val sampleRate: Int,
        val channelCount: Int,
        val sampleFormat: Int,
        val frameSize: Int,
    ) {
        val capacity: Int
            get() = frameSize * channelCount * (if (sampleFormat == 2) 4 else 2)
    }
    
    private fun calculateCapacity(width: Int, height: Int, colorFormat: Int): Int {
        return when (colorFormat) {
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible -> {
                val ySize = width * height
                val uvSize = (width / 2) * (height / 2) * 2
                ySize + uvSize
            }
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar,
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar -> {
                val alignedWidth = (width + 127) and (-128)
                val alignedHeight = (height + 15) and (-16)
                alignedWidth * alignedHeight * 3 / 2
            }
            else -> width * height * 3 / 2
        }
    }
    
    private class FramePool(private val capacity: Int) {
        private val available = java.util.concurrent.ConcurrentLinkedQueue<DirectByteBuffer>()
        private val allBuffers = mutableListOf<DirectByteBuffer>()
        private var maxSize = 8
        
        fun acquire(): DirectByteBuffer {
            return available.poll() ?: allocateNew()
        }
        
        private fun allocateNew(): DirectByteBuffer {
            val buffer = java.nio.ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
            val directBuffer = DirectByteBuffer(buffer, capacity)
            allBuffers.add(directBuffer)
            return directBuffer
        }
        
        fun release(buffer: DirectByteBuffer) {
            if (buffer.capacity != capacity) return
            
            buffer.position(0)
            buffer.limit(capacity)
            
            if (available.size < maxSize) {
                available.add(buffer)
            }
        }
        
        fun trim(targetReduction: Float) {
            val targetSize = (allBuffers.size * (1 - targetReduction)).toInt()
            val toRemove = allBuffers.size - targetSize
            
            repeat(toRemove) {
                available.poll()?.let { buffer ->
                    allBuffers.remove(buffer)
                    buffer.cleaner?.clean()
                }
            }
        }
        
        val size: Int get() = allBuffers.size
    }
    
    data class FramePoolStats(
        val videoFramePools: Int,
        val audioFramePools: Int,
        val totalVideoFrames: Int,
        val totalAudioFrames: Int,
        val totalAllocatedMb: Float,
    )
}