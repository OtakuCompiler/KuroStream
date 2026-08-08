// This file is part of KuroStream.
//
// OpenGLRenderer — VideoRenderer implementation using OpenGL ES 2/3.
// Manages EGL context, offscreen FBO for two-pass rendering, and
// coordinates with EnhancedUpscaleEngine.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.GLES20
import android.util.Log
import com.kurostream.players.render.EnhancedUpscaleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenGLRenderer @Inject constructor(
    private val profile: KuroVisionDeviceProfile,
) : VideoRenderer {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContextHandle: EGLContext? = null
    private var upscaler: EnhancedUpscaleEngine? = null
    private var fboTexture = 0
    private var fboId = 0
    private var passthroughProgram = 0
    @Volatile private var isInit = false

    companion object {
        private const val TAG = "OpenGLRenderer"
    }

    override fun initialize(): Boolean {
        return try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Log.w(TAG, "No EGL display")
                return false
            }
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                Log.w(TAG, "EGL init failed")
                return false
            }
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0] ?: run {
                Log.w(TAG, "No EGL config")
                return false
            }

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, if (profile.supportsOpenGlEs3) 3 else 2,
                EGL14.EGL_NONE,
            )
            eglContextHandle = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContextHandle == null || eglContextHandle == EGL14.EGL_NO_CONTEXT) {
                Log.w(TAG, "EGL context creation failed")
                return false
            }

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            val pbuffer = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttribs, 0)
            if (pbuffer == null || pbuffer == EGL14.EGL_NO_SURFACE) {
                Log.w(TAG, "Pbuffer creation failed")
                return false
            }
            EGL14.eglMakeCurrent(eglDisplay, pbuffer, pbuffer, eglContextHandle)

            upscaler = EnhancedUpscaleEngine(profile).also { it.initialize() }
            createPassthroughProgram()

            isInit = true
            Log.i(TAG, "OpenGLRenderer init OK (GL ES ${if (profile.supportsOpenGlEs3) 3 else 2})")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "initialize failed: ${t.message}")
            false
        }
    }

override fun process(
    inputTexture: Int,
    width: Int,
    height: Int,
    mode: KuroVisionQualityMode,
  ): KuroVisionEngine.ProcessedFrame {
    if (!isInit || upscaler == null) {
      return KuroVisionEngine.ProcessedFrame.passthrough(inputTexture, width, height)
    }
return try {
    upscaler!!.setMode(mode, profile.recommendedUpscaleAlgorithm)
    upscaler!!.render(inputTexture, width, height, width, height)
    KuroVisionEngine.ProcessedFrame(inputTexture, width, height, false)
  } catch (t: Throwable) {
      Log.w(TAG, "process failed: ${t.message}")
      KuroVisionEngine.ProcessedFrame.passthrough(inputTexture, width, height)
    }
  }

    override fun release() {
        try {
            isInit = false
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            eglContextHandle?.let { EGL14.eglDestroyContext(eglDisplay, it) }
            upscaler?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "release error", t)
        } finally {
            eglContextHandle = null
            upscaler = null
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }

    override val eglContext: EGLContext?
        get() = eglContextHandle

 override val isInitialized: Boolean
    get() = isInit

    private fun createPassthroughProgram() {
        val vs = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 vTc;
            void main() { gl_Position = vPosition; vTc = vTexCoord; }
        """.trimIndent()
        val fs = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTc;
            uniform samplerExternalOES sTexture;
            void main() { gl_FragColor = texture2D(sTexture, vTc); }
        """.trimIndent()
        passthroughProgram = createProgram(vs, fs)
    }

    private fun createProgram(vs: String, fs: String): Int {
        val v = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also {
            GLES20.glShaderSource(it, vs)
            GLES20.glCompileShader(it)
        }
        val f = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also {
            GLES20.glShaderSource(it, fs)
            GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, v)
            GLES20.glAttachShader(it, f)
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(v)
            GLES20.glDeleteShader(f)
        }
    }
}
