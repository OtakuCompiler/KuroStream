package com.kurostream.benchmark

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.junit4.BenchmarkRunner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkRunner {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun runDecodeBenchmarks() = runBlocking(Dispatchers.IO) {
        val results = mutableMapOf<String, Any>()
        
        val configs = listOf(
            DecodeConfig("video/avc", 1920, 1080, "H264_1080p"),
            DecodeConfig("video/hevc", 1920, 1080, "HEVC_1080p"),
            DecodeConfig("video/hevc", 3840, 2160, "HEVC_4K"),
            DecodeConfig("video/x-vnd.on2.vp9", 1920, 1080, "VP9_1080p"),
        )
        
        for (config in configs) {
            val result = runDecodeBenchmark(config)
            results[config.name] = mapOf(
                "frames_decoded" to result.framesDecoded,
                "dropped_frames" to result.droppedFrames,
                "avg_decode_time_us" to if (result.framesDecoded > 0) result.totalDecodeTimeUs / result.framesDecoded else 0L,
                "decode_fps" to if (result.totalDecodeTimeUs > 0) result.framesDecoded * 1_000_000.0 / result.totalDecodeTimeUs else 0.0,
                "dropped_rate_percent" to if (result.framesDecoded + result.droppedFrames > 0) 
                    (result.droppedFrames * 100.0 / (result.framesDecoded + result.droppedFrames)) else 0.0,
            )
        }
        
        val report = BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            results = results,
        )
        
        Log.i("BenchmarkRunner", "Benchmark Report:\n${report.toJson()}")
        
        // Save to file
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.filesDir, "benchmark_report_${System.currentTimeMillis()}.json")
        file.writeText(report.toJson())
        Log.i("BenchmarkRunner", "Report saved to: ${file.absolutePath}")
    }
    
    @Test
    fun runMemoryBenchmark() = runBlocking(Dispatchers.IO) {
        val result = runMemoryBenchmark()
        
        val report = BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            results = mapOf(
                "memory_benchmark" to mapOf(
                    "allocation_rate_mb_s" to result.allocationRateMbPerSec,
                    "gc_count" to result.gcCount,
                    "gc_total_time_ms" to result.gcTotalTimeMs,
                    "avg_gc_time_ms" to if (result.gcCount > 0) result.gcTotalTimeMs / result.gcCount else 0L,
                    "peak_memory_mb" to result.peakMemoryMb,
                    "final_memory_mb" to result.finalMemoryMb,
                )
            ),
        )
        
        Log.i("BenchmarkRunner", "Memory Benchmark:\n${report.toJson()}")
    }
    
    @Test
    fun runThermalBenchmark() = runBlocking(Dispatchers.IO) {
        val result = runThermalBenchmark()
        
        val report = BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            results = mapOf(
                "thermal_benchmark" to mapOf(
                    "initial_temp_c" to result.initialTempC,
                    "peak_temp_c" to result.peakTempC,
                    "thermal_throttle_events" to result.throttleEvents,
                    "time_to_throttle_ms" to result.timeToThrottleMs,
                    "sustained_performance_percent" to result.sustainedPerformancePercent,
                )
            ),
        )
        
        Log.i("BenchmarkRunner", "Thermal Benchmark:\n${report.toJson()}")
    }
}

data class DecodeConfig(
    val mimeType: String,
    val width: Int,
    val height: Int,
    val name: String,
)

data class DecodeResult(
    val framesDecoded: Long,
    val droppedFrames: Long,
    val totalDecodeTimeUs: Long,
)

data class MemoryBenchmarkResult(
    val allocationRateMbPerSec: Double,
    val gcCount: Int,
    val gcTotalTimeMs: Long,
    val peakMemoryMb: Long,
    val finalMemoryMb: Long,
)

data class ThermalBenchmarkResult(
    val initialTempC: Float,
    val peakTempC: Float,
    val throttleEvents: Int,
    val timeToThrottleMs: Long,
    val sustainedPerformancePercent: Float,
)

data class BenchmarkReport(
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val results: Map<String, Any>,
) {
    fun toJson(): String {
        val json = Json { prettyPrint = true }
        return json.encodeToString(this)
    }
}

object BenchmarkExecutor {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("KuroStream Benchmark Mode")
        println("Device: ${android.os.Build.MODEL}")
        println("Android: ${android.os.Build.VERSION.RELEASE}")
        println("Starting benchmarks...")
        
