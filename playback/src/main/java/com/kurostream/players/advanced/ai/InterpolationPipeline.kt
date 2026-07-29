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

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Integrates frame interpolation into the video playback pipeline.
 * Intercepts decoded frames, generates intermediate frames, and feeds them to the output surface.
 */
class InterpolationPipeline(
    private val interpolationManager: FrameInterpolationManager,
    private val outputSurface: Surface
) {
    private val pipelineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isRunning = AtomicBoolean(false)
    private val frameBuffer = PriorityBlockingQueue<DecodedFrame>(16) { a, b ->
        a.pts.compareTo(b.pts)
    }

    // Frame pacing
    private val lastOutputPts = AtomicLong(0)
    private val targetFrameIntervalNs = AtomicLong(16_666_666) // 60fps

    data class DecodedFrame(
        val buffer: ByteBuffer,
        val pts: Long,
        val format: MediaFormat
    )

    fun start() {
        isRunning.set(true)
        pipelineScope.launch { frameInterpolationLoop() }
        pipelineScope.launch { frameOutputLoop() }
    }

    /**
     * Submit a decoded frame from MediaCodec.
     */
    fun onFrameDecoded(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, format: MediaFormat) {
        if (!isRunning.get()) return

        val frame = DecodedFrame(
            buffer = buffer,
            pts = bufferInfo.presentationTimeUs,
            format = format
        )
        frameBuffer.offer(frame)
    }

    private suspend fun frameInterpolationLoop() = withContext(Dispatchers.Default) {
        var previousFrame: DecodedFrame? = null

        while (isActive && isRunning.get()) {
            val currentFrame = frameBuffer.poll()
            if (currentFrame == null) {
                delay(1)
                continue
            }

            // Submit pair for interpolation
            previousFrame?.let { prev ->
                interpolationManager.submitFramePair(
                    frameA = prev.buffer,
                    frameB = currentFrame.buffer,
                    presentationTimeUs = prev.pts
                )
            }

            // Output original frame immediately (low latency)
            renderFrame(currentFrame)

            // Output interpolated frames when ready
            val interpolated = interpolationManager.consumeInterpolatedFrames()
            interpolated?.forEach { interp ->
                val interpFrame = DecodedFrame(
                    buffer = interp.buffer,
                    pts = interp.pts,
                    format = currentFrame.format
                )
                renderFrame(interpFrame)
            }

            previousFrame = currentFrame
        }
    }

    private suspend fun frameOutputLoop() = withContext(Dispatchers.Default) {
        // Frame pacing to maintain consistent output timing
        while (isActive && isRunning.get()) {
            val now = SystemClock.elapsedRealtimeNanos()
            val nextOutput = lastOutputPts.get() + targetFrameIntervalNs.get()
            val waitTime = nextOutput - now

            if (waitTime > 0) {
                delay(waitTime / 1_000_000)
            }
        }
    }

    private fun renderFrame(frame: DecodedFrame) {
        // Render to output surface
        // This would integrate with the existing Surface rendering pipeline
        lastOutputPts.set(SystemClock.elapsedRealtimeNanos())
    }

    fun stop() {
        isRunning.set(false)
        pipelineScope.cancel()
        frameBuffer.clear()
    }
}
