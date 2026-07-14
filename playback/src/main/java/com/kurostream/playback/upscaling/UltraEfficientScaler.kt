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

package com.kurostream.playback.upscaling

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.Image
import android.renderscript.*
import android.util.Log
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltraEfficientScaler {
    companion object {
        private const val TAG = "UltraEfficientScaler"
        private const val MAX_TEXTURE_SIZE = 4096
        private const val POOL_SIZE = 3
    }
    
    private var rs: RenderScript? = null
    private val scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB? = null
    private val scaleIntrinsic: ScriptIntrinsicBlur? = null
    private val bitmapPool = ArrayDeque<Bitmap>(POOL_SIZE)
    private val allocationPool = ArrayDeque<Allocation>(POOL_SIZE)
    private var currentRS: RenderScript? = null
    private var isInitialized = false
    
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        isFilterBitmap = true
        xfermode = android.graphics.PorterDuffMode.SRC_OVER
    }
    
    private val matrix = Matrix()
    
    data class ScaleConfig(
        val inputWidth: Int,
        val inputHeight: Int,
        val outputWidth: Int,
        val outputHeight: Int,
        val scaleType: ScaleType,
        val memoryBudget: Int
    )
    
    enum class ScaleType {
        BILINEAR,
        BICUBIC,
        LANCZOS,
        NEAREST_NEIGHBOR
    }
    
    fun initialize(memoryBudget: Int) {
        if (isInitialized) return
        
        try {
            rs = RenderScript.create(androidx.core.content.ContextCompat.RELEASE_SCRIPT)
            currentRS = rs
            isInitialized = true
            Log.d(TAG, "Initialized with ${memoryBudget / 1024 / 1024}MB budget")
        } catch (e: Exception) {
            Log.e(TAG, "RenderScript initialization failed, falling back to software", e)
            isInitialized = false
        }
    }
    
    fun scaleFrame(
        input: Image,
        outputWidth: Int,
        outputHeight: Int,
        config: ScaleConfig
    ): Bitmap? {
        if (!isInitialized) {
            return scaleSoftware(input, outputWidth, outputHeight, config.scaleType)
        }
        
        return try {
            scaleWithRenderScript(input, outputWidth, outputHeight, config)
        } catch (e: Exception) {
            Log.w(TAG, "RenderScript failed, falling back to software", e)
            scaleSoftware(input, outputWidth, outputHeight, config.scaleType)
        }
    }
    
    private fun scaleWithRenderScript(
        input: Image,
        outputWidth: Int,
        outputHeight: Int,
        config: ScaleConfig
    ): Bitmap? {
        val rs = currentRS ?: return scaleSoftware(input, outputWidth, outputHeight, config.scaleType)
        
        val inputAllocation = acquireAllocation(input.width, input.height)
        val outputAllocation = acquireAllocation(outputWidth, outputHeight)
        val outputBitmap = acquireBitmap(outputWidth, outputHeight)
        
        try {
            inputAllocation.copyFromUnchecked(input)
            
            val yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            yuvToRgb.setInput(inputAllocation)
            yuvToRgb.forEach(outputAllocation)
            
            if (config.scaleType == ScaleType.BILINEAR) {
                val scale = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                scale.setInput(outputAllocation)
                scale.setRadius(0.1f)
                scale.forEach(outputAllocation)
            }
            
            outputAllocation.copyTo(outputBitmap)
            return outputBitmap
        } finally {
            releaseAllocation(inputAllocation)
            releaseAllocation(outputAllocation)
            if (outputBitmap != null) {
                releaseBitmap(outputBitmap)
            }
        }
    }
    
    private fun scaleSoftware(
        input: Image,
        outputWidth: Int,
        outputHeight: Int,
        scaleType: ScaleType
    ): Bitmap? {
        val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(outputBitmap)
        canvas.drawColor(0)
        
        matrix.reset()
        matrix.setScale(outputWidth.toFloat() / input.width, outputHeight.toFloat() / input.height)
        
        when (scaleType) {
            ScaleType.BILINEAR -> {
                paint.isFilterBitmap = true
                paint.isAntiAlias = true
            }
            ScaleType.NEAREST_NEIGHBOR -> {
                paint.isFilterBitmap = false
                paint.isAntiAlias = false
            }
            else -> {
                paint.isFilterBitmap = true
                paint.isAntiAlias = true
            }
        }
        
        val inputBitmap = imageToBitmap(input)
        try {
            canvas.drawBitmap(inputBitmap, matrix, paint)
        } finally {
            inputBitmap?.recycle()
        }
        
        return outputBitmap
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        
        return if (rowPadding == 0) {
            bitmap
        } else {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            cropped
        }
    }
    
    private fun acquireAllocation(width: Int, height: Int): Allocation {
        synchronized(allocationPool) {
            val rs = currentRS ?: throw IllegalStateException("RenderScript not initialized")
            
            val existing = allocationPool.find { it.type.x == width && it.type.y == height }
            if (existing != null) {
                allocationPool.remove(existing)
                return existing
            }
            
            val type = Type.Builder(rs, Element.U8_4(rs)).setX(width).setY(height).create()
            return Allocation.createTyped(rs, type)
        }
    }
    
    private fun releaseAllocation(allocation: Allocation) {
        synchronized(allocationPool) {
            if (allocationPool.size < POOL_SIZE) {
                allocationPool.add(allocation)
            } else {
                allocation.destroy()
            }
        }
    }
    
    private fun acquireBitmap(width: Int, height: Int): Bitmap {
        synchronized(bitmapPool) {
            val existing = bitmapPool.find { it.width == width && it.height == height && !it.isRecycled }
            if (existing != null) {
                bitmapPool.remove(existing)
                existing.eraseColor(0)
                return existing
            }
            
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }
    
    private fun releaseBitmap(bitmap: Bitmap) {
        synchronized(bitmapPool) {
            if (!bitmap.isRecycled && bitmapPool.size < POOL_SIZE) {
                bitmap.eraseColor(0)
                bitmapPool.add(bitmap)
            } else if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    
    fun upscale1080pTo4K(input: Image): Bitmap? {
        val config = ScaleConfig(
            inputWidth = input.width,
            inputHeight = input.height,
            outputWidth = 3840,
            outputHeight = 2160,
            scaleType = ScaleType.LANCZOS,
            memoryBudget = 25 * 1024 * 1024
        )
        return scaleFrame(input, 3840, 2160, config)
    }
    
    fun cleanup() {
        synchronized(bitmapPool) {
            bitmapPool.forEach { it.recycle() }
            bitmapPool.clear()
        }
        
        synchronized(allocationPool) {
            allocationPool.forEach { it.destroy() }
            allocationPool.clear()
        }
        
        rs?.destroy()
        rs = null
        currentRS = null
        isInitialized = false
    }
    
    fun getMemoryUsage(): Int {
        var total = 0
        synchronized(bitmapPool) {
            total += bitmapPool.sumOf { it.allocationByteCount }
        }
        synchronized(allocationPool) {
            total += allocationPool.sumOf { it.bytesSize }
        }
        return total
    }
}