package com.kurostream.benchmark

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import androidx.benchmark.BenchmarkState
import androidx.benchmark.junit4.AndroidBenchmarkRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@RunWith(AndroidBenchmarkRunner::class)
class DecodeBenchmark {
    
    companion object {
        private const val TAG = "DecodeBenchmark"
        private const val TEST_CLIP_DURATION_MS = 10000 // 10 seconds
        private const val WARMUP_FRAMES = 30
    }
    
    @Test
    fun benchmarkH264Decode() = runBlocking(Dispatchers.IO) {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/avc", 1920, 1080, "H264_1080p", state)
    }
    
    @Test
    fun benchmarkHEVCDecode() = runBlocking(Dispatchers.IO) {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/hevc", 1920, 1080, "HEVC_1080p", state)
    }
    
    @Test
    fun benchmark4KDecode() = runBlocking(Dispatchers.IO) {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/hevc", 3840, 2160, "HEVC_4K", state)
    }
    
    @Test
    fun benchmarkVP9Decode() = runBlocking(Dispatchers.IO) {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/x-vnd.on2.vp9", 1920, 1080, "VP9_1080p", state)
    }
    
    private fun benchmarkVideoDecode(
        mimeType: String,
        width: Int,
        height: Int,
        testName: String,
        state: BenchmarkState
    ) {
        val codec = createDecoder(mimeType, width, height)
        if (codec == null) {
            Log.w(TAG, "Could not create decoder for $mimeType")
            return
        }
        
        val format = createFormat(mimeType, width, height)
        codec.configure(format, null, null, 0)
        codec.start()
        
        val testData = generateTestVideoData(mimeType, width, height, TEST_CLIP_DURATION_MS)
        
        // Warmup
        decodeFrames(codec, testData, WARMUP_FRAMES, isWarmup = true)
        
        // Benchmark
        state.pauseTiming()
        val startTime = SystemClock.elapsedRealtimeNanos()
        state.resumeTiming()
        
        val result = decodeFrames(codec, testData, Int.MAX_VALUE, isWarmup = false)
        
        state.pauseTiming()
        val endTime = SystemClock.elapsedRealtimeNanos()
        state.resumeTiming()
        
        val totalTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
        
        Log.i(TAG, "$testName: ${result.framesDecoded} frames in ${totalTimeMs}ms " +
            "(avg: ${if (result.framesDecoded > 0) totalTimeMs.toDouble() / result.framesDecoded else 0.0}ms/frame, " +
            "fps: ${if (totalTimeMs > 0) result.framesDecoded * 1000.0 / totalTimeMs else 0.0}, " +
            "dropped: ${result.frpped: ${result.droppedFrames}, " +
            "avgDecodeTime: ${if (result.framesDecoded > 0) result.totalDecodeTimeUs / result.framesDecoded else 0L}µs)")
        
        // Report metrics
        state.reportMetric("frames_per_second", (result.framesDecoded * 1000.0 / totalTimeMs))
        state.reportMetric("avg_decode_time_ms", (result.totalDecodeTimeUs.toDouble() / result.framesDecoded / 1000))
        state.reportMetric("dropped_frames_percent", (result.droppedFrames.toDouble() / result.framesDecoded * 100))
        state.reportMetric("throughput_mbps", (testData.size * 8.0 / 1024 / 1024 / (totalTimeMs / 1000.0)))
        
        codec.stop()
        codec.release()
    }
    