        runBlocking(Dispatchers.IO) {
            runAllBenchmarks()
        }
    }
    
    private fun runAllBenchmarks() {
        println("\n=== VIDEO DECODE BENCHMARKS ===")
        val decodeConfigs = listOf(
            DecodeConfig("video/avc", 1920, 1080, "H264_1080p"),
            DecodeConfig("video/hevc", 1920, 1080, "HEVC_1080p"),
            DecodeConfig("video/hevc", 3840, 2160, "HEVC_4K"),
            DecodeConfig("video/x-vnd.on2.vp9", 1920, 1080, "VP9_1080p"),
        )
        
        for (config in decodeConfigs) {
            println("\nRunning ${config.name}...")
            val result = runDecodeBenchmark(config)
            println("  Frames: ${result.framesDecoded}, Dropped: ${result.droppedFrames}")
            println("  Avg decode: ${if (result.framesDecoded > 0) result.totalDecodeTimeUs / result.framesDecoded else 0} µs")
            println("  FPS: ${if (result.totalDecodeTimeUs > 0) result.framesDecoded * 1_000_000.0 / result.totalDecodeTimeUs else 0.0}")
        }
        
        println("\n=== MEMORY BENCHMARK ===")
        val memResult = runMemoryBenchmark()
        println("  Allocation rate: ${memResult.allocationRateMbPerSec} MB/s")
        println("  GC count: ${memResult.gcCount}")
        println("  Peak memory: ${memResult.peakMemoryMb} MB")
        
        println("\n=== THERMAL BENCHMARK ===")
        val thermalResult = runThermalBenchmark()
        println("  Initial temp: ${thermalResult.initialTempC}°C")
        println("  Peak temp: ${thermalResult.peakTempC}°C")
        println("  Throttle events: ${thermalResult.throttleEvents}")
        
        println("\n=== BENCHMARK COMPLETE ===")
    }
    
    private fun runDecodeBenchmark(config: DecodeConfig): DecodeResult {
        var framesDecoded = 0L
        var droppedFrames = 0L
        var totalDecodeTimeUs = 0L
        
        val codec = try {
            android.media.MediaCodec.createDecoderByType(config.mimeType)
        } catch (e: Exception) {
            println("  Failed to create decoder: ${e.message}")
            return DecodeResult(0, 0, 0)
        }
        
        val format = android.media.MediaFormat.createVideoFormat(config.mimeType, config.width, config.height)
        codec.configure(format, null, null, 0)
        codec.start()
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        
        val testDurationMs = 10000
        val startTime = System.currentTimeMillis()
        var frameIndex = 0
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val buffer = inputBuffers[inputIndex]
                buffer?.clear()
                
                // Fill with test pattern
                fillTestPattern(buffer!!, config.width, config.height, frameIndex)
                
                val presentationTimeUs = (frameIndex * 33333L) // ~30fps
                val flags = if (frameIndex == 0) android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                
                codec.queueInputBuffer(inputIndex, 0, estimateFrameSize(config.width, config.height), presentationTimeUs, flags)
                frameIndex++
            }
            
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val decodeStart = System.nanoTime()
                
                val outputBuffer = outputBuffers[outputIndex]
                if (outputBuffer != null && bufferInfo.size > 0) {
                    // Simulate processing
                    var sum = 0
                    for (i in 0 until bufferInfo.size.coerceAtMost(64).step(4)) {
                        sum += outputBuffer.getInt(i)
                    }
                }
                
                totalDecodeTimeUs += (System.nanoTime() - decodeStart) / 1000
                framesDecoded++
                
                val isLate = bufferInfo.presentationTimeUs < System.nanoTime() / 1000 - 16000
                if (isLate) droppedFrames++
                
                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == android.media.MediaCodec.INFO_TRY_AGAIN_LATER) {
                droppedFrames++
            }
        }
        
        codec.stop()
        codec.release()
        
        return DecodeResult(framesDecoded, droppedFrames, totalDecodeTimeUs)
    }
    
    private fun fillTestPattern(buffer: java.nio.ByteBuffer, width: Int, height: Int, frameIndex: Int) {
        val ySize = width * height
        val uvSize = ySize / 4
        val totalSize = ySize + uvSize
        
        buffer.limit(totalSize)
        
        // Y plane - gradient
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = ((x + y + frameIndex) % 256).toByte()
                buffer.put(value)
            }
        }
        
        // UV planes - color pattern
        for (i in 0 until uvSize * 2) {
            buffer.put(((i + frameIndex * 2) % 256).toByte())
        }
    }
    
    private fun estimateFrameSize(width: Int, height: Int): Int {
        return width * height * 3 / 2 // YUV420
    }
    
    private fun runMemoryBenchmark(): MemoryBenchmarkResult {
        val runtime = Runtime.getRuntime()
        val initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        var peakMemory = initialMemory
        
        var gcCount = 0
        var gcTotalTimeMs = 0L
        
        val startTime = System.currentTimeMillis()
        val allocations = mutableListOf<ByteArray>()
        
        // Allocate and release memory to measure allocation rate
        repeat(1000) { i ->
            val size = (1024 * 1024 * (i % 10 + 1)).coerceAtMost(16 * 1024 * 1024) // 1-16MB
            val array = ByteArray(size)
            // Fill with data
            for (j in 0 until size step 1024) {
                array[j] = (i + j).toByte()
            }
            allocations.add(array)
            
            val currentMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            peakMemory = kotlin.math.max(peakMemory, currentMem)
            
            // Periodically clear to trigger GC
            if (i % 50 == 0) {
                val beforeGc = System.currentTimeMillis()
                System.gc()
                System.runFinalization()
                gcTotalTimeMs += System.currentTimeMillis() - beforeGc
                gcCount++
                allocations.clear()
            }
        }
        
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        val totalAllocatedMb = allocations.sumOf { it.size } / (1024.0 * 1024.0)
        val allocationRate = totalAllocatedMb / elapsedSec
        
        val finalMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        
        return MemoryBenchmarkResult(
            allocationRateMbPerSec = allocationRate,
            gcCount = gcCount,
            gcTotalTimeMs = gcTotalTimeMs,
            peakMemoryMb = peakMemory,
            finalMemoryMb = finalMemory,
        )
    }
    
    private fun runThermalBenchmark(): ThermalBenchmarkResult {
        var initialTempC = 0f
        var peakTempC = 0f
        var throttleEvents = 0
        var timeToThrottleMs = 0L
        var sustainedPerformancePercent = 100f
        
        // Try to read initial temperature
        initialTempC = readTemperature()
        peakTempC = initialTempC
        
        val startTime = System.currentTimeMillis()
        var hasThrottled = false
        
        // Run CPU-intensive workload to generate heat
        val threads = (0 until kotlin.math.max(2, Runtime.getRuntime().availableProcessors() / 2)).map {
            Thread {
                var sum = 0L
                while (!Thread.currentThread().isInterrupted) {
                    // CPU intensive work
                    for (j in 0..1000000) {
                        sum += j * j
                        sum = sum.shl(1) xor sum.shr(1)
                    }
                    // Small yield
                    Thread.yield()
                }
            }.apply { 
                start() 
                priority = Thread.MAX_PRIORITY 
            }
        }
        
        // Monitor temperature for 30 seconds
        val monitorThread = Thread {
            while (System.currentTimeMillis() - startTime < 30000) {
                Thread.sleep(1000)
                val temp = readTemperature()
                if (temp > 0) {
                    peakTempC = kotlin.math.max(peakTempC, temp)
                    
                    // Check for thermal throttling
                    if (temp >= 45f && !hasThrottled) {
                        hasThrottled = true
                        timeToThrottleMs = System.currentTimeMillis() - startTime
                        throttleEvents++
                    } else if (temp >= 45f) {
                        throttleEvents++
                    }
                }
            }
        }.apply { start() }
        
        try {
            monitorThread.join()
        } catch (e: InterruptedException) {}
        
        // Stop worker threads
        threads.forEach { it.interrupt() }
        threads.forEach { it.join(1000) }
        
        // Calculate sustained performance
        sustainedPerformancePercent = if (timeToThrottleMs > 0) {
            (timeToThrottleMs.toFloat() / 30000f) * 100f
        } else {
            100f
        }
        
        return ThermalBenchmarkResult(
            initialTempC = initialTempC,
            peakTempC = peakTempC,
            throttleEvents = throttleEvents,
            timeToThrottleMs = timeToThrottleMs,
            sustainedPerformancePercent = sustainedPerformancePercent,
        )
    }
    
    private fun readTemperature(): Float {
        return try {
            val thermalDir = java.io.File("/sys/class/thermal")
            val zones = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") }
                ?.sortedBy { it.name } ?: emptyArray()
            
            var maxTemp = 0f
            for (zone in zones) {
                val tempFile = java.io.File(zone, "temp")
                if (tempFile.exists()) {
                    val tempStr = tempFile.readText().trim()
                    val temp = tempStr.toFloatOrNull()
                    if (temp != null) {
                        val tempC = if (temp > 1000) temp / 1000 else temp
                        maxTemp = kotlin.math.max(maxTemp, tempC)
                    }
                }
            }
            maxTemp
        } catch (e: Exception) {
            0f
        }
    }
}