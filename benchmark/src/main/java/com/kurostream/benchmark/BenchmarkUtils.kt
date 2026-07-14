package com.kurostream.benchmark

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

object BenchmarkUtils {
    private const val TAG = "BenchmarkUtils"

    data class DecodeConfig(
        val mimeType: String, val width: Int, val height: Int, val name: String,
    )

    data class DecodeResult(
        val framesDecoded: Int, val droppedFrames: Int, val totalDecodeTimeUs: Long,
    )

    data class MemoryBenchmarkResult(
        val allocationRateMbPerSec: Double, val gcCount: Int, val gcTotalTimeMs: Long,
        val peakMemoryMb: Long, val finalMemoryMb: Long,
    )

    data class ThermalBenchmarkResult(
        val initialTempC: Float, val peakTempC: Float, val throttleEvents: Int,
        val timeToThrottleMs: Long, val sustainedPerformancePercent: Float,
    )

    data class BenchmarkReport(
        val timestamp: Long, val deviceModel: String, val androidVersion: String,
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
                    value.forEach { (k, v) -> builder.append("      \"$k\": $v,\n") }
                    builder.append("    },\n")
                } else {
                    builder.append("$value,\n")
                }
            }
            builder.append("  }\n")
            builder.append("}")
            return builder.toString()
        }
    }

    fun readTemperature(): Float {
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
                        maxTemp = kotlin.math.max(maxTemp, if (temp > 1000) temp / 1000 else temp)
                    }
                }
            }
            maxTemp
        } catch (e: Exception) {
            0f
        }
    }

    fun createDecoder(mimeType: String): MediaCodec? {
        return try {
            MediaCodec.createDecoderByType(mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create decoder for $mimeType", e)
            null
        }
    }

    fun configureDecoder(codec: MediaCodec, mimeType: String, width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec.configure(format, null, null, 0)
        codec.start()
    }

    fun generateTestFrames(frameCount: Int, frameSize: Int): ByteArray {
        val data = ByteArray(frameCount * frameSize)
        val random = java.util.Random(42)
        for (i in 0 until frameCount) {
            val offset = i * frameSize
            random.nextBytes(data, offset, frameSize.coerceAtMost(data.size - offset))
            if (i == 0 || i % 30 == 0) {
                data[offset] = 0x00
                data[offset + 1] = 0x00
                data[offset + 2] = 0x00
                data[offset + 3] = 0x01
                data[offset + 4] = if (i == 0) 0x67 else 0x65
            }
        }
        return data
    }

    fun decodeFrames(codec: MediaCodec, data: ByteArray, maxFrames: Int): DecodeResult {
        var framesDecoded = 0
        var droppedFrames = 0
        var totalDecodeTimeUs = 0L
        var inputOffset = 0
        val frameSize = data.size / maxFrames.coerceAtLeast(1)
        val bufferInfo = MediaCodec.BufferInfo()

        while (framesDecoded < maxFrames && inputOffset < data.size) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex) ?: ByteBuffer.allocate(frameSize)
                val remaining = data.size - inputOffset
                val chunkSize = remaining.coerceAtMost(frameSize)
                inputBuffer.clear()
                inputBuffer.put(data, inputOffset, chunkSize)
                inputOffset += chunkSize
                codec.queueInputBuffer(
                    inputIndex, 0, chunkSize, framesDecoded * 33333L,
                    if (framesDecoded == 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                )
                framesDecoded++
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val decodeStart = System.nanoTime()
                codec.getOutputBuffer(outputIndex)?.let { buffer ->
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

        return DecodeResult(framesDecoded, droppedFrames, totalDecodeTimeUs)
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
            allocationRateMbPerSec = allocationRate, gcCount = gcCount,
            gcTotalTimeMs = gcTotalTimeMs, peakMemoryMb = peakMemory, finalMemoryMb = finalMemory,
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
            (timeToThrottleMs.toFloat() / 30000f) * 100f
        } else {
            100f
        }

        return ThermalBenchmarkResult(
            initialTempC = initialTempC, peakTempC = peakTempC,
            throttleEvents = throttleEvents, timeToThrottleMs = timeToThrottleMs,
            sustainedPerformancePercent = sustainedPerformancePercent,
        )
    }

    fun fillTestPattern(buffer: java.nio.ByteBuffer, width: Int, height: Int, frameIndex: Int) {
        val ySize = width * height
        val uvSize = ySize / 4
        val totalSize = ySize + uvSize
        buffer.limit(totalSize)
        for (y in 0 until height) {
            for (x in 0 until width) {
                buffer.put(((x + y + frameIndex) % 256).toByte())
            }
        }
        for (i in 0 until uvSize * 2) {
            buffer.put(((i + frameIndex * 2) % 256).toByte())
        }
    }

    fun estimateFrameSize(width: Int, height: Int): Int = width * height * 3 / 2
}
