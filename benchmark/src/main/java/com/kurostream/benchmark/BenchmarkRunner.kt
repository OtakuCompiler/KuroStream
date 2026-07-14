package com.kurostream.benchmark

import android.util.Log
import androidx.benchmark.BenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
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
    fun runDecodeBenchmarks() = runBlocking {
        val results = mutableMapOf<String, Any>()

        val configs = listOf(
            BenchmarkUtils.DecodeConfig("video/avc", 1920, 1080, "H264_1080p"),
            BenchmarkUtils.DecodeConfig("video/hevc", 1920, 1080, "HEVC_1080p"),
            BenchmarkUtils.DecodeConfig("video/hevc", 3840, 2160, "HEVC_4K"),
            BenchmarkUtils.DecodeConfig("video/x-vnd.on2.vp9", 1920, 1080, "VP9_1080p"),
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

        val report = BenchmarkUtils.BenchmarkReport(
            timestamp = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            results = results,
        )

        Log.i("BenchmarkRunner", "Benchmark Report:\n${report.toJson()}")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.filesDir, "benchmark_report_${System.currentTimeMillis()}.json")
        file.writeText(report.toJson())
        Log.i("BenchmarkRunner", "Report saved to: ${file.absolutePath}")
    }

    @Test
    fun runMemoryBenchmark() = runBlocking {
        val result = BenchmarkUtils.runMemoryBenchmark()

        val report = BenchmarkUtils.BenchmarkReport(
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
    fun runThermalBenchmark() = runBlocking {
        val result = BenchmarkUtils.runThermalBenchmark()

        val report = BenchmarkUtils.BenchmarkReport(
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

    private fun runDecodeBenchmark(config: BenchmarkUtils.DecodeConfig): BenchmarkUtils.DecodeResult {
        val codec = BenchmarkUtils.createDecoder(config.mimeType) ?: return BenchmarkUtils.DecodeResult(0, 0, 0)

        BenchmarkUtils.configureDecoder(codec, config.mimeType, config.width, config.height)

        val testDurationMs = 10000L
        val startTime = System.currentTimeMillis()
        var frameIndex = 0
        var framesDecoded = 0L
        var droppedFrames = 0L
        var totalDecodeTimeUs = 0L

        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val buffer = codec.getInputBuffer(inputIndex)
                buffer?.let {
                    BenchmarkUtils.fillTestPattern(it, config.width, config.height, frameIndex)
                }
                val presentationTimeUs = (frameIndex * 33333L)
                val flags = if (frameIndex == 0) android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                codec.queueInputBuffer(inputIndex, 0, BenchmarkUtils.estimateFrameSize(config.width, config.height), presentationTimeUs, flags)
                frameIndex++
            }

            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val decodeStart = System.nanoTime()
                codec.getOutputBuffer(outputIndex)?.let { outputBuffer ->
                    if (bufferInfo.size > 0) {
                        var sum = 0
                        for (i in 0 until bufferInfo.size.coerceAtMost(64).step(4)) {
                            sum += outputBuffer.getInt(i)
                        }
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

        return BenchmarkUtils.DecodeResult(framesDecoded.toInt(), droppedFrames.toInt(), totalDecodeTimeUs)
    }
}