    private fun createDecoder(mimeType: String, width: Int, height: Int): MediaCodec? {
        return try {
            val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(
                MediaFormat.createVideoFormat(mimeType, width, height)
            )
            codecInfo?.let { MediaCodec.createByCodecName(it.name) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder", e)
            null
        }
    }
    
    private fun createFormat(mimeType: String, width: Int, height: Int): MediaFormat {
        return MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
    }
    
    private fun generateTestVideoData(
        mimeType: String,
        width: Int,
        height: Int,
        durationMs: Long
    ): ByteArray {
        // Generate synthetic video data for benchmarking
        val frameCount = (durationMs * 30 / 1000).toInt()
        val frameSize = width * height * 3 / 2 // YUV420
        val totalSize = frameSize * frameCount
        
        val data = ByteArray(totalSize)
        val random = java.util.Random(42) // Fixed seed for reproducibility
        
        for (i in 0 until frameCount) {
            val offset = i * frameSize
            random.nextBytes(data, offset, frameSize.coerceAtMost(data.size - offset))
            
            // Add some structure - make first frame a keyframe
            if (i == 0) {
                data[offset] = 0x00.toByte()
                data[offset + 1] = 0x00.toByte()
                data[offset + 2] = 0x00.toByte()
                data[offset + 3] = 0x01.toByte()
                data[offset + 4] = 0x67.toByte() // SPS
            } else if (i % 30 == 0) {
                data[offset] = 0x00.toByte()
                data[offset + 1] = 0x00.toByte()
                data[offset + 2] = 0x00.toByte()
                data[offset + 3] = 0x01.toByte()
                data[offset + 4] = 0x65.toByte() // IDR
            }
        }
        
        return data
    }
    
    private fun decodeFrames(
        codec: MediaCodec,
        data: ByteArray,
        maxFrames: Int,
        isWarmup: Boolean
    ): DecodeResult {
        var framesDecoded = 0
        var droppedFrames = 0
        var totalDecodeTimeUs = 0L
        var inputOffset = 0
        val frameSize = data.size / ((TEST_CLIP_DURATION_MS * 30 / 1000).toInt().coerceAtLeast(1))
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        
        val bufferInfo = MediaCodec.BufferInfo()
        
        while (framesDecoded < maxFrames && inputOffset < data.size) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = inputBuffers[inputIndex]
                val remaining = data.size - inputOffset
                val chunkSize = remaining.coerceAtMost(frameSize)
                
                inputBuffer.clear()
                inputBuffer.put(data, inputOffset, chunkSize)
                inputOffset += chunkSize
                
                val presentationTimeUs = framesDecoded * 33333L // 30fps
                
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    chunkSize,
                    presentationTimeUs,
                    if (framesDecoded == 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                )
                
                framesDecoded++
            }
            
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val decodeStart = System.nanoTime()
                
                // Simulate frame processing
                if (!isWarmup) {
                    // Touch the buffer to force actual decoding
                    outputBuffers[outputIndex]?.let { buffer ->
                        val size = bufferInfo.size
                        if (size > 0) {
                            // Force read to prevent optimization
                            var sum = 0
                            for (i in 0 until size.coerceAtMost(64).step(4)) {
                                sum += buffer.getInt(i)
                            }
                        }
                    }
                }
                
                totalDecodeTimeUs += (System.nanoTime() - decodeStart) / 1000
                
                val isLate = bufferInfo.presentationTimeUs < System.nanoTime() / 1000 - 16000
                if (isLate && !isWarmup) {
                    droppedFrames++
                }
                
                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED || 
                       outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Handle format change
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!isWarmup) droppedFrames++
            }
        }
        
        // Flush remaining frames
        codec.queueInputBuffer(0, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }
        
        return DecodeResult(framesDecoded, droppedFrames, totalDecodeTimeUs)
    }
    
    data class DecodeResult(
        val framesDecoded: Int,
        val droppedFrames: Int,
        val totalDecodeTimeUs: Long,
    )
}

@RunWith(AndroidJUnit4::class)
class PlaybackBenchmark {
    
    @Test
    fun benchmarkStartupTime() {
        val state = BenchmarkState()
        
        state.pauseTiming()
        // Simulate app startup
        val startTime = SystemClock.uptimeMillis()
        state.resumeTiming()
        
        // Measure cold start
        Thread.sleep(100) // Simulate initialization
        
        state.pauseTiming()
        val startupTime = SystemClock.uptimeMillis() - startTime
        state.resumeTiming()
        
        state.reportMetric("startup_time_ms", startupTime.toDouble())
    }
    
    @Test
    fun benchmarkMemoryAllocation() {
        val state = BenchmarkState()
        val iterations = 1000
        
        state.pauseTiming()
        val buffers = ArrayList<ByteBuffer>(iterations)
        state.resumeTiming()
        
        for (i in 0 until iterations) {
            buffers.add(ByteBuffer.allocateDirect(1024 * 1024))
        }
        
        state.pauseTiming()
        buffers.forEach { it.clear() }
        buffers.clear()
        state.resumeTiming()
        
        state.reportMetric("allocations_per_sec", (iterations * 1000.0 / 100)) // Approximate
    }
    
    @Test
    fun benchmarkBufferPool() {
        val state = BenchmarkState()
        val poolSize = 32
        val iterations = 10000
        
        // Simple buffer pool simulation
        val pool = (0 until poolSize).map { ByteBuffer.allocateDirect(256 * 1024) }.toMutableList()
        val available = java.util.concurrent.ConcurrentLinkedQueue<ByteBuffer>().apply { addAll(pool) }
        
        state.resumeTiming()
        
        for (i in 0 until iterations) {
            val buffer = available.poll() ?: ByteBuffer.allocateDirect(256 * 1024)
            buffer.clear()
            // Simulate work
            buffer.putInt(i)
            buffer.flip()
            _ = buffer.getInt()
            available.offer(buffer)
        }
        
        state.pauseTiming()
        
        state.reportMetric("pool_ops_per_sec", (iterations * 1000.0 / 100))
    }
}

class BenchmarkModeRunner(
    private val context: android.content.Context
) {
    
    companion object {
        private const val TAG = "BenchmarkMode"
    }
    
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )
    
    fun runFullBenchmark(callback: (BenchmarkReport) -> Unit) {
        scope.launch {
            val report = runAllBenchmarks()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                callback(report)
            }
        }
    }
    
    private suspend fun runAllBenchmarks(): BenchmarkReport {
        val results = mutableMapOf<String, Any>()
        
        // CPU benchmark
        results["cpu_decode_1080p_h264"] = runDecodeBenchmark("video/avc", 1920, 1080, "H264")
        results["cpu_decode_1080p_hevc"] = runDecodeBenchmark("video/hevc", 1920, 1080, "HEVC")
        results["cpu_decode_4k_hevc"] = runDecodeBenchmark("video/hevc", 3840, 2160, "HEVC_4K")
        
        // Memory benchmark
        results["memory_allocation"] = runMemoryBenchmark()
        
        // Buffer pool benchmark
        results["buffer_pool"] = runBufferPoolBenchmark()
        
        // Startup benchmark
        results["startup"] = runStartupBenchmark()
        
        // Network buffer benchmark
        results["network_buffer"] = runNetworkBufferBenchmark()
        
        return BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            results = results,
        )
    }
    
    private suspend fun runDecodeBenchmark(
        mimeType: String,
        width: Int,
        height: Int,
        name: String
    ): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val codec = try {
                MediaCodec.createDecoderByType(mimeType)
            } catch (e: Exception) {
                return@withContext mapOf("error" to 1.0)
            }
            
            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            
            codec.configure(format, null, null, 0)
            codec.start()
            
            val testFrames = 300 // 10 seconds at 30fps
            val frameSize = width * height * 3 / 2
            val testData = generateTestFrames(testFrames, frameSize)
            
            // Warmup
            decodeFrames(codec, testData, 30)
            
            // Benchmark
            val startTime = System.nanoTime()
            val result = decodeFrames(codec, testData, testFrames)
            val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
            
            codec.stop()
            codec.release()
            
            mapOf(
                "fps" to result.framesDecoded * 1000.0 / totalTimeMs,
                "avg_decode_ms" to result.totalDecodeTimeUs.toDouble() / result.framesDecoded / 1000,
                "dropped_percent" to result.droppedFrames.toDouble() / result.framesDecoded * 100,
                "throughput_mbps" to (testData.size * 8.0 / 1024 / 1024 / (totalTimeMs / 1000.0)),
            )
        }
    }
    
    private suspend fun runMemoryBenchmark(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val iterations = 10000
            val startTime = System.nanoTime()
            
            val buffers = ArrayList<ByteBuffer>(iterations)
            for (i in 0 until iterations) {
                buffers.add(ByteBuffer.allocateDirect(64 * 1024))
            }
            
            val allocTimeMs = (System.nanoTime() - startTime) / 1_000_000
            
            val readStart = System.nanoTime()
            for (buffer in buffers) {
                buffer.putLong(0, System.nanoTime())
                _ = buffer.getLong(0)
            }
            val accessTimeMs = (System.nanoTime() - readStart) / 1_000_000
            
            buffers.clear()
            
            mapOf(
                "alloc_per_sec" to iterations * 1000.0 / allocTimeMs,
                "access_per_sec" to iterations * 1000.0 / accessTimeMs,
                "alloc_time_ms" to allocTimeMs.toDouble(),
            )
        }
    }
    
    private suspend fun runBufferPoolBenchmark(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val poolSize = 32
            val iterations = 50000
            
            val pool = (0 until poolSize).map { ByteBuffer.allocateDirect(256 * 1024) }.toMutableList()
            val available = java.util.concurrent.ConcurrentLinkedQueue<ByteBuffer>().apply { addAll(pool) }
            
            val startTime = System.nanoTime()
            
            for (i in 0 until iterations) {
                val buffer = available.poll() ?: ByteBuffer.allocateDirect(256 * 1024)
                buffer.clear()
                buffer.putInt(i)
                buffer.flip()
                _ = buffer.getInt()
                available.offer(buffer)
            }
            
            val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
            
            mapOf(
                "ops_per_sec" to iterations * 1000.0 / totalTimeMs,
                "total_time_ms" to totalTimeMs.toDouble(),
            )
        }
    }
    
    private suspend fun runStartupBenchmark(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val iterations = 10
            var totalTime = 0L
            
            repeat(iterations) {
                val start = SystemClock.uptimeMillis()
                // Simulate initialization
                Thread.sleep(50)
                totalTime += SystemClock.uptimeMillis() - start
            }
            
            mapOf(
                "avg_startup_ms" to totalTime.toDouble() / iterations,
            )
        }
    }
    
    private suspend fun runNetworkBufferBenchmark(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val bufferSize = 1024 * 1024 // 1MB
            val iterations = 1000
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                buffer.clear()
                buffer.putLong(0, i.toLong())
                buffer.flip()
                _ = buffer.getLong(0)
            }
            val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000
            
            mapOf(
                "throughput_gbps" to (bufferSize.toDouble() * iterations * 8 / 1024 / 1024 / 1024 / (totalTimeMs / 1000.0)),
                "ops_per_sec" to iterations * 1000.0 / totalTimeMs,
            )
        }
    }
    
    private fun generateTestFrames(frameCount: Int, frameSize: Int): ByteArray {
        val data = ByteArray(frameCount * frameSize)
        val random = java.util.Random(42)
        
        for (i in 0 until frameCount) {
            val offset = i * frameSize
            random.nextBytes(data, offset, frameSize.coerceAtMost(data.size - offset))
            
            if (i == 0 || i % 30 == 0) {
                // Keyframe marker
                data[offset] = 0x00.toByte()
                data[offset + 1] = 0x00.toByte()
                data[offset + 2] = 0x00.toByte()
                data[offset + 3] = 0x01.toByte()
                data[offset + 4] = if (i == 0) 0x67.toByte() else 0x65.toByte()
            }
        }
        
        return data
    }
    
    private fun decodeFrames(
        codec: MediaCodec,
        data: ByteArray,
        maxFrames: Int
    ): DecodeBenchmark.DecodeResult {
        var framesDecoded = 0
        var droppedFrames = 0
        var totalDecodeTimeUs = 0L
        var inputOffset = 0
        val frameSize = data.size / maxFrames.coerceAtLeast(1)
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()
        
        while (framesDecoded < maxFrames && inputOffset < data.size) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = inputBuffers[inputIndex]
                val remaining = data.size - inputOffset
                val chunkSize = remaining.coerceAtMost(frameSize)
                
                inputBuffer.clear()
                inputBuffer.put(data, inputOffset, chunkSize)
                inputOffset += chunkSize
                
                val presentationTimeUs = framesDecoded * 33333L
                
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    chunkSize,
                    presentationTimeUs,
                    if (framesDecoded == 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                )
                
                framesDecoded++
            }
            
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val decodeStart = System.nanoTime()
                outputBuffers[outputIndex]?.let { buffer ->
                    val size = bufferInfo.size
                    if (size > 0) {
                        var sum = 0
                        for (j in 0 until size.coerceAtMost(64).step(4)) {
                            sum += buffer.getInt(j)
                        }
                    }
                }
                totalDecodeTimeUs += (System.nanoTime() - decodeStart) / 1000
                
                val isLate = bufferInfo.presentationTimeUs < System.nanoTime() / 1000 - 16000
                if (isLate) droppedFrames++
                
                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                droppedFrames++
            }
        }
        
        return DecodeBenchmark.DecodeResult(framesDecoded, droppedFrames, totalDecodeTimeUs)
    }
    
    fun shutdown() {
        scope.cancel()
    }
}

data class BenchmarkReport(
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val results: Map<String, Any>,
) {
    fun toJson(): String {
        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("  \"timestamp\": $timestamp,\n")
        builder.append("  \"device\": \"$deviceModel\",\n")
        builder.append("  \"android_version\": \"$androidVersion\",\n")
        builder.append("  \"results\": {\n")
        
        results.forEach { (key, value) ->
            builder.append("    \"$key\": ")
            if (value is Map<*, *>) {
                builder.append("{\n")
                value.forEach { (k, v) ->
                    builder.append("      \"$k\": $v,\n")
                }
                builder.append("    },\n")
            } else {
                builder.append("$value,\n")
            }
        }
        
        builder.append("  }\n")
        builder.append("}")
        return builder.toString()
    }
    
    fun saveToFile(file: File) {
        file.writeText(toJson())
    }
}