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

package com.kurostream.playback.render

import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * GPU Frame Buffer Recycling System
 * 
 * Manages OpenGL textures with aggressive recycling to minimize GPU memory usage.
 * Immediately deletes textures after rendering and reuses texture IDs to reduce
 * memory fragmentation and allocation overhead.
 * 
 * Target: <80MB RAM during 4K playback, 0% frame drops
 */
class GpuTextureRecycler {
    private val TAG = "GpuTextureRecycler"
    
    // Texture pool configuration
    private val MAX_POOL_SIZE = 8 // Max textures to keep in pool (was 32)
    private val MAX_TEXTURE_SIZE = 4096 // Max texture dimension (was 8K)
    
    // Texture types
    enum class TextureType {
        RGBA_8888, // Standard RGBA
        RGB_565,   // Lower memory RGB
        EXTERNAL_OES, // External texture (camera, video)
        FLOAT_16,  // HDR textures
        DEPTH     // Depth textures
    }
    
    // Texture pool per type
    private val texturePools = mutableMapOf<TextureType, ConcurrentLinkedQueue<Int>>()
    private val activeTextures = mutableMapOf<Int, TextureInfo>()
    private val textureLock = Any()
    
    // Statistics
    private val _stats = MutableStateFlow(TextureStats(0, 0, 0, 0, 0, 0, 0, 0))
    val stats: StateFlow<TextureStats> = _stats.asStateFlow()
    
    private val totalTexturesCreated = AtomicLong(0)
    private val totalTexturesRecycled = AtomicLong(0)
    private val totalTexturesDeleted = AtomicLong(0)
    private val peakActiveTextures = AtomicInteger(0)
    private val currentActiveTextures = AtomicInteger(0)
    
    // Scope for async cleanup
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cleanupChannel = Channel<Int>(Channel.UNLIMITED)
    private val shutdownFlag = AtomicBoolean(false)
    
    init {
        // Initialize pools for each texture type
        TextureType.values().forEach { type ->
            texturePools[type] = ConcurrentLinkedQueue()
        }
        
        // Start cleanup worker
        startCleanupWorker()
    }
    
    /**
     * Acquire a texture ID from the pool or create a new one.
     * 
     * @param type Texture type
     * @param width Texture width
     * @param height Texture height
     * @param format OpenGL format (e.g., GLES20.GL_RGBA)
     * @param internalFormat OpenGL internal format (e.g., GLES20.GL_RGBA8)
     * @param typeFormat OpenGL type (e.g., GLES20.GL_UNSIGNED_BYTE)
     * @return Texture ID
     */
    fun acquireTexture(
        type: TextureType = TextureType.RGBA_8888,
        width: Int = 0,
        height: Int = 0,
        format: Int = GLES20.GL_RGBA,
        internalFormat: Int = GLES20.GL_RGBA8,
        typeFormat: Int = GLES20.GL_UNSIGNED_BYTE
    ): Int {
        // Try to get from pool
        val pool = texturePools[type]!!
        var textureId = pool.poll()
        
        if (textureId != null) {
            // Reuse existing texture
            totalTexturesRecycled.incrementAndGet()
            
            // Update active textures
            synchronized(textureLock) {
                activeTextures[textureId] = TextureInfo(
                    type = type,
                    width = width,
                    height = height,
                    format = format,
                    internalFormat = internalFormat,
                    typeFormat = typeFormat,
                    timestamp = System.nanoTime()
                )
            }
        } else {
            // Create new texture
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            totalTexturesCreated.incrementAndGet()
            
            // Configure texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, internalFormat, 
                width, height, 0, format, typeFormat, null
            )
            
            // Set default parameters
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            
            // Update active textures
            synchronized(textureLock) {
                activeTextures[textureId] = TextureInfo(
                    type = type,
                    width = width,
                    height = height,
                    format = format,
                    internalFormat = internalFormat,
                    typeFormat = typeFormat,
                    timestamp = System.nanoTime()
                )
            }
        }
        
        // Update stats
        val current = currentActiveTextures.incrementAndGet()
        if (current > peakActiveTextures.get()) {
            peakActiveTextures.set(current)
        }
        updateStats()
        
