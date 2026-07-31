// This file is part of KuroStream.
//
// KuroVisionPipeline — glue layer between playback backends and the engine.
// Each backend calls [processFrame] on decoded output to apply enhancement
// before presenting. Designed to hook into Media3's VideoProcessor or VLC's
// video output callbacks.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroVisionPipeline @Inject constructor(
    private val context: Context,
    private val engine: KuroVisionEngine,
) {

    private val _frameStats = MutableStateFlow(FrameStats())
    val frameStats: StateFlow<FrameStats> = _frameStats.asStateFlow()

    @Volatile private var frameCount = 0
    @Volatile private var dropCount = 0
    @Volatile private var lastFpsTime = System.nanoTime()
    @Volatile private var currentFps = 0f

    fun onFrameAvailable(textureId: Int, width: Int, height: Int): Int {
        val t0 = System.nanoTime()
        val result = if (engine.currentMode != KuroVisionQualityMode.HARDWARE && engine.isInitialized) {
            engine.processFrame(textureId, width, height)
        } else {
            KuroVisionEngine.ProcessedFrame.passthrough(textureId, width, height)
        }
        val elapsedUs = (System.nanoTime() - t0) / 1000

        frameCount++
        if (frameCount % 60 == 0) {
            val now = System.nanoTime()
            currentFps = 60_000_000_000f / (now - lastFpsTime).coerceAtLeast(1)
            lastFpsTime = now
        }

        _frameStats.value = FrameStats(
            fps = currentFps,
            frameCount = frameCount,
            dropCount = dropCount,
            lastProcessTimeUs = elapsedUs,
            isFallback = result.isPassthrough,
            mode = engine.currentMode,
        )

        return result.texture
    }

    fun onFrameDropped() {
        dropCount++
    }

    fun resetStats() {
        frameCount = 0
        dropCount = 0
        currentFps = 0f
        lastFpsTime = System.nanoTime()
    }

    data class FrameStats(
        val fps: Float = 0f,
        val frameCount: Long = 0,
        val dropCount: Int = 0,
        val lastProcessTimeUs: Long = 0,
        val isFallback: Boolean = true,
        val mode: KuroVisionQualityMode = KuroVisionQualityMode.HARDWARE,
    )
}
