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

package com.kurostream.common.pool

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * UI Bitmap Pool for aggressive recycling of small UI bitmaps.
 * 
 * Maintains a pool of 4MB maximum for UI bitmaps (posters, thumbnails, icons).
 * Recycles bitmaps aggressively to minimize GC pressure and memory usage.
 * 
 * Target: <45MB idle RAM, <55MB during 1080p streaming
 */
class UiBitmapPool {
    private val TAG = "UiBitmapPool"
    
    // Pool configuration
    private val MAX_POOL_SIZE_BYTES = 4 * 1024 * 1024 // 4MB max
    private val MAX_BITMAP_SIZE = 1024 * 1024 // 1MB max per bitmap
    private val MAX_POOL_SIZE_COUNT = 32 // Max bitmaps in pool
    
    // Bitmap configurations
    private val configs = listOf(
        Bitmap.Config.ARGB_8888,
        Bitmap.Config.RGB_565,
        Bitmap.Config.ALPHA_8
    )
    
    // Pools per configuration
    private val pools = mutableMapOf<Bitmap.Config, ConcurrentLinkedQueue<Bitmap>>()
    private val activeBitmaps = mutableMapOf<Int, BitmapInfo>()
    private val bitmapLock = Any()
    
    // Statistics
    private val _stats = MutableStateFlow(BitmapPoolStats(0, 0, 0, 0, 0, 0, 0, 0.0))
    val stats: StateFlow<BitmapPoolStats> = _stats.asStateFlow()
    
    private val totalBitmapsCreated = AtomicLong(0)
    private val totalBitmapsRecycled = AtomicLong(0)
    private val totalBitmapsDeleted = AtomicLong(0)
    private val peakActiveBitmaps = AtomicInteger(0)
    private val currentActiveBitmaps = AtomicInteger(0)
    private val currentPoolSizeBytes = AtomicLong(0)
    
    // Scope for async cleanup
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cleanupChannel = Channel<Bitmap>(Channel.UNLIMITED)
    private val shutdownFlag = AtomicBoolean(false)
    
    init {
        // Initialize pools for each config
        configs.forEach { config ->
            pools[config] = ConcurrentLinkedQueue()
        }
        
        // Start cleanup worker
        startCleanupWorker()
    }
    
    /**
     * Acquire a bitmap from the pool or create a new one.
     * 
     * @param width Desired width
     * @param height Desired height
     * @param config Desired bitmap config
     * @return Bitmap from pool or newly created
     */
    fun acquireBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val sizeBytes = calculateBitmapSize(width, height, config)
        
        // Try to get from pool
        val pool = pools[config]!!
        var bitmap = pool.poll()
        
        if (bitmap != null) {
            // Reuse existing bitmap
            totalBitmapsRecycled.incrementAndGet()
            
            // Resize if needed
            if (bitmap.width != width || bitmap.height != height) {
                bitmap.reconfigure(width, height, config)
            }
            
            // Update active bitmaps
            synchronized(bitmapLock) {
                activeBitmaps[bitmap.hashCode()] = BitmapInfo(
                    width = width,
                    height = height,
                    config = config,
                    sizeBytes = sizeBytes,
                    timestamp = System.nanoTime()
                )
            }
        } else {
            // Create new bitmap
            bitmap = Bitmap.createBitmap(width, height, config)
            totalBitmapsCreated.incrementAndGet()
            
            // Update active bitmaps
            synchronized(bitmapLock) {
                activeBitmaps[bitmap.hashCode()] = BitmapInfo(
                    width = width,
                    height = height,
                    config = config,
                    sizeBytes = sizeBytes,
                    timestamp = System.nanoTime()
                )
            }
        }
        
        // Update stats
        val current = currentActiveBitmaps.incrementAndGet()
        if (current > peakActiveBitmaps.get()) {
            peakActiveBitmaps.set(current)
        }
        updateStats()
        
        return bitmap
    }
    
    /**
     * Release a bitmap back to the pool.
     * 
     * @param bitmap Bitmap to release
     * @param keepInPool Whether to keep in pool (default: true)
     */
    fun releaseBitmap(bitmap: Bitmap, keepInPool: Boolean = true) {
        if (bitmap.isRecycled) return
        
        synchronized(bitmapLock) {
            val info = activeBitmaps.remove(bitmap.hashCode()) ?: return
            
            if (keepInPool) {
                // Return to pool if there's space
                val pool = pools[info.config]!!
                val sizeBytes = info.sizeBytes
                
                if (pool.size < MAX_POOL_SIZE_COUNT && 
                    currentPoolSizeBytes.get() + sizeBytes <= MAX_POOL_SIZE_BYTES) {
                    // Clear bitmap content
                    bitmap.eraseColor(0)
                    pool.offer(bitmap)
                    currentPoolSizeBytes.addAndGet(sizeBytes)
                } else {
                    // Pool full, schedule for deletion
                    scope.launch { cleanupChannel.send(bitmap) }
                }
            } else {
                // Schedule for immediate deletion
                scope.launch { cleanupChannel.send(bitmap) }
            }
            
            currentActiveBitmaps.decrementAndGet()
            updateStats()
        }
    }
    
    /**
     * Decode a bitmap from bytes with pooling.
     * 
     * @param bytes Image data
     * @param offset Offset in bytes
     * @param length Length of data
     * @param options BitmapFactory options
     * @return Decoded bitmap from pool or newly created
     */
    fun decodeBitmap(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size, options: BitmapFactory.Options? = null): Bitmap {
        val opts = options ?: BitmapFactory.Options().apply {
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        // Try to decode with inBitmap from pool
        val pool = pools[opts.inPreferredConfig]!!
        if (pool.isNotEmpty()) {
            opts.inBitmap = pool.peek()
        }
        
        try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, offset, length, opts)
            
            // Update active bitmaps
            val sizeBytes = calculateBitmapSize(bitmap.width, bitmap.height, bitmap.config)
            synchronized(bitmapLock) {
                activeBitmaps[bitmap.hashCode()] = BitmapInfo(
                    width = bitmap.width,
                    height = bitmap.height,
                    config = bitmap.config,
                    sizeBytes = sizeBytes,
                    timestamp = System.nanoTime()
                )
            }
            
            // Update stats
            currentActiveBitmaps.incrementAndGet()
            if (opts.inBitmap != null) {
                // Remove from pool since it was used
                pool.poll()
                currentPoolSizeBytes.addAndGet(-sizeBytes)
            }
            
            updateStats()
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap with pool", e)
            // Fallback to normal decode
            return BitmapFactory.decodeByteArray(bytes, offset, length)
        }
    }
    
    /**
     * Release all bitmaps of a specific configuration.
     */
    fun releaseBitmapsByConfig(config: Bitmap.Config) {
        synchronized(bitmapLock) {
            val toRemove = activeBitmaps.filter { it.value.config == config }.keys
            toRemove.forEach { hashCode ->
                val bitmap = activeBitmaps[hashCode]?.let { findBitmapByInfo(it) }
                bitmap?.let { releaseBitmap(it, keepInPool = false) }
            }
        }
    }
    
    /**
     * Release all bitmaps.
     */
    fun releaseAllBitmaps() {
        synchronized(bitmapLock) {
            activeBitmaps.keys.forEach { hashCode ->
                val bitmap = activeBitmaps[hashCode]?.let { findBitmapByInfo(it) }
                bitmap?.let { releaseBitmap(it, keepInPool = false) }
            }
        }
    }
    
    /**
     * Delete a bitmap immediately (called from cleanup worker).
     */
    private fun deleteBitmap(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
            totalBitmapsDeleted.incrementAndGet()
            updateStats()
        }
    }
    
    /**
     * Start the cleanup worker that recycles bitmaps in the background.
     */
    private fun startCleanupWorker() {
        scope.launch {
            while (!shutdownFlag.get()) {
                val bitmap = cleanupChannel.receive()
                deleteBitmap(bitmap)
            }
        }
    }
    
    /**
     * Shutdown the pool and clean up all resources.
     */
    fun shutdown() {
        shutdownFlag.set(true)
        scope.cancel()
        
        // Delete all bitmaps
        releaseAllBitmaps()
        
        // Clear all pools
        pools.values.forEach { pool ->
            while (pool.isNotEmpty()) {
                val bitmap = pool.poll()
                if (bitmap != null) {
                    deleteBitmap(bitmap)
                }
            }
        }
        
        currentPoolSizeBytes.set(0)
    }
    
    /**
     * Get current pool statistics.
     */
    fun getStats(): BitmapPoolStats = _stats.value
    
    /**
     * Get total memory used by active bitmaps.
     */
    fun getActiveMemoryUsage(): Long {
        synchronized(bitmapLock) {
            return activeBitmaps.values.sumOf { it.sizeBytes }
        }
    }
    
    /**
     * Get total memory used by pooled bitmaps.
     */
    fun getPooledMemoryUsage(): Long = currentPoolSizeBytes.get()
    
    /**
     * Update statistics.
     */
    private fun updateStats() {
        _stats.value = BitmapPoolStats(
            totalCreated = totalBitmapsCreated.get(),
            totalRecycled = totalBitmapsRecycled.get(),
            totalDeleted = totalBitmapsDeleted.get(),
            peakActive = peakActiveBitmaps.get(),
            currentActive = currentActiveBitmaps.get(),
            poolSize = pools.values.sumOf { it.size },
            poolSizeBytes = currentPoolSizeBytes.get(),
            activeMemoryUsageMb = getActiveMemoryUsage().toDouble() / (1024 * 1024)
        )
    }
    
    /**
     * Calculate bitmap size in bytes.
     */
    private fun calculateBitmapSize(width: Int, height: Int, config: Bitmap.Config): Long {
        return when (config) {
            Bitmap.Config.ALPHA_8 -> (width * height).toLong()
            Bitmap.Config.RGB_565 -> (width * height * 2).toLong()
            Bitmap.Config.ARGB_4444 -> (width * height * 2).toLong()
            Bitmap.Config.ARGB_8888 -> (width * height * 4).toLong()
            Bitmap.Config.RGBA_F16 -> (width * height * 8).toLong()
            else -> (width * height * 4).toLong() // Default to ARGB_8888
        }
    }
    
    /**
     * Find a bitmap by its info (for cleanup).
     */
    private fun findBitmapByInfo(info: BitmapInfo): Bitmap? {
        synchronized(bitmapLock) {
            return activeBitmaps.entries.find { it.value == info }?.let { entry ->
                // This is a placeholder - in reality we'd need to track bitmaps by reference
                // For now, return null to force recreation
                null
            }
        }
    }
    
    // ===== Data Classes =====
    
    data class BitmapInfo(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config,
        val sizeBytes: Long,
        val timestamp: Long
    )
    
    data class BitmapPoolStats(
        val totalCreated: Long,
        val totalRecycled: Long,
        val totalDeleted: Long,
        val peakActive: Int,
        val currentActive: Int,
        val poolSize: Int,
        val poolSizeBytes: Long,
        val activeMemoryUsageMb: Double
    ) {
        val poolMemoryUsageMb: Double
            get() = poolSizeBytes.toDouble() / (1024 * 1024)
    }
}