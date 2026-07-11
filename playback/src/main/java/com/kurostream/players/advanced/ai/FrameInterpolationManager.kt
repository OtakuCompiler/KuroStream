// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.players.advanced.ai

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.SystemClock
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Frame interpolation using RIFE (Real-Time Intermediate Flow Estimation) model.
 * Runs inference on a dedicated background thread pool for smooth playback.
 */
class FrameInterpolationManager private constructor(
    private val context: Context
) {

    companion object {
        @Volatile
        private var instance: FrameInterpolationManager? = null

        fun getInstance(context: Context): FrameInterpolationManager {
            return instance ?: synchronized(this) {
                instance ?: FrameInterpolationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

        const val MODEL_VERSION = "rife_v4.6_lite"
        const val INPUT_CHANNELS = 7  // 2 frames * 3 channels + 1 timestep
        const val OUTPUT_CHANNELS = 3
        const val MAX_QUEUE_SIZE = 6
        const val INTERPOLATION_FACTOR = 2  // Generate 1 intermediate frame between each pair
    }

    private val interpolationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(2)
    )
    private val isEnabled = AtomicBoolean(false)
    private val isModelLoaded = AtomicBoolean(false)
    private val frameQueue = ConcurrentLinkedQueue<FramePair>()
    private val outputQueue = ConcurrentLinkedQueue<InterpolatedFrame>()
    private val processedFrameCount = AtomicInteger(0)
    private val droppedFrameCount = AtomicInteger(0)
    private val totalLatencyNs = AtomicLong(0)

    private var rifeModel: Module? = null
    private var modelWidth: Int = 0
    private var modelHeight: Int = 0

    // Performance metrics
    data class InterpolationMetrics(
        val processedFrames: Int,
        val droppedFrames: Int,
        val averageLatencyMs: Double,
        val queueDepth: Int,
        val isThrottling: Boolean
    )

    fun initialize(width: Int, height: Int) {
        if (isModelLoaded.get()) return

        modelWidth = width
        modelHeight = height

        interpolationScope.launch {
            try {
                loadRifeModel()
                isModelLoaded.set(true)
                startInterpolationLoop()
            } catch (e: Exception) {
                android.util.Log.e("FrameInterpolation", "Failed to load model", e)
            }
        }
    }

    private suspend fun loadRifeModel() = withContext(Dispatchers.IO) {
        val modelPath = copyAssetIfNeeded("$MODEL_VERSION.ptl")
        rifeModel = LiteModuleLoader.load(modelPath)
        android.util.Log.i("FrameInterpolation", "RIFE model loaded: $MODEL_VERSION")
    }

    private fun copyAssetIfNeeded(assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists()) return file.absolutePath

        context.assets.open(assetName).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    /**
     * Submit a pair of consecutive frames for interpolation.
     * @param frameA First frame (RGBA ByteBuffer)
     * @param frameB Second frame (RGBA ByteBuffer)
     * @param presentationTimeUs PTS of frameA
     */
    @WorkerThread
    fun submitFramePair(frameA: ByteBuffer, frameB: ByteBuffer, presentationTimeUs: Long) {
        if (!isEnabled.get() || !isModelLoaded.get()) return

        if (frameQueue.size >= MAX_QUEUE_SIZE) {
            droppedFrameCount.incrementAndGet()
            return
        }

        val pair = FramePair(
            frameA = frameA,
            frameB = frameB,
            ptsA = presentationTimeUs,
            ptsB = presentationTimeUs + 16666, // Approximate next frame
            timestamp = SystemClock.elapsedRealtimeNanos()
        )
        frameQueue.offer(pair)
    }

    private fun startInterpolationLoop() {
        interpolationScope.launch(Dispatchers.Default) {
            while (isActive && isEnabled.get()) {
                val pair = frameQueue.poll()
                if (pair != null) {
                    interpolateFramePair(pair)
                } else {
                    delay(1)
                }
            }
        }
    }

    private suspend fun interpolateFramePair(pair: FramePair) = withContext(Dispatchers.Default) {
        val model = rifeModel ?: return@withContext
        val startTime = SystemClock.elapsedRealtimeNanos()

        try {
            // Convert frames to model input format [1, 7, H, W]
            val inputTensor = prepareRifeInput(pair.frameA, pair.frameB)

            // Run inference
            val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()

            // Extract interpolated frame
            val interpolatedFrame = extractInterpolatedFrame(outputTensor)

            // Calculate intermediate PTS
            val midPts = (pair.ptsA + pair.ptsB) / 2

            val result = InterpolatedFrame(
                buffer = interpolatedFrame,
                pts = midPts,
                generationLatencyNs = SystemClock.elapsedRealtimeNanos() - startTime
            )

            outputQueue.offer(result)
            processedFrameCount.incrementAndGet()
            totalLatencyNs.addAndGet(result.generationLatencyNs)

        } catch (e: Exception) {
            android.util.Log.e("FrameInterpolation", "Interpolation failed", e)
            droppedFrameCount.incrementAndGet()
        }
    }

    /**
     * Prepare RIFE model input: concatenate two RGB frames + timestep channel.
     * Input shape: [1, 7, H, W] where channels are [R1, G1, B1, R2, G2, B2, timestep]
     */
    private fun prepareRifeInput(frameA: ByteBuffer, frameB: ByteBuffer): Tensor {
        val size = modelWidth * modelHeight
        val floatData = FloatArray(INPUT_CHANNELS * size)

        // Normalize frame A [0, 255] -> [0, 1]
        frameA.rewind()
        for (i in 0 until size) {
            val r = (frameA.get(i * 4).toInt() and 0xFF) / 255.0f
            val g = (frameA.get(i * 4 + 1).toInt() and 0xFF) / 255.0f
            val b = (frameA.get(i * 4 + 2).toInt() and 0xFF) / 255.0f

            floatData[i] = r
            floatData[i + size] = g
            floatData[i + 2 * size] = b
        }

        // Normalize frame B
        frameB.rewind()
        for (i in 0 until size) {
            val r = (frameB.get(i * 4).toInt() and 0xFF) / 255.0f
            val g = (frameB.get(i * 4 + 1).toInt() and 0xFF) / 255.0f
            val b = (frameB.get(i * 4 + 2).toInt() and 0xFF) / 255.0f

            floatData[i + 3 * size] = r
            floatData[i + 4 * size] = g
            floatData[i + 5 * size] = b
        }

        // Timestep channel (0.5 = interpolate midpoint)
        for (i in 0 until size) {
            floatData[i + 6 * size] = 0.5f
        }

        return Tensor.fromBlob(
            floatData,
            longArrayOf(1, INPUT_CHANNELS.toLong(), modelHeight.toLong(), modelWidth.toLong())
        )
    }

    private fun extractInterpolatedFrame(tensor: Tensor): ByteBuffer {
        val data = tensor.dataAsFloatArray
        val size = modelWidth * modelHeight
        val buffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder())

        for (i in 0 until size) {
            val r = (data[i] * 255.0f).toInt().coerceIn(0, 255)
            val g = (data[i + size] * 255.0f).toInt().coerceIn(0, 255)
            val b = (data[i + 2 * size] * 255.0f).toInt().coerceIn(0, 255)

            buffer.put(i * 4, r.toByte())
            buffer.put(i * 4 + 1, g.toByte())
            buffer.put(i * 4 + 2, b.toByte())
            buffer.put(i * 4 + 3, 0xFF.toByte())
        }

        return buffer
    }

    /**
     * Retrieve interpolated frames for playback.
     * @return List of interpolated frames sorted by PTS, or null if none available.
     */
    fun consumeInterpolatedFrames(): List<InterpolatedFrame>? {
        if (outputQueue.isEmpty()) return null

        val frames = mutableListOf<InterpolatedFrame>()
        while (true) {
            val frame = outputQueue.poll() ?: break
            frames.add(frame)
        }
        return frames.sortedBy { it.pts }
    }

    fun getMetrics(): InterpolationMetrics {
        val processed = processedFrameCount.get()
        val dropped = droppedFrameCount.get()
        val avgLatency = if (processed > 0) totalLatencyNs.get() / processed / 1_000_000.0 else 0.0

        return InterpolationMetrics(
            processedFrames = processed,
            droppedFrames = dropped,
            averageLatencyMs = avgLatency,
            queueDepth = frameQueue.size,
            isThrottling = frameQueue.size >= MAX_QUEUE_SIZE - 1
        )
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        if (!enabled) {
            frameQueue.clear()
            outputQueue.clear()
        }
    }

    fun release() {
        isEnabled.set(false)
        interpolationScope.cancel()
        rifeModel?.destroy()
        rifeModel = null
        frameQueue.clear()
        outputQueue.clear()
    }

    data class FramePair(
        val frameA: ByteBuffer,
        val frameB: ByteBuffer,
        val ptsA: Long,
        val ptsB: Long,
        val timestamp: Long
    )

    data class InterpolatedFrame(
        val buffer: ByteBuffer,
        val pts: Long,
        val generationLatencyNs: Long
    )
}
