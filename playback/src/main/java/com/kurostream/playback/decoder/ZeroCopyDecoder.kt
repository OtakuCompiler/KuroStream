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

package com.kurostream.playback.decoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import com.kurostream.playback.render.GpuTextureRecycler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Zero-Copy Decoder Output using MediaCodec with Surface
 * 
 * Configures MediaCodec to output directly to a Surface, eliminating buffer copies
 * between decoder and renderer. Uses EGL for GPU texture sharing.
 * 
 * Target: <50ms seek latency, <80MB RAM during 4K playback
 */
class ZeroCopyDecoder(
    private val textureRecycler: GpuTextureRecycler,
    private val config: DecoderConfig = DecoderConfig.DEFAULT
) {
    private val TAG = "ZeroCopyDecoder"
    
    // Decoder state
    private var mediaCodec: MediaCodec? = null
    private var surface: Surface? = null
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    
    // Texture management
    private var outputTextureId: Int = 0
    private var framebufferId: Int = 0
    private var renderbufferId: Int = 0
    
    // Decoder thread
    private var decoderThread: HandlerThread? = null
    private var decoderHandler: Handler? = null
    
    // Callbacks
    private var frameAvailableCallback: ((Surface) -> Unit)? = null
    private var errorCallback: ((DecoderError) -> Unit)? = null
    
    // Statistics
    private val _stats = MutableStateFlow(DecoderStats(0, 0, 0, 0, 0, 0, 0, 0, 0.0))
    val stats: StateFlow<DecoderStats> = _stats.asStateFlow()
    
    private val totalFramesDecoded = AtomicLong(0)
    private val totalFramesRendered = AtomicLong(0)
    private val totalDecodeTimeMs = AtomicLong(0)
    private val totalRenderTimeMs = AtomicLong(0)
    private val peakDecodeTimeMs = AtomicLong(0)
    private val peakRenderTimeMs = AtomicLong(0)
    private val currentDecodeTimeMs = AtomicLong(0)
    private val currentRenderTimeMs = AtomicLong(0)
    
    // Scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val shutdownFlag = AtomicBoolean(false)
    
    // Frame queue for rendering — bounded to prevent OOM
    private val frameQueue = Channel<DecodedFrame>(Channel.RENDEZVOUS)
    
    companion object {
        // MediaCodec configuration
        private const val TIMEOUT_US = 10_000L // 10ms timeout
        private const val MAX_FRAMES_IN_FLIGHT = 2 // Max frames being processed (was 4)
    }
    
    /**
     * Initialize the decoder for a specific media format.
     * 
     * @param format MediaFormat describing the video
     * @return Result with Surface for MediaCodec configuration
     */
    fun initialize(format: MediaFormat): Result<Surface> {
        try {
            // Create decoder thread
            decoderThread = HandlerThread("ZeroCopyDecoder").apply { start() }
            decoderHandler = Handler(decoderThread!!.looper)
            
            // Create EGL context for texture sharing
            createEglContext()
            
            // Create output texture
            outputTextureId = textureRecycler.acquireTexture(
                type = GpuTextureRecycler.TextureType.EXTERNAL_OES,
                width = format.getInteger(MediaFormat.KEY_WIDTH),
                height = format.getInteger(MediaFormat.KEY_HEIGHT)
            )
            
            // Create framebuffer for rendering
            createFramebuffer(format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT))
            
            // Create Surface for MediaCodec
            surface = createSurfaceFromTexture(outputTextureId)
            
            // Create MediaCodec
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            mediaCodec = MediaCodec.createDecoderByType(mime)
            
            // Configure with Surface output
            mediaCodec?.configure(format, surface, null, 0)
            
            // Start decoder
            mediaCodec?.start()
            
            // Start frame processing
            startFrameProcessing()
            
            updateStats()
            return Result.success(surface!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ZeroCopyDecoder", e)
            release()
            return Result.failure(e)
        }
    }
    
    /**
     * Queue input data for decoding.
     * 
     * @param data Input data
     * @param offset Data offset
     * @param size Data size
     * @param presentationTimeUs Presentation timestamp
     * @param flags MediaCodec flags
     */
    fun queueInputBuffer(data: ByteBuffer, offset: Int, size: Int, presentationTimeUs: Long, flags: Int) {
        try {
            val inputIndex = mediaCodec?.dequeueInputBuffer(TIMEOUT_US) ?: return
            if (inputIndex >= 0) {
                val inputBuffer = mediaCodec?.getInputBuffer(inputIndex) ?: return
                inputBuffer.clear()
                inputBuffer.put(data)
                mediaCodec?.queueInputBuffer(inputIndex, offset, size, presentationTimeUs, flags)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue input buffer", e)
            errorCallback?.invoke(DecoderError.DECODE_ERROR)
        }
    }
    
    /**
     * Release all resources.
     */
    fun release() {
        shutdownFlag.set(true)
        
        // Stop frame processing
        scope.cancel()
        
        // Release MediaCodec
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaCodec", e)
        }
        mediaCodec = null
        
        // Release Surface
        surface?.release()
        surface = null
        
        // Release EGL resources
        releaseEglContext()
        
        // Release textures
        if (outputTextureId != 0) {
            textureRecycler.releaseTexture(outputTextureId)
            outputTextureId = 0
        }
        
        // Release framebuffer
        if (framebufferId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }
        
        if (renderbufferId != 0) {
            GLES20.glDeleteRenderbuffers(1, intArrayOf(renderbufferId), 0)
            renderbufferId = 0
        }
        
        // Stop decoder thread
        decoderHandler?.removeCallbacksAndMessages(null)
        decoderThread?.quitSafely()
        decoderThread = null
        decoderHandler = null
        
        // Clear callbacks
        frameAvailableCallback = null
        errorCallback = null
    }
    
    /**
     * Set callback for when a new frame is available.
     */
    fun setFrameAvailableCallback(callback: (Surface) -> Unit) {
        frameAvailableCallback = callback
    }
    
    /**
     * Set error callback.
     */
    fun setErrorCallback(callback: (DecoderError) -> Unit) {
        errorCallback = callback
    }
    
    /**
     * Get current decoder statistics.
     */
    fun getStats(): DecoderStats = _stats.value
    
    // ===== Private Implementation =====
    
    private fun createEglContext() {
        // Get default display
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Failed to get EGL display")
        }
        
        // Initialize EGL
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("Failed to initialize EGL")
        }
        
        // Configure EGL
        val configAttrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )
        
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, configs.size, numConfigs, 0)) {
            throw RuntimeException("Failed to choose EGL config")
        }
        
        // Create EGL context
        val contextAttrs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Failed to create EGL context")
        }
        
        // Create dummy surface
        val surfaceAttrs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttrs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Failed to create EGL surface")
        }
        
        // Make current
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("Failed to make EGL context current")
        }
    }
    
    private fun releaseEglContext() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSurfaceFromTexture(textureId: Int): Surface {
        // Create SurfaceTexture from texture ID
        val surfaceTexture = android.graphics.SurfaceTexture(textureId)
        surfaceTexture.setDefaultBufferSize(
            config.outputWidth.coerceAtLeast(1),
            config.outputHeight.coerceAtLeast(1)
        )
        
        // Create Surface from SurfaceTexture
        return Surface(surfaceTexture)
    }
    
    private fun createFramebuffer(width: Int, height: Int) {
        // Create framebuffer
        val framebuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, framebuffers, 0)
        framebufferId = framebuffers[0]
        
        // Create renderbuffer for depth/stencil
        val renderbuffers = IntArray(1)
        GLES20.glGenRenderbuffers(1, renderbuffers, 0)
        renderbufferId = renderbuffers[0]
        
        // Bind framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
        
        // Bind renderbuffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderbufferId)
        GLES20.glRenderbufferStorage(
            GLES20.GL_RENDERBUFFER,
            GLES20.GL_DEPTH_COMPONENT16,
            width, height
        )
        
        // Attach renderbuffer to framebuffer
        GLES20.glFramebufferRenderbuffer(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER,
            renderbufferId
        )
        
        // Check framebuffer status
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer incomplete: 0x${status.toString(16)}")
        }
        
        // Unbind
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }
    
    private fun startFrameProcessing() {
        scope.launch {
            while (!shutdownFlag.get()) {
                try {
                    // Dequeue output buffer
                    val bufferInfo = MediaCodec.BufferInfo()
                    val outputIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: continue
                    
                    if (outputIndex >= 0) {
                        // Process the frame
                        processOutputBuffer(outputIndex, bufferInfo)
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Format changed
                        val newFormat = mediaCodec?.outputFormat
                        Log.i(TAG, "Output format changed: $newFormat")
                    } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // No data available, try again
                        continue
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in frame processing loop", e)
                    errorCallback?.invoke(DecoderError.DECODE_ERROR)
                }
            }
        }
    }
    
    private fun processOutputBuffer(index: Int, bufferInfo: MediaCodec.BufferInfo) {
        val startTime = System.nanoTime()
        
        try {
            // Make EGL context current
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("Failed to make EGL context current")
            }
            
            // Bind framebuffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
            
            // Set viewport
            GLES20.glViewport(0, 0, config.outputWidth, config.outputHeight)
            
            // Clear framebuffer
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            
            // Render the texture
            renderTextureToFramebuffer()
            
            // Release output buffer
            mediaCodec?.releaseOutputBuffer(index, true)
            
            // Notify callback
            frameAvailableCallback?.invoke(surface!!)
            
            // Update stats
            val decodeTimeMs = (System.nanoTime() - startTime) / 1_000_000
            totalFramesDecoded.incrementAndGet()
            totalDecodeTimeMs.addAndGet(decodeTimeMs)
            currentDecodeTimeMs.set(decodeTimeMs)
            if (decodeTimeMs > peakDecodeTimeMs.get()) {
                peakDecodeTimeMs.set(decodeTimeMs)
            }
            
            updateStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process output buffer", e)
            errorCallback?.invoke(DecoderError.RENDER_ERROR)
        } finally {
            // Unbind framebuffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }
    }
    
    private fun renderTextureToFramebuffer() {
        // This would use a shader to render the external texture to the framebuffer
        // For now, just bind the texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, outputTextureId)
        
        // In a real implementation, we'd render a full-screen quad with the texture
        // For now, just mark as rendered
        totalFramesRendered.incrementAndGet()
    }
    
    private fun updateStats() {
        val avgDecodeTime = if (totalFramesDecoded.get() > 0) {
            totalDecodeTimeMs.get().toDouble() / totalFramesDecoded.get()
        } else {
            0.0
        }
        
        val avgRenderTime = if (totalFramesRendered.get() > 0) {
            totalRenderTimeMs.get().toDouble() / totalFramesRendered.get()
        } else {
            0.0
        }
        
        _stats.value = DecoderStats(
            totalFramesDecoded = totalFramesDecoded.get(),
            totalFramesRendered = totalFramesRendered.get(),
            totalDecodeTimeMs = totalDecodeTimeMs.get(),
            totalRenderTimeMs = totalRenderTimeMs.get(),
            peakDecodeTimeMs = peakDecodeTimeMs.get(),
            peakRenderTimeMs = peakRenderTimeMs.get(),
            currentDecodeTimeMs = currentDecodeTimeMs.get(),
            currentRenderTimeMs = currentRenderTimeMs.get(),
            averageDecodeTimeMs = avgDecodeTime
        )
    }
    
    // ===== Data Classes =====
    
    data class DecoderConfig(
        val outputWidth: Int = 1920,
        val outputHeight: Int = 1080,
        val maxFramesInFlight: Int = MAX_FRAMES_IN_FLIGHT,
        val enableHdr: Boolean = false,
        val enable10Bit: Boolean = false,
        val max4kFrameBuffers: Int = 3 // Minimum steady-decode frames for 4K
    ) {
        companion object {
            val DEFAULT = DecoderConfig()
        }
    }
    
    data class DecoderStats(
        val totalFramesDecoded: Long,
        val totalFramesRendered: Long,
        val totalDecodeTimeMs: Long,
        val totalRenderTimeMs: Long,
        val peakDecodeTimeMs: Long,
        val peakRenderTimeMs: Long,
        val currentDecodeTimeMs: Long,
        val currentRenderTimeMs: Long,
        val averageDecodeTimeMs: Double
    )
    
    enum class DecoderError {
        DECODE_ERROR,
        RENDER_ERROR,
        MEMORY_ERROR,
        FORMAT_ERROR
    }
    
    data class DecodedFrame(
        val textureId: Int,
        val width: Int,
        val height: Int,
        val timestampNs: Long,
        val presentationTimeUs: Long
    )
}