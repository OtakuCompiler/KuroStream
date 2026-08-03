// This file is part of KuroStream.
//
// NativeFramePool — zero-copy frame buffer pool using direct ByteBuffers
// and reusable texture handles. Reduces GC pressure during heavy playback.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeFramePool @Inject constructor(
    private val profile: KuroVisionDeviceProfile,
) {
    private val pool = ConcurrentLinkedQueue<PooledFrame>()
    private val maxSize = (profile.memoryBudgetMb * 1024 * 1024 / 4).coerceAtLeast(8)
    private var allocated = 0

    fun acquire(width: Int, height: Int): PooledFrame {
        var frame = pool.poll()
        if (frame == null || frame.width < width || frame.height < height) {
            frame = allocate(width, height)
            allocated++
        }
        return frame
    }

    fun release(frame: PooledFrame) {
        if (pool.size < maxSize) {
            pool.offer(frame)
        } else {
            frame.destroy()
            allocated--
        }
    }

    fun clear() {
        while (pool.isNotEmpty()) {
            pool.poll()?.destroy()
        }
        allocated = 0
    }

    private fun allocate(width: Int, height: Int): PooledFrame {
        val size = width * height * 4
        val buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        if (GLES20.glGetError() != 0) {
            Log.w(TAG, "glTexParameteri error")
        }
        return PooledFrame(texIds[0], width, height, buffer)
    }

    class PooledFrame(
        val textureId: Int,
        val width: Int,
        val height: Int,
        val buffer: ByteBuffer,
    ) {
        fun destroy() {
            try {
                val ids = intArrayOf(textureId)
                GLES20.glDeleteTextures(1, ids, 0)
            } catch (_: Exception) { }
        }
    }

    companion object {
        private const val TAG = "NativeFramePool"
    }
}