        return textureId
    }
    
    /**
     * Release a texture back to the pool or delete it if pool is full.
     * 
     * @param textureId Texture ID to release
     * @param keepInPool Whether to keep in pool (default: true)
     */
    fun releaseTexture(textureId: Int, keepInPool: Boolean = true) {
        synchronized(textureLock) {
            val info = activeTextures.remove(textureId) ?: return
            
            if (keepInPool) {
                // Return to pool if there's space
                val pool = texturePools[info.type]!!
                if (pool.size < MAX_POOL_SIZE) {
                    pool.offer(textureId)
                } else {
                    // Pool full, schedule for deletion
                    scope.launch { cleanupChannel.send(textureId) }
                }
            } else {
                // Schedule for immediate deletion
                scope.launch { cleanupChannel.send(textureId) }
            }
            
            currentActiveTextures.decrementAndGet()
            updateStats()
        }
    }
    
    /**
     * Release all textures of a specific type.
     */
    fun releaseTexturesByType(type: TextureType) {
        synchronized(textureLock) {
            val toRemove = activeTextures.filter { it.value.type == type }.keys
            toRemove.forEach { textureId ->
                releaseTexture(textureId, keepInPool = false)
            }
        }
    }
    
    /**
     * Release all textures.
     */
    fun releaseAllTextures() {
        synchronized(textureLock) {
            activeTextures.keys.forEach { textureId ->
                releaseTexture(textureId, keepInPool = false)
            }
        }
    }
    
    /**
     * Delete a texture immediately (called from cleanup worker).
     */
    private fun deleteTexture(textureId: Int) {
        val textures = intArrayOf(textureId)
        GLES20.glDeleteTextures(1, textures, 0)
        totalTexturesDeleted.incrementAndGet()
        updateStats()
    }
    
    /**
     * Start the cleanup worker that deletes textures in the background.
     */
    private fun startCleanupWorker() {
        scope.launch {
            while (!shutdownFlag.get()) {
                val textureId = cleanupChannel.receive()
                deleteTexture(textureId)
            }
        }
    }
    
    /**
     * Shutdown the recycler and clean up all resources.
     */
    fun shutdown() {
        shutdownFlag.set(true)
        scope.cancel()
        
        // Delete all textures
        releaseAllTextures()
        
        // Clear all pools
        texturePools.values.forEach { pool ->
            while (pool.isNotEmpty()) {
                val textureId = pool.poll()
                if (textureId != null) {
                    deleteTexture(textureId)
                }
            }
        }
    }
    
    /**
     * Update texture parameters (e.g., after resize).
     */
    fun updateTextureParameters(
        textureId: Int,
        width: Int,
        height: Int,
        format: Int = GLES20.GL_RGBA,
        internalFormat: Int = GLES20.GL_RGBA8,
        typeFormat: Int = GLES20.GL_UNSIGNED_BYTE
    ) {
        synchronized(textureLock) {
            val info = activeTextures[textureId] ?: return
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, internalFormat, 
                width, height, 0, format, typeFormat, null
            )
            
            // Update info
            activeTextures[textureId] = info.copy(
                width = width,
                height = height,
                format = format,
                internalFormat = internalFormat,
                typeFormat = typeFormat
            )
        }
    }
    
    /**
     * Get texture information.
     */
    fun getTextureInfo(textureId: Int): TextureInfo? {
        synchronized(textureLock) {
            return activeTextures[textureId]
        }
    }
    
    /**
     * Get total GPU memory used by textures (estimated).
     */
    fun getEstimatedGpuMemoryUsage(): Long {
        synchronized(textureLock) {
            return activeTextures.values.sumOf { info ->
                val bytesPerPixel = when (info.internalFormat) {
                    GLES20.GL_RGBA8 -> 4
                    GLES20.GL_RGB565 -> 2
                    GLES30.GL_RGBA16F -> 8
                    else -> 4 // Default to 4 bytes per pixel
                }
                (info.width * info.height * bytesPerPixel).toLong()
            }
        }
    }
    
    /**
     * Update statistics.
     */
    private fun updateStats() {
        _stats.value = TextureStats(
            totalCreated = totalTexturesCreated.get(),
            totalRecycled = totalTexturesRecycled.get(),
            totalDeleted = totalTexturesDeleted.get(),
            peakActive = peakActiveTextures.get(),
            currentActive = currentActiveTextures.get(),
            poolSize = texturePools.values.sumOf { it.size },
            estimatedGpuMemoryBytes = getEstimatedGpuMemoryUsage(),
            poolSizes = texturePools.mapValues { it.value.size }
        )
    }
    
    /**
     * Texture information.
     */
    data class TextureInfo(
        val type: TextureType,
        val width: Int,
        val height: Int,
        val format: Int,
        val internalFormat: Int,
        val typeFormat: Int,
        val timestamp: Long
    )
    
    /**
     * Texture statistics.
     */
    data class TextureStats(
        val totalCreated: Long,
        val totalRecycled: Long,
        val totalDeleted: Long,
        val peakActive: Int,
        val currentActive: Int,
        val poolSize: Int,
        val estimatedGpuMemoryBytes: Long,
        val poolSizes: Map<TextureType, Int>
    ) {
        val estimatedGpuMemoryMb: Double
            get() = estimatedGpuMemoryBytes.toDouble() / (1024 * 1024)
    }
    
    /**
     * Utility function to check for OpenGL errors.
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError 0x${error.toString(16)}")
        }
    }
}