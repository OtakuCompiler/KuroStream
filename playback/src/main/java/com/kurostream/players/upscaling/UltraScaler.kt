package com.kurostream.players.upscaling

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicYuvToRGB
import androidx.annotation.IntRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ultra-efficient image upscaler with object pooling.
 * Uses RenderScript for hardware acceleration and paint recycling.
 */
@Singleton
class UltraScaler @Inject constructor() {

    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var yuvScript: ScriptIntrinsicYuvToRGB? = null

    @Volatile private var isInitialized = false
    private val paintPool = ArrayBlockingQueue<Paint>(5)

    suspend fun initialize(context: android.content.Context) = withContext(Dispatchers.Default) {
        if (isInitialized) return@withContext
        try {
            renderScript = RenderScript.create(context.applicationContext)
            blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
            yuvScript = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
            
            // Pre-allocate paint objects for pooling
            repeat(5) {
                paintPool.offer(Paint().apply {
                    isFiltering = true
                    isAntiAlias = true
                    flags = Paint.FILTER_BITMAP_FLAG
                })
            }
            
            isInitialized = true
            Timber.d("UltraScaler initialized (RAM: ~8MB)")
        } catch (e: Exception) {
            Timber.e(e, "UltraScaler init failed")
        }
    }

    suspend fun upscale(
        bitmap: Bitmap,
        @IntRange(from = 1, to = 4) scale: Int = 2,
        @IntRange(from = 0, to = 100) quality: Int = 85
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!isInitialized) return@withContext bitmap

        val width = bitmap.width * scale
        val height = bitmap.height * scale

        val config = when {
            quality >= 90 -> Bitmap.Config.ARGB_8888
            quality >= 70 -> Bitmap.Config.RGB_565
            else -> Bitmap.Config.RGB_565
        }

        val output = Bitmap.createBitmap(width, height, config)
        val paint = paintPool.poll() ?: Paint().apply {
            isFiltering = true
            isAntiAlias = true
            flags = Paint.FILTER_BITMAP_FLAG
        }

        try {
            val canvas = Canvas(output)
            canvas.drawBitmap(bitmap, android.graphics.Rect(0, 0, bitmap.width, bitmap.height),
                android.graphics.Rect(0, 0, width, height), paint)

            if (quality >= 80) {
                applySharpen(output, 0.15f)
            }

            Timber.d("Upscaled ${bitmap.width}x${bitmap.height} → ${width}x${height} (RAM: ${output.allocationByteCount / 1024}KB)")
            output
        } finally {
            // Return paint to pool
            paint.reset()
            paint.isFiltering = true
            paint.isAntiAlias = true
            paint.flags = Paint.FILTER_BITMAP_FLAG
            paintPool.offer(paint)
        }
    }

    private suspend fun applySharpen(bitmap: Bitmap, strength: Float) = withContext(Dispatchers.Default) {
        val rs = renderScript ?: return@withContext
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)

        try {
            val kernel = android.renderscript.RenderScript.RScriptC(rs, sharpenKernel)
            kernel.forEach(input, output)
            output.copyTo(bitmap)
        } catch (_: Exception) {
        } finally {
            input.destroy()
            output.destroy()
        }
    }

    fun release() {
        blurScript?.destroy()
        yuvScript?.destroy()
        renderScript?.destroy()
        paintPool.clear()
        isInitialized = false
        Timber.d("UltraScaler released")
    }

    private val sharpenKernel = """
        #pragma version(1)
        #pragma rs java_package_name=com.kurostream.players.upscaling

        rs_script_allocation_t input;
        int width;
        int height;

        uchar4 __attribute__((kernel)) root(int x, int y) {
            float kernel[9] = {0, -0.1, 0, -0.1, 1.4, -0.1, 0, -0.1, 0};
            float4 result = (float4)(0);
            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int sx = clamp(x + kx, 0, width - 1);
                    int sy = clamp(y + ky, 0, height - 1);
                    float4 pixel = rsUnpackColor8888(rsGetElementAt_uchar4(input, sx, sy));
                    result += pixel * kernel[(ky + 1) * 3 + (kx + 1)];
                }
            }
            return convert_uchar4(clamp(result, 0.0f, 255.0f));
        }
    """.trimIndent()
}