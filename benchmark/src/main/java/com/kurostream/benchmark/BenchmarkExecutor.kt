package com.kurostream.benchmark

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class BenchmarkExecutor {
    
    companion object {
        private const val TAG = "BenchmarkExecutor"
    }
    
    data class DecodeBenchmarkResult(
        val codecName: String,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val fps: Double,
        val avgDecodeTimeMs: Double,
        val droppedFramesPercent: Double,
        val throughputMbps: Double,
        val cpuUsagePercent: Double = 0.0,
        val peakMemoryMb: Long = 0,
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
    
    data class FullBenchmarkReport(
        val timestamp: Long,
        val deviceModel: String,
        val androidVersion: String,
        val decodeResults: List<DecodeBenchmarkResult>,
        val memoryResult: MemoryBenchmarkResult?,
        val thermalResult: ThermalBenchmarkResult?,
    ) {
        fun toJson(): String {
            return """
                {
                  "timestamp": $timestamp,
                  "device": {
                    "model": "$deviceModel",
                    "androidVersion": "$androidVersion",
                    "sdk": ${Build.VERSION.SDK_INT}
                  },
                  "decode": [
                    ${decodeResults.joinToString(",\n") { it.toJson() }}
                  ],
                  "memory": ${memoryResult?.toJson() ?: "null"},
                  "thermal": ${thermalResult?.toJson() ?: "null"}
                }
            """.trimIndent()
        }
    }
    
    fun runDecodeBenchmark(
        mimeType: String,
        width: Int,
        height: Int,
        codecName: String
    ): DecodeBenchmarkResult {
        val codec = try {
            MediaCodec.createDecoderByType(mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder for $mimeType", e)
            return DecodeBenchmarkResult(
                codecName = codecName,
                mimeType = mimeType,
                width = width,
                height = height,
                fps = 0.0,
                avgDecodeTimeMs = 0.0,
                droppedFramesPercent = 100.0,
                throughputMbps = 0.0,
            )
        }
        
        val format = MediaFormat.createVideoFormat(mimeType, width, height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        
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
        
        val fps = result.framesDecoded * 1000.0 / totalTimeMs
        val avgDecodeTimeMs = result.totalDecodeTimeUs.toDouble() / result.framesDecoded / 1000
        val droppedPercent = result.droppedFrames.toDouble() / result.framesDecoded * 100
        val throughputMbps = (testData.size * 8.0 / 1024 / 1024 / (totalTimeMs / 1000.0))
        
        return DecodeBenchmarkResult(
            codecName = codecName,
            mimeType = mimeType,
            width = width,
            height = height,
            fps = fps,
            avgDecodeTimeMs = avgDecodeTimeMs,
            droppedFramesPercent = droppedPercent,
            throughputMbps = throughputMbps,
        )
    }
    
    fun runAllDecodeBenchmarks(): List<DecodeBenchmarkResult> {
        val results = mutableListOf<DecodeBenchmarkResult>()
        
        val configs = listOf(
            "video/avc" to 1920 to 1080 to "H264_1080p",
            "video/hevc" to 1920 to 1080 to "HEVC_1080p",
            "video/hevc" to 3840 to 2160 to "HEVC_4K",
            "video/x-vnd.on2.vp9" to 1920 to 1080 to "VP9_1080p",
        )
        
        for ((mimeType, width, height, name) in configs) {
            try {
                val result = runDecodeBenchmark(mimeType, width, height, name)
                results.add(result)
                Log.i(TAG, "Completed $name: ${result.fps} fps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed $name", e)
            }
        }
        
        return results
    }
    
    fun runMemoryBenchmark(): MemoryBenchmarkResult {
        val runtime = Runtime.getRuntime()
        val initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        var peakMemory = initialMemory
        
        var gcCount = 0
        var gcTotalTimeMs = 0L
        
        val startTime = System.currentTimeMillis()
        val allocations = mutableListOf<ByteArray>()
        
        repeat(1000) { i ->
            val size = (1024 * 1024 * (i % 10 + 1)).coerceAtMost(16 * 1024 * 1024)
            val array = ByteArray(size)
            for (j in 0 until size step 1024) {
                array[j] = (i + j).toByte()
            }
            allocations.add(array)
            
            val currentMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            peakMemory = kotlin.math.max(peakMemory, currentMem)
            
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
    
    fun runThermalBenchmark(): ThermalBenchmarkResult {
        var initialTempC = 0f
        var peakTempC = 0f
        var throttleEvents = 0
        var timeToThrottleMs = 0L
        var sustainedPerformancePercent = 100f
        
        initialTempC = readTemperature()
        peakTempC = initialTempC
        
        val startTime = System.currentTimeMillis()
        var hasThrottled = false
        
        val threads = (0 until kotlin.math.max(2, Runtime.getRuntime().availableProcessors() / 2)).map {
            Thread {
                var sum = 0L
                while (!Thread.currentThread().isInterrupted) {
                    for (j in 0..1_000_000) {
                        sum += j * j
                        sum = sum.shl(1) xor sum.shr(1)
                    }
                    Thread.yield()
                }
            }.apply { 
                start() 
                priority = Thread.MAX_PRIORITY 
            }
        }
        
        val monitorThread = Thread {
            while (System.currentTimeMillis() - startTime < 30_000) {
                Thread.sleep(1000)
                val temp = readTemperature()
                if (temp > 0) {
                    peakTempC = kotlin.math.max(peakTempC, temp)
                    
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
        
        threads.forEach { it.interrupt() }
        threads.forEach { it.join(1000) }
        
        sustainedPerformancePercent = if (timeToThrottleMs > 0) {
            (timeToThrottleMs.toFloat() / 30_000f) * 100f
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
    
    fun runFullBenchmark(): FullBenchmarkReport {
        val decodeResults = runAllDecodeBenchmarks()
        val memoryResult = runMemoryBenchmark()
        val thermalResult = runThermalBenchmark()
        
        return FullBenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            decodeResults = decodeResults,
            memoryResult = memoryResult,
            thermalResult = thermalResult,
        )
    }
    
    private fun decodeFrames(
        codec: MediaCodec,
        data: ByteArray,
        maxFrames: Int
    ): FrameDecodeResult {
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
                        for (i in 0 until size.coerceAtMost(64).step(4)) {
                            sum += buffer.getInt(i)
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
        
        codec.queueInputBuffer(0, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }
        
        return FrameDecodeResult(framesDecoded, droppedFrames, totalDecodeTimeUs)
    }
    
    private fun generateTestFrames(frameCount: Int, frameSize: Int): ByteArray {
        val data = ByteArray(frameCount * frameSize)
        val random = java.util.Random(42)
        
        for (i in 0 until frameCount) {
            val offset = i * frameSize
            random.nextBytes(data, offset, frameSize.coerceAtMost(data.size - offset))
            
            if (i == 0 || i % 30 == 0) {
                data[offset] = 0x00.toByte()
                data[offset + 1] = 0x00.toByte()
                data[offset + 2] = 0x00.toByte()
                data[offset + 3] = 0x01.toByte()
                data[offset + 4] = if (i == 0) 0x67.toByte() else 0x65.toByte()
            }
        }
        
        return data
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
    
    private data class FrameDecodeResult(
        val framesDecoded: Int,
        val droppedFrames: Int,
        val totalDecodeTimeUs: Long,
    )
}

fun BenchmarkExecutor.DecodeBenchmarkResult.toJson(): String {
    return """
        {
          "codec": "$codecName",
          "mimeType": "$mimeType",
          "resolution": "${width}x$height",
          "fps": $fps,
          "avgDecodeTimeMs": $avgDecodeTimeMs,
          "droppedFramesPercent": $droppedFramesPercent,
          "throughputMbps": $throughputMbps
        }
    """.trimIndent()
}

fun BenchmarkExecutor.MemoryBenchmarkResult.toJson(): String {
    return """
        {
          "allocationRateMbPerSec": $allocationRateMbPerSec,
          "gcCount": $gcCount,
          "gcTotalTimeMs": $gcTotalTimeMs,
          "peakMemoryMb": $peakMemoryMb,
          "finalMemoryMb": $finalMemoryMb
        }
    """.trimIndent()
}

fun BenchmarkExecutor.ThermalBenchmarkResult.toJson(): String {
    return """
        {
          "initialTempC": $initialTempC,
          "peakTempC": $peakTempC,
          "throttleEvents": $throttleEvents,
          "timeToThrottleMs": $timeToThrottleMs,
          "sustainedPerformancePercent": $sustainedPerformancePercent
        }
    """.trimIndent()
}