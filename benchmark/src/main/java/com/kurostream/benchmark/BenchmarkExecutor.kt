package com.kurostream.benchmark

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer

class BenchmarkExecutor {

    companion object {
        private const val TAG = "BenchmarkExecutor"
    }

    data class DecodeBenchmarkResult(
        val codecName: String, val mimeType: String, val width: Int, val height: Int,
        val fps: Double, val avgDecodeTimeMs: Double, val droppedFramesPercent: Double,
        val throughputMbps: Double, val cpuUsagePercent: Double = 0.0, val peakMemoryMb: Long = 0,
    )

    data class FullBenchmarkReport(
        val timestamp: Long, val deviceModel: String, val androidVersion: String,
        val decodeResults: List<DecodeBenchmarkResult>,
        val memoryResult: BenchmarkUtils.MemoryBenchmarkResult?,
        val thermalResult: BenchmarkUtils.ThermalBenchmarkResult?,
    ) {
        fun toJson(): String {
            return buildString {
                appendLine("{")
                appendLine("  \"timestamp\": $timestamp,")
                appendLine("  \"device\": {")
                appendLine("    \"model\": \"$deviceModel\",")
                appendLine("    \"androidVersion\": \"$androidVersion\",")
                appendLine("    \"sdk\": ${Build.VERSION.SDK_INT}")
                appendLine("  },")
                appendLine("  \"decode\": [")
                append(decodeResults.joinToString(",\n") { it.toJson() })
                appendLine("  ],")
                appendLine("  \"memory\": ${memoryResult?.let { memoryToJson(it) } ?: "null"},")
                appendLine("  \"thermal\": ${thermalResult?.let { thermalToJson(it) } ?: "null"}")
                append("}")
            }
        }

        private fun memoryToJson(result: BenchmarkUtils.MemoryBenchmarkResult): String = """
            {
              "allocationRateMbPerSec": ${result.allocationRateMbPerSec},
              "gcCount": ${result.gcCount},
              "gcTotalTimeMs": ${result.gcTotalTimeMs},
              "peakMemoryMb": ${result.peakMemoryMb},
              "finalMemoryMb": ${result.finalMemoryMb}
            }
        """.trimIndent()

        private fun thermalToJson(result: BenchmarkUtils.ThermalBenchmarkResult): String = """
            {
              "initialTempC": ${result.initialTempC},
              "peakTempC": ${result.peakTempC},
              "throttleEvents": ${result.throttleEvents},
              "timeToThrottleMs": ${result.timeToThrottleMs},
              "sustainedPerformancePercent": ${result.sustainedPerformancePercent}
            }
        """.trimIndent()
    }

    private fun DecodeBenchmarkResult.toJson(): String = """
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

    fun runDecodeBenchmark(mimeType: String, width: Int, height: Int, codecName: String): DecodeBenchmarkResult {
        val codec = try {
            MediaCodec.createDecoderByType(mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder for $mimeType", e)
            return DecodeBenchmarkResult(codecName, mimeType, width, height, 0.0, 0.0, 100.0, 0.0)
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

        val fps = result.framesDecoded * 1000.0 / totalTimeMs
        val avgDecodeTimeMs = result.totalDecodeTimeUs.toDouble() / result.framesDecoded / 1000
        val droppedPercent = result.droppedFrames.toDouble() / result.framesDecoded * 100
        val throughputMbps = (testData.size * 8.0 / 1024 / 1024 / (totalTimeMs / 1000.0))

        return DecodeBenchmarkResult(
            codecName = codecName, mimeType = mimeType, width = width, height = height,
            fps = fps, avgDecodeTimeMs = avgDecodeTimeMs,
            droppedFramesPercent = droppedPercent, throughputMbps = throughputMbps,
        )
    }

    fun runAllDecodeBenchmarks(): List<DecodeBenchmarkResult> {
        val configs = listOf(
            Triple("video/avc", 1920) to 1080 to "H264_1080p",
            Triple("video/hevc", 1920) to 1080 to "HEVC_1080p",
            Triple("video/hevc", 3840) to 2160 to "HEVC_4K",
            Triple("video/x-vnd.on2.vp9", 1920) to 1080 to "VP9_1080p",
        )
        return configs.map { ((mimeType, width), height), name ->
            try {
                runDecodeBenchmark(mimeType, width, height, name).also {
                    Log.i(TAG, "Completed $name: ${it.fps} fps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed $name", e)
                null
            }
        }.filterNotNull()
    }

    fun runFullBenchmark(): FullBenchmarkReport {
        val decodeResults = runAllDecodeBenchmarks()
        val memoryResult = BenchmarkUtils.runMemoryBenchmark()
        val thermalResult = BenchmarkUtils.runThermalBenchmark()

        return FullBenchmarkReport(
            timestamp = System.currentTimeMillis(), deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            decodeResults = decodeResults, memoryResult = memoryResult, thermalResult = thermalResult,
        )
    }
}
