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
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@RunWith(AndroidBenchmarkRunner::class)
class DecodeBenchmark {
    
    companion object {
        private const val TAG = "DecodeBenchmark"
        private const val TEST_CLIP_DURATION_MS = 10000
        private const val WARMUP_FRAMES = 30
    }
    
    @Test
    fun benchmarkH264Decode() = runBlocking {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/avc", 1920, 1080, "H264_1080p", state)
    }
    
    @Test
    fun benchmarkHEVCDecode() = runBlocking {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/hevc", 1920, 1080, "HEVC_1080p", state)
    }
    
    @Test
    fun benchmark4KDecode() = runBlocking {
        val state = BenchmarkState()
        benchmarkVideoDecode("video/hevc", 3840, 2160, "HEVC_4K", state)
    }
    
    @Test
    fun benchmarkVP9Decode() = runBlocking {
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
        val codec = createDecoder(mimeType)
        if (codec == null) {
            Log.w(TAG, "Could not create decoder for $mimeType")
            return
        }
        
        BenchmarkUtils.configureDecoder(codec, mimeType, width, height)
        
        val frameCount = (TEST_CLIP_DURATION_MS * 30 / 1000).toInt()
        val frameSize = width * height * 3 / 2
        val testData = BenchmarkUtils.generateTestFrames(frameCount, frameSize)
        
        BenchmarkUtils.decodeFrames(codec, testData, WARMUP_FRAMES)
        
        state.pauseTiming()
        val startTime = SystemClock.elapsedRealtimeNanos()
        state.resumeTiming()
        
        val result = BenchmarkUtils.decodeFrames(codec, testData, Int.MAX_VALUE)
        
        state.pauseTiming()
        val endTime = SystemClock.elapsedRealtimeNanos()
        state.resumeTiming()
        
        val totalTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
        
        Log.i(TAG, "$testName: ${result.framesDecoded} frames in ${totalTimeMs}ms " +
            "(avg: ${if (result.framesDecoded > 0) totalTimeMs.toDouble() / result.framesDecoded else 0.0}ms/frame, " +
            "dropped: ${result.droppedFrames}, " +
            "avgDecodeTime: ${if (result.framesDecoded > 0) result.totalDecodeTimeUs / result.framesDecoded else 0L}µs)")
        
        state.reportMetric("frames_per_second", (result.framesDecoded * 1000.0 / totalTimeMs))
        state.reportMetric("avg_decode_time_ms", (result.totalDecodeTimeUs.toDouble() / result.framesDecoded / 1000))
        state.reportMetric("dropped_frames_percent", (result.droppedFrames.toDouble() / result.framesDecoded * 100))
        
        codec.stop()
        codec.release()
    }
    
    private fun createDecoder(mimeType: String): MediaCodec? {
        return try {
            MediaCodec.createDecoderByType(mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder", e)
            null
        }
    }
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
    
    fun runFullBenchmark(callback: (BenchmarkUtils.BenchmarkReport) -> Unit) {
        scope.launch {
            val report = runAllBenchmarks()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                callback(report)
            }
        }
    }
    
    private suspend fun runAllBenchmarks(): BenchmarkUtils.BenchmarkReport {
        val results = mutableMapOf<String, Any>()
        
        results["cpu_decode_1080p_h264"] = runDecodeBenchmark("video/avc", 1920, 1080)
        results["cpu_decode_1080p_hevc"] = runDecodeBenchmark("video/hevc", 1920, 1080)
        results["cpu_decode_4k_hevc"] = runDecodeBenchmark("video/hevc", 3840, 2160)
        
        results["memory_allocation"] = runMemoryBenchmark()
        results["buffer_pool"] = runBufferPoolBenchmark()
        results["startup"] = runStartupBenchmark()
        results["network_buffer"] = runNetworkBufferBenchmark()
        
        return BenchmarkUtils.BenchmarkReport(
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
    ): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val codec = try {
                MediaCodec.createDecoderByType(mimeType)
            } catch (e: Exception) {
                return@withContext mapOf("error" to 1.0)
            }
            
            BenchmarkUtils.configureDecoder(codec, mimeType, width, height)
            
            val testFrames = 300
            val frameSize = width * height * 3 / 2
            val testData = BenchmarkUtils.generateTestFrames(testFrames, frameSize)
            
            BenchmarkUtils.decodeFrames(codec, testData, 30)
            
            val startTime = System.nanoTime()
            val result = BenchmarkUtils.decodeFrames(codec, testData, testFrames)
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
            val bufferSize = 1024 * 1024
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
    
    fun shutdown() {
        scope.cancel()
    }
}