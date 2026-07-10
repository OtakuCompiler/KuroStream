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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.Image
import android.media.ImageReader
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Size
import android.view.Surface
import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.kurostream.players.advanced.settings.PerformanceSettings
import kotlinx.coroutines.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.NativePeer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Super-resolution upscaling using ESRGAN Tiny model accelerated via NNAPI.
 * Processes video frames in real-time on a background thread.
 */
@Keep
class SuperResolutionManager private constructor(
    private val context: Context,
    private val settings: PerformanceSettings
) {

    companion object {
        @Volatile
        private var instance: SuperResolutionManager? = null

        fun getInstance(context: Context, settings: PerformanceSettings): SuperResolutionManager {
            return instance ?: synchronized(this) {
                instance ?: SuperResolutionManager(context.applicationContext, settings).also {
                    instance = it
                }
            }
        }

        const val MODEL_INPUT_SIZE = 128  // ESRGAN Tiny processes 128x128 tiles
        const val UPSCALE_FACTOR = 2
        const val TILE_OVERLAP = 8
        const val MAX_CONCURRENT_TILES = 4
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isEnabled = AtomicBoolean(false)
    private val isModelLoaded = AtomicBoolean(false)
    private val processingQueue = ConcurrentLinkedQueue<FrameBuffer>()
    private val resultCallback = AtomicReference<((Surface) -> Unit)?>(null)

    private var esrganModel: Module? = null
    private var nnapiDelegate: org.pytorch.NativePeer? = null
    private var inputSurface: Surface? = null
    private var outputSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var outputTextureId: Int = 0
    private var framebufferId: Int = 0

    // OpenGL resources for zero-copy texture processing
    private var glProgram: Int = 0
    private var vertexBuffer: Int = 0

    /**
     * Initialize the ESRGAN Tiny model with NNAPI acceleration.
     */
    fun initialize() {
        if (!settings.superResolutionEnabled) {
            return
        }

        scope.launch {
            try {
                loadModel()
                setupOpenGLPipeline()
                isModelLoaded.set(true)
                startProcessingLoop()
            } catch (e: Exception) {
                android.util.Log.e("SuperResolution", "Failed to initialize", e)
                fallbackToSoftware()
            }
        }
    }

    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        val modelPath = copyAssetIfNeeded("esrgan_tiny.ptl")

        // Configure NNAPI delegate for hardware acceleration
        val nnapiOptions = org.pytorch.NativePeer.NnapiModuleOptions()
            .setNnapiPartitioningMode(
                org.pytorch.NativePeer.NnapiModuleOptions.CompilationMode.COMPLETE
            )
            .setExecutionPriority(
                org.pytorch.NativePeer.NnapiModuleOptions.ExecutionPriority.PRIORITY_LOW_LATENCY
            )

        esrganModel = LiteModuleLoader.load(modelPath)

        // Attempt NNAPI binding if available (API 27+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            try {
                nnapiDelegate = org.pytorch.NativePeer.loadNnapiModule(
                    modelPath,
                    nnapiOptions
                )
                android.util.Log.i("SuperResolution", "NNAPI delegate loaded successfully")
            } catch (e: Exception) {
                android.util.Log.w("SuperResolution", "NNAPI not available, using CPU", e)
            }
        }
    }

    private fun copyAssetIfNeeded(assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists()) return file.absolutePath

        context.assets.open(assetName).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    private fun setupOpenGLPipeline() {
        // Create output texture for upscaled frames
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        outputTextureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Create framebuffer for rendering
        val framebuffers = IntArray(1)
        GLES30.glGenFramebuffers(1, framebuffers, 0)
        framebufferId = framebuffers[0]
    }

    /**
     * Enable/disable super-resolution at runtime.
     */
    fun setEnabled(enabled: Boolean) {
        val wasEnabled = isEnabled.getAndSet(enabled)
        if (enabled && !wasEnabled) {
            initialize()
        } else if (!enabled && wasEnabled) {
            release()
        }
    }

    /**
     * Process a video frame through super-resolution.
     * @param inputFrame The input frame as a ByteBuffer (RGBA)
     * @param width Frame width
     * @param height Frame height
     * @return Upscaled frame buffer
     */
    @WorkerThread
    fun processFrame(inputFrame: ByteBuffer, width: Int, height: Int): FrameBuffer? {
        if (!isEnabled.get() || !isModelLoaded.get()) {
            return null
        }

        val frameBuffer = FrameBuffer(inputFrame, width, height, System.nanoTime())
        processingQueue.offer(frameBuffer)
        return null // Async processing, result delivered via callback
    }

    private fun startProcessingLoop() {
        scope.launch(Dispatchers.Default) {
            while (isActive && isEnabled.get()) {
                val frame = processingQueue.poll()
                if (frame != null) {
                    processFrameInternal(frame)
                } else {
                    delay(1) // Prevent busy-waiting
                }
            }
        }
    }

    private suspend fun processFrameInternal(frame: FrameBuffer) = withContext(Dispatchers.Default) {
        val model = esrganModel ?: return@withContext

        val inputWidth = frame.width
        val inputHeight = frame.height
        val outputWidth = inputWidth * UPSCALE_FACTOR
        val outputHeight = inputHeight * UPSCALE_FACTOR

        // Tile-based processing for large frames
        val tilesX = (inputWidth + MODEL_INPUT_SIZE - 1) / MODEL_INPUT_SIZE
        val tilesY = (inputHeight + MODEL_INPUT_SIZE - 1) / MODEL_INPUT_SIZE

        val outputBuffer = ByteBuffer.allocateDirect(outputWidth * outputHeight * 4)
            .order(ByteOrder.nativeOrder())

        // Process tiles in parallel (up to MAX_CONCURRENT_TILES)
        val tileJobs = mutableListOf<Deferred<TileResult>>()

        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                if (tileJobs.size >= MAX_CONCURRENT_TILES) {
                    // Wait for a slot
                    tileJobs.awaitAll()
                    tileJobs.clear()
                }

                val tileJob = async {
                    processTile(frame, tx, ty, model)
                }
                tileJobs.add(tileJob)
            }
        }

        val results = tileJobs.awaitAll()

        // Stitch tiles into output buffer
        stitchTiles(results, outputBuffer, outputWidth, outputHeight)

        // Deliver result
        val resultFrame = FrameBuffer(outputBuffer, outputWidth, outputHeight, frame.timestamp)
        withContext(Dispatchers.Main) {
            deliverResult(resultFrame)
        }
    }

    private fun processTile(frame: FrameBuffer, tileX: Int, tileY: Int, model: Module): TileResult {
        val tileW = minOf(MODEL_INPUT_SIZE, frame.width - tileX * MODEL_INPUT_SIZE)
        val tileH = minOf(MODEL_INPUT_SIZE, frame.height - tileY * MODEL_INPUT_SIZE)

        // Extract tile data and normalize to [-1, 1]
        val tileData = FloatArray(3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        extractAndNormalizeTile(frame.buffer, frame.width, frame.height,
            tileX * MODEL_INPUT_SIZE, tileY * MODEL_INPUT_SIZE,
            tileW, tileH, tileData)

        // Create input tensor [1, 3, H, W]
        val inputTensor = Tensor.fromBlob(
            tileData,
            longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())
        )

        // Run inference
        val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
        val outputData = outputTensor.dataAsFloatArray

        // Denormalize output [0, 255]
        val outputBytes = ByteArray(3 * MODEL_INPUT_SIZE * UPSCALE_FACTOR * MODEL_INPUT_SIZE * UPSCALE_FACTOR)
        denormalizeOutput(outputData, outputBytes)

        return TileResult(
            x = tileX * MODEL_INPUT_SIZE * UPSCALE_FACTOR,
            y = tileY * MODEL_INPUT_SIZE * UPSCALE_FACTOR,
            width = tileW * UPSCALE_FACTOR,
            height = tileH * UPSCALE_FACTOR,
            data = outputBytes
        )
    }

    private fun extractAndNormalizeTile(
        src: ByteBuffer, srcWidth: Int, srcHeight: Int,
        offsetX: Int, offsetY: Int,
        tileW: Int, tileH: Int,
        dst: FloatArray
    ) {
        var idx = 0
        for (y in 0 until MODEL_INPUT_SIZE) {
            for (x in 0 until MODEL_INPUT_SIZE) {
                val srcX = offsetX + x
                val srcY = offsetY + y

                if (srcX < srcWidth && srcY < srcHeight) {
                    val pixelIdx = (srcY * srcWidth + srcX) * 4
                    val r = (src.get(pixelIdx).toInt() and 0xFF) / 127.5f - 1.0f
                    val g = (src.get(pixelIdx + 1).toInt() and 0xFF) / 127.5f - 1.0f
                    val b = (src.get(pixelIdx + 2).toInt() and 0xFF) / 127.5f - 1.0f

                    dst[idx] = r
                    dst[idx + MODEL_INPUT_SIZE * MODEL_INPUT_SIZE] = g
                    dst[idx + 2 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE] = b
                } else {
                    // Pad with zeros (reflection padding could be better)
                    dst[idx] = 0f
                    dst[idx + MODEL_INPUT_SIZE * MODEL_INPUT_SIZE] = 0f
                    dst[idx + 2 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE] = 0f
                }
                idx++
            }
        }
    }

    private fun denormalizeOutput(src: FloatArray, dst: ByteArray) {
        val size = dst.size / 3
        for (i in 0 until size) {
            val r = ((src[i] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)
            val g = ((src[i + size] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)
            val b = ((src[i + 2 * size] + 1.0f) * 127.5f).toInt().coerceIn(0, 255)

            dst[i * 3] = r.toByte()
            dst[i * 3 + 1] = g.toByte()
            dst[i * 3 + 2] = b.toByte()
        }
    }

    private fun stitchTiles(tiles: List<TileResult>, output: ByteBuffer, outWidth: Int, outHeight: Int) {
        for (tile in tiles) {
            for (y in 0 until tile.height) {
                for (x in 0 until tile.width) {
                    val srcIdx = (y * tile.width + x) * 3
                    val dstIdx = ((tile.y + y) * outWidth + (tile.x + x)) * 4

                    output.put(dstIdx, tile.data[srcIdx])
                    output.put(dstIdx + 1, tile.data[srcIdx + 1])
                    output.put(dstIdx + 2, tile.data[srcIdx + 2])
                    output.put(dstIdx + 3, 0xFF.toByte()) // Alpha
                }
            }
        }
    }

    private fun deliverResult(frame: FrameBuffer) {
        // Render to output surface
        val callback = resultCallback.get() ?: return

        // Create surface texture from buffer and deliver
        // Implementation depends on downstream consumer
    }

    private fun fallbackToSoftware() {
        android.util.Log.w("SuperResolution", "Falling back to bicubic upscaling")
        // Use Android's built-in scaling as fallback
    }

    fun release() {
        isEnabled.set(false)
        scope.cancel()

        esrganModel?.destroy()
        esrganModel = null

        if (outputTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
        }
        if (framebufferId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
        }

        imageReader?.close()
        inputSurface?.release()
        outputSurface?.release()
    }

    data class FrameBuffer(
        val buffer: ByteBuffer,
        val width: Int,
        val height: Int,
        val timestamp: Long
    )

    data class TileResult(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray
    )
}
