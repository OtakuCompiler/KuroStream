package com.kurostream.players.render

import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.opengl.GLES20
import com.kurostream.players.buffer.HardwareBufferCache
import com.kurostream.players.frame.VideoFrame

class GlRenderer {
    private val hardwareBufferCache = HardwareBufferCache()
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val lock = Any()
    private var isInitialized = false
    private var isReleased = false

    fun init(eglContext: EGLContext, eglSurface: EGLSurface) {
        synchronized(lock) {
            if (isReleased) {
                throw IllegalStateException("Renderer has been released")
            }
            this.eglContext = eglContext
            this.eglSurface = eglSurface
            val result = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            if (!result) {
                throw IllegalStateException("eglMakeCurrent failed: ${EGL14.eglGetError()}")
            }
            isInitialized = true
        }
    }

    fun renderFrame(frame: VideoFrame) {
        synchronized(lock) {
            if (!isInitialized || isReleased) {
                return
            }
            
            val buffer = hardwareBufferCache.getOrCreate(frame.width, frame.height)
            val eglImage = hardwareBufferCache.createEGLImage(buffer)
            
            val textureId = IntArray(1)
            GLES20.glGenTextures(1, textureId, 0)
            val texture = textureId[0]
            
            try {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
                
                GLES20.glEGLImageTargetTexture2DOES(GLES20.GL_TEXTURE_2D, eglImage)
                
                // Render - draw quad with texture
                // ... drawing code ...
                
                GLES20.glFinish()
            } finally {
                // Cleanup in finally block to ensure resources are released
                hardwareBufferCache.destroyEGLImage(eglImage)
                GLES20.glDeleteTextures(1, textureId, 0)
            }
        }
    }
    
    fun makeCurrent() {
        synchronized(lock) {
            if (isInitialized && !isReleased) {
                val result = EGL14.eglMakeCurrent(eglDisplay, eglSurface!!, eglSurface!!, eglContext!!)
                if (!result) {
                    throw IllegalStateException("eglMakeCurrent failed: ${EGL14.eglGetError()}")
                }
            }
        }
    }
    
    fun release() {
        synchronized(lock) {
            if (isReleased) return
            isReleased = true
            isInitialized = false
            
            // Release EGL resources
            if (eglContext != null && eglSurface != null) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            }
            eglContext = null
            eglSurface = null
        }
    }
    
    fun isInitialized(): Boolean {
        synchronized(lock) {
            return isInitialized && !isReleased
        }
    }
}