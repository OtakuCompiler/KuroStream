package com.kurostream.players.buffer

import android.hardware.HardwareBuffer
import android.opengl.EGL14
import android.opengl.EGLDisplay
import android.opengl.EGLImageKHR
import java.util.concurrent.ConcurrentHashMap

class HardwareBufferCache {
    private val eglDisplay: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val lock = Any()
    private var isInitialized = false
    
    // Actual cache for HardwareBuffers
    private val bufferCache = ConcurrentHashMap<String, HardwareBuffer>()
    private val eglImageCache = ConcurrentHashMap<String, EGLImageKHR>()
    
    init {
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw IllegalStateException("Failed to initialize EGL display: ${EGL14.eglGetError()}")
        }
        isInitialized = true
    }
    
    private fun cacheKey(width: Int, height: Int, format: Int) = "$width}x$height:$format"
    
    fun getOrCreate(width: Int, height: Int, format: Int = HardwareBuffer.RGBA_8888): HardwareBuffer {
        val key = cacheKey(width, height, format)
        
        // Try to get from cache first
        bufferCache[key]?.let { cached ->
            if (!cached.isClosed()) return cached
            // Buffer was closed, remove from cache
            bufferCache.remove(key)
        }
        
        // Create new buffer
        val buffer = HardwareBuffer.allocate(
            HardwareBuffer.Descriptor().apply {
                this.width = width
                this.height = height
                this.format = format
                this.usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                            HardwareBuffer.USAGE_COMPRESSIBLE
            }
        )
        
        bufferCache[key] = buffer
        return buffer
    }
    
    fun createEGLImage(buffer: HardwareBuffer): EGLImageKHR {
        synchronized(lock) {
            checkInitialized()
            val eglImage = EGL14.eglCreateImageKHR(
                eglDisplay,
                EGL14.EGL_NO_CONTEXT,
                EGL14.EGL_NATIVE_BUFFER_ANDROID,
                buffer,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            val error = EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS || eglImage == EGL14.EGL_NO_IMAGE_KHR) {
                throw IllegalStateException("Failed to create EGLImage: $error")
            }
            return eglImage
        }
    }
    
    fun destroyEGLImage(eglImage: EGLImageKHR) {
        synchronized(lock) {
            checkInitialized()
            if (eglImage != EGL14.EGL_NO_IMAGE_KHR) {
                EGL14.eglDestroyImageKHR(eglDisplay, eglImage)
                val error = EGL14.eglGetError()
                if (error != EGL14.EGL_SUCCESS) {
                    throw IllegalStateException("Failed to destroy EGLImage: $error")
                }
            }
        }
    }
    
    fun release() {
        synchronized(lock) {
            // Release all cached HardwareBuffers
            bufferCache.values.forEach { buffer ->
                if (!buffer.isClosed()) {
                    buffer.close()
                }
            }
            bufferCache.clear()
            
            // Release all EGL images
            eglImageCache.values.forEach { eglImage ->
                if (eglImage != EGL14.EGL_NO_IMAGE_KHR) {
                    EGL14.eglDestroyImageKHR(eglDisplay, eglImage)
                }
            }
            eglImageCache.clear()
            
            if (isInitialized) {
                EGL14.eglTerminate(eglDisplay)
                isInitialized = false
            }
        }
    }
    
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("EGL display not initialized or already released")
        }
    }
}