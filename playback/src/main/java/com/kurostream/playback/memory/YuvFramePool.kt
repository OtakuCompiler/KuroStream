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

package com.kurostream.playback.memory

import android.graphics.Bitmap
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class YuvFramePool @Inject constructor() {
    companion object {
        private const val TAG = "YuvFramePool"
        private const val MAX_POOL_SIZE = 8
        private const val MAX_4K_FRAMES = 3
        private const val YUV420_SIZE_FACTOR = 1.5 // Y + U + V = 1.5 bytes per pixel
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val framePools = ConcurrentHashMap<FrameKey, FramePool>()
    private val allocationPools = ConcurrentHashMap<AllocationKey, AllocationPool>()
    private val bitmapPools = ConcurrentHashMap<BitmapKey, BitmapPool>()
    private val totalAllocated = AtomicLong(0)
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)

    data class FrameKey(
        val width: Int,
        val height: Int,
        val format: Int, // ImageFormat.YUV_420_888 etc.
        val usage: Int // GRALLOC_USAGE_HW_VIDEO_ENCODER etc.
    )

    data class AllocationKey(
        val width: Int,
        val height: Int,
        val element: Element // RenderScript Element
    )

    data class BitmapKey(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config
    )

    data class FramePool(
        val key: FrameKey,
        private val frames: java.util.concurrent.ArrayDeque<YuvFrame> = java.util.concurrent.ArrayDeque(),
        val capacity: Int = MAX_POOL_SIZE
    ) {
        fun acquire(): YuvFrame {
            val frame = frames.pollLast()
            if (frame != null) {
                frame.reset()
                hitCount.incrementAndGet()
                return frame
            }
            missCount.incrementAndGet()
            return createNewFrame(key)
        }

        fun release(frame: YuvFrame) {
            if (frames.size < capacity && frame.isValid()) {
                frames.addLast(frame)
            } else {
                frame.close()
            }
        }

        fun getStats(): Map<String, Any> = mapOf(
            "width" to key.width,
            "height" to key.height,
            "format" to key.format,
            "poolSize" to frames.size,
            "capacity" to capacity,
            "frameSizeKB" to (key.width * key.height * YUV420_SIZE_FACTOR / 1024)
        )

        private fun createNewFrame(key: FrameKey): YuvFrame {
            val ySize = key.width * key.height
            val uvSize = ySize / 4
            val totalSize = ySize + uvSize * 2
            val buffer = ByteBuffer.allocateDirect(totalSize.toLong())
            buffer.order(java.nio.ByteOrder.nativeOrder())
            totalAllocated.addAndGet(totalSize.toLong())
            return YuvFrame(buffer, key.width, key.height, key.format)
        }

        fun clear() {
            frames.forEach { it.close() }
            frames.clear()
        }
    }

    data class AllocationPool(
        val key: AllocationKey,
        private val allocations: java.util.concurrent.ArrayDeque<Allocation> = java.util.concurrent.ArrayDeque(),
        val capacity: Int = MAX_POOL_SIZE
    ) {
        private var rs: RenderScript? = null

        fun initialize(renderScript: RenderScript) {
            rs = renderScript
        }

        fun acquire(): Allocation {
            val alloc = allocations.pollLast()
            if (alloc != null) {
                hitCount.incrementAndGet()
                return alloc
            }
            missCount.incrementAndGet()
            return createAllocation()
        }

        fun release(allocation: Allocation) {
            if (allocations.size < capacity) {
                allocations.addLast(allocation)
            } else {
                allocation.destroy()
            }
        }

        private fun createAllocation(): Allocation {
            val rsInstance = rs ?: throw IllegalStateException("RenderScript not initialized")
            val type = Type.Builder(rsInstance, key.element)
                .setX(key.width)
                .setY(key.height)
                .create()
            val allocation = Allocation.createTyped(rsInstance, type)
            val size = allocation.bytesSize.toLong()
            totalAllocated.addAndGet(size)
            return allocation
        }

        fun getStats(): Map<String, Any> = mapOf(
            "width" to key.width,
            "height" to key.height,
            "poolSize" to allocations.size,
            "capacity" to capacity
        )

        fun clear() {
            allocations.forEach { it.destroy() }
            allocations.clear()
        }
    }

    data class BitmapPool(
        val key: BitmapKey,
        private val bitmaps: java.util.concurrent.ArrayDeque<Bitmap> = java.util.concurrent.ArrayDeque(),
        val capacity: Int = MAX_POOL_SIZE
    ) {
        fun acquire(): Bitmap {
            val bitmap = bitmaps.pollLast()
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.eraseColor(0)
                hitCount.incrementAndGet()
                return bitmap
            }
            missCount.incrementAndGet()
            return Bitmap.createBitmap(key.width, key.height, key.config)
        }

        fun release(bitmap: Bitmap) {
            if (!bitmap.isRecycled && bitmaps.size < capacity) {
                bitmap.eraseColor(0)
                bitmaps.addLast(bitmap)
            } else if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }

        fun getStats(): Map<String, Any> = mapOf(
            "width" to key.width,
            "height" to key.height,
            "config" to key.config.name,
            "poolSize" to bitmaps.size,
            "capacity" to capacity
        )

        fun clear() {
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            bitmaps.clear()
        }
    }

    @Keep
    class YuvFrame(
        val buffer: ByteBuffer,
        val width: Int,
        val height: Int,
        val format: Int
    ) {
        private var valid = true
        private val yBuffer: ByteBuffer
        private val uBuffer: ByteBuffer
        private val vBuffer: ByteBuffer

        init {
            val ySize = width * height
            val uvSize = ySize / 4

            buffer.position(0)
            yBuffer = buffer.slice()
            yBuffer.limit(ySize)

            buffer.position(ySize)
            uBuffer = buffer.slice()
            uBuffer.limit(uvSize)

            buffer.position(ySize + uvSize)
            vBuffer = buffer.slice()
            vBuffer.limit(uvSize)

            buffer.position(0)
        }

        fun reset() {
            buffer.clear()
            yBuffer.clear()
            uBuffer.clear()
            vBuffer.clear()
            valid = true
        }

        fun isValid(): Boolean = valid && !buffer.isDirect || buffer.capacity() > 0

        fun getYPlane(): ByteBuffer = yBuffer.duplicate().apply { clear() }
        fun getUPlane(): ByteBuffer = uBuffer.duplicate().apply { clear() }
        fun getVPlane(): ByteBuffer = vBuffer.duplicate().apply { clear() }

        fun copyFrom(image: Image) {
            val planes = image.planes
            if (planes.size < 3) return

            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            copyPlane(yPlane.buffer, yBuffer, width, height, yPlane.pixelStride, yPlane.rowStride)
            copyPlane(uPlane.buffer, uBuffer, width / 2, height / 2, uPlane.pixelStride, uPlane.rowStride)
            copyPlane(vPlane.buffer, vBuffer, width / 2, height / 2, vPlane.pixelStride, vPlane.rowStride)
        }

        private fun copyPlane(src: ByteBuffer, dst: ByteBuffer, w: Int, h: Int, pixelStride: Int, rowStride: Int) {
            src.rewind()
            dst.clear()
            if (pixelStride == 1 && rowStride == w) {
                val size = w * h
                val limit = min(src.remaining(), size)
                dst.limit(limit)
                dst.put(src)
            } else {
                for (y in 0 until h) {
                    val rowSrcPos = y * rowStride
                    val rowDstPos = y * w
                    src.position(rowSrcPos)
                    dst.position(rowDstPos)
                    val rowLimit = min(w, src.remaining())
                    dst.limit(dst.position() + rowLimit)
                    dst.put(src)
                }
            }
        }

        fun close() {
            if (valid) {
                // ByteBuffer is direct, let GC handle it
                valid = false
            }
        }

        fun getSize(): Long = buffer.capacity().toLong()
    }

    fun initializeRenderScript(rs: RenderScript) {
        allocationPools.values.forEach { it.initialize(rs) }
    }

    fun acquireFrame(width: Int, height: Int, format: Int = android.graphics.ImageFormat.YUV_420_888, usage: Int = 0): YuvFrame {
        val key = FrameKey(width, height, format, usage)
        val pool = framePools.getOrPut(key) {
            FramePool(key, capacity = if (width >= 3840) MAX_4K_FRAMES else MAX_POOL_SIZE)
        }
        return pool.acquire()
    }

    fun releaseFrame(frame: YuvFrame) {
        val key = FrameKey(frame.width, frame.height, frame.format, 0)
        framePools[key]?.release(frame)
    }

    fun acquireAllocation(width: Int, height: Int, element: Element = Element.U8_4(null)): Allocation {
        val key = AllocationKey(width, height, element)
        val pool = allocationPools.getOrPut(key) { AllocationPool(key) }
        return pool.acquire()
    }

    fun releaseAllocation(allocation: Allocation) {
        val key = AllocationKey(allocation.type.x, allocation.type.y, allocation.element)
        allocationPools[key]?.release(allocation)
    }

    fun acquireBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val key = BitmapKey(width, height, config)
        val pool = bitmapPools.getOrPut(key) { BitmapPool(key) }
        return pool.acquire()
    }

    fun releaseBitmap(bitmap: Bitmap) {
        val key = BitmapKey(bitmap.width, bitmap.height, bitmap.config)
        bitmapPools[key]?.release(bitmap)
    }

    fun convertYuvToRgb(yuvFrame: YuvFrame, rs: RenderScript, outputAllocation: Allocation): Allocation {
        val yuvToRgb = android.renderscript.ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val inputAlloc = acquireAllocation(yuvFrame.width, yuvFrame.height, Element.YUV(rs))
        
        try {
            inputAlloc.copyFromUnchecked(yuvFrame.buffer)
            yuvToRgb.setInput(inputAlloc)
            yuvToRgb.forEach(outputAllocation)
            return outputAllocation
        } finally {
            releaseAllocation(inputAlloc)
        }
    }

    fun convertRgbToYuv(rgbAllocation: Allocation, width: Int, height: Int, rs: RenderScript): YuvFrame {
        val yuvFrame = acquireFrame(width, height)
        val yuvAlloc = acquireAllocation(width, height, Element.YUV(rs))
        
        try {
            val rgbToYuv = android.renderscript.ScriptIntrinsicYuvToRGB.create(rs, Element.YUV(rs))
            rgbToYuv.setInput(rgbAllocation)
            rgbToYuv.forEach(yuvAlloc)
            yuvAlloc.copyTo(yuvFrame.buffer)
            return yuvFrame
        } finally {
            releaseAllocation(yuvAlloc)
        }
    }

    fun getStats(): Map<String, Any> {
        val frameStats = framePools.values.map { it.getStats() }
        val allocStats = allocationPools.values.map { it.getStats() }
        val bitmapStats = bitmapPools.values.map { it.getStats() }
        
        val totalFrames = framePools.values.sumOf { it.frames.size }
        val totalAllocs = allocationPools.values.sumOf { it.allocations.size }
        val totalBitmaps = bitmapPools.values.sumOf { it.bitmaps.size }
        val totalRequests = hitCount.get() + missCount.get()
        val hitRate = if (totalRequests > 0) hitCount.get().toDouble() / totalRequests else 0.0

        return mapOf(
            "totalAllocatedMB" to totalAllocated.get() / 1024 / 1024,
            "framePools" to framePools.size,
            "totalFramesInPool" to totalFrames,
            "allocationPools" to allocationPools.size,
            "totalAllocationsInPool" to totalAllocs,
            "bitmapPools" to bitmapPools.size,
            "totalBitmapsInPool" to totalBitmaps,
            "hitRate" to String.format("%.2f%%", hitRate * 100),
            "frameDetails" to frameStats,
            "allocationDetails" to allocStats,
            "bitmapDetails" to bitmapStats
        )
    }

    fun trimMemory(level: Int) {
        val reduction = when (level) {
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> 0.25f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.5f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> 0.75f
            android.app.ActivityManager.TRIM_MEMORY_UI_HIDDEN -> 0.3f
            android.app.ActivityManager.TRIM_MEMORY_BACKGROUND -> 0.6f
            android.app.ActivityManager.TRIM_MEMORY_COMPLETE -> 0.9f
            else -> 0f
        }

        if (reduction > 0) {
            framePools.values.forEach { pool ->
                val toRemove = (pool.frames.size * reduction).toInt()
                repeat(toRemove) {
                    val frame = pool.frames.pollLast()
                    frame?.close()
                }
            }

            allocationPools.values.forEach { pool ->
                val toRemove = (pool.allocations.size * reduction).toInt()
                repeat(toRemove) {
                    val alloc = pool.allocations.pollLast()
                    alloc?.destroy()
                }
            }

            bitmapPools.values.forEach { pool ->
                val toRemove = (pool.bitmaps.size * reduction).toInt()
                repeat(toRemove) {
                    val bitmap = pool.bitmaps.pollLast()
                    if (bitmap != null && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    fun forceCleanup() {
        framePools.values.forEach { it.clear() }
        allocationPools.values.forEach { it.clear() }
        bitmapPools.values.forEach { it.clear() }
        framePools.clear()
        allocationPools.clear()
        bitmapPools.clear()
        totalAllocated.set(0)
        hitCount.set(0)
        missCount.set(0)
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        forceCleanup()
    }
}