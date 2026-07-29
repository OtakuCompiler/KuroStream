package com.kurostream.players.frame

import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 * Represents a video frame with multiple buffer representations.
 * Supports zero-copy rendering via HardwareBuffer and SurfaceTexture.
 */
data class VideoFrame(
    val width: Int,
    val height: Int,
    val timestampUs: Long,
    val format: Int = MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
    val hardwareBuffer: HardwareBuffer? = null,
    val byteBuffer: ByteBuffer? = null,
    val surfaceTexture: SurfaceTexture? = null,
    val presentationTimeUs: Long = 0,
    val isKeyFrame: Boolean = false,
    val rotationDegrees: Int = 0,
    val cropRect: android.graphics.Rect? = null,
) {
    /**
     * Creates a VideoFrame from a MediaCodec output buffer.
     */
    companion object {
        fun fromMediaCodec(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
            width: Int,
            height: Int
        ): VideoFrame {
            val buffer = codec.getOutputBuffer(index)
            return VideoFrame(
                width = width,
                height = height,
                timestampUs = info.presentationTimeUs,
                byteBuffer = buffer,
                presentationTimeUs = info.presentationTimeUs,
                isKeyFrame = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
            )
        }

        /**
         * Creates a VideoFrame from a HardwareBuffer for zero-copy rendering.
         */
        fun fromHardwareBuffer(
            buffer: HardwareBuffer,
            timestampUs: Long,
            presentationTimeUs: Long = 0,
            isKeyFrame: Boolean = false
        ): VideoFrame {
            return VideoFrame(
                width = buffer.width,
                height = buffer.height,
                timestampUs = timestampUs,
                hardwareBuffer = buffer,
                presentationTimeUs = presentationTimeUs,
                isKeyFrame = isKeyFrame,
            )
        }

        /**
         * Creates a VideoFrame from a SurfaceTexture for zero-copy rendering.
         */
        fun fromSurfaceTexture(
            surfaceTexture: SurfaceTexture,
            timestampUs: Long,
            presentationTimeUs: Long = 0
        ): VideoFrame {
            return VideoFrame(
                width = surfaceTexture.width,
                height = surfaceTexture.height,
                timestampUs = timestampUs,
                surfaceTexture = surfaceTexture,
                presentationTimeUs = presentationTimeUs,
            )
        }
    }

    /**
     * Returns true if this frame has valid data.
     */
    fun isValid(): Boolean {
        return hardwareBuffer != null || byteBuffer != null || surfaceTexture != null
    }

    /**
     * Returns the primary buffer for rendering.
     */
    fun getPrimaryBuffer(): Any? {
        return hardwareBuffer ?: byteBuffer ?: surfaceTexture
    }
}