// This file is part of KuroStream.
//
// KuroVisionEngine — singleton orchestrator for the KuroVision enhancement
// pipeline. Owns the device profile, active quality mode, renderer reference,
// and provides a simple hook for backends to push decoded frames through the
// enhancement stack before surface presentation.
//
// Architecture:
//   Backend (Media3 / VLC / MPV)
//        │
//        ▼
//   KuroVisionEngine.processFrame(inputTexture) → outputTexture
//        │
//        ▼
//   Surface / Swapchain (zero-copy when possible)
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroVisionEngine @Inject constructor(
    private val context: Context,
    private val settings: KuroVisionSettings,
    private val inspector: AndroidDeviceInspector,
) {
    private val engineScope = CoroutineScope(Dispatchers.Default + Job())

    @Volatile private var profile: KuroVisionDeviceProfile? = null
    @Volatile private var activeMode: KuroVisionQualityMode = KuroVisionQualityMode.CINEMA
    @Volatile private var renderer: VideoRenderer? = null
    @Volatile private var eglContext: EGLContext? = null
    @Volatile private var initialized = false

    val currentProfile: KuroVisionDeviceProfile
        get() = profile ?: inspector.inspect().also { profile = it }

    val currentMode: KuroVisionQualityMode
        get() = activeMode

    val isInitialized: Boolean
        get() = initialized

    suspend fun initialize() {
        if (initialized) return
        withContext(Dispatchers.Main) {
            profile = inspector.inspect()
            activeMode = profile!!.recommendedQualityMode
            Log.i(TAG, "KuroVision init: device=${profile!!.modelLabel} class=${profile!!.deviceClass} mode=$activeMode")
        }
        try {
            renderer = OpenGLRenderer(currentProfile)
            val eglOk = renderer!!.initialize()
            if (!eglOk) {
                Log.w(TAG, "KuroVision: OpenGL init failed, falling back to passthrough")
                activeMode = KuroVisionQualityMode.HARDWARE
            } else {
                eglContext = renderer!!.eglContext
            }
        } catch (t: Throwable) {
            Log.w(TAG, "KuroVision init failed: ${t.message}")
            activeMode = KuroVisionQualityMode.HARDWARE
        }
        initialized = true

        engineScope.launch {
            observeSettings()
        }
    }

    private suspend fun observeSettings() {
        try {
            settings.enabled.collect { enabled ->
                if (!enabled) {
                    activeMode = KuroVisionQualityMode.HARDWARE
                } else {
                    val pref = settings.qualityMode.first()
                    if (pref != activeMode) {
                        activeMode = KuroVisionQualityMode.chooseFor(currentProfile, pref)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Settings observation failed", t)
        }
    }

    fun processFrame(inputTexture: Int, width: Int, height: Int): ProcessedFrame {
        if (activeMode == KuroVisionQualityMode.HARDWARE || renderer == null) {
            return ProcessedFrame.passthrough(inputTexture, width, height)
        }
        return renderer!!.process(inputTexture, width, height, activeMode)
    }

    fun release() {
        try {
            renderer?.release()
            eglContext?.let {
                EGL14.eglDestroyContext(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY), it)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "release failed", t)
        } finally {
            renderer = null
            eglContext = null
            initialized = false
        }
    }

    data class ProcessedFrame(
        val texture: Int,
        val width: Int,
        val height: Int,
        val isPassthrough: Boolean,
    ) {
        companion object {
            fun passthrough(tex: Int, w: Int, h: Int) = ProcessedFrame(tex, w, h, true)
        }
    }

companion object {
        private const val TAG = "KuroVisionEngine"
    }
}
