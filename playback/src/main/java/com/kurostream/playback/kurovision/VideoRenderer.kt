// This file is part of KuroStream.
//
// VideoRenderer — abstraction over GPU-based frame enhancement.
// Implementations: OpenGLRenderer, VulkanRenderer (future), PassthroughRenderer.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.opengl.EGLContext
import androidx.annotation.Keep

@Keep
interface VideoRenderer {
    fun initialize(): Boolean
    fun process(inputTexture: Int, width: Int, height: Int, mode: KuroVisionQualityMode): KuroVisionEngine.ProcessedFrame
    fun release()
    val eglContext: EGLContext?
    val isInitialized: Boolean
}

abstract class BaseVideoRenderer : VideoRenderer {
    @Volatile protected var initialized = false
    protected val renderScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.Job()
    )
    protected lateinit var profile: KuroVisionDeviceProfile
    protected abstract val TAG: String

    override fun release() {
        initialized = false
    }
}
