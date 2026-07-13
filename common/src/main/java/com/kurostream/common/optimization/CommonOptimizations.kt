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

package com.kurostream.common.optimization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Texture Atlas for UI - Optimization #11
 * 
 * Combines small UI images into a single texture to reduce draw calls.
 * 
 * Target: 60fps UI, reduced GPU overhead
 */
class UiTextureAtlas(
    private val maxSize: Int = 1024
) {
    private val TAG = "UiTextureAtlas"
    private val atlasBitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.RGB_565)
    private val canvas = Canvas(atlasBitmap)
    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    
    private var currentX = 0
    private var currentY = 0
    private var rowHeight = 0
    private val padding = 2
    
    private val regions = ConcurrentHashMap<String, AtlasRegion>()
    private val bitmapCache = LruCache<String, Bitmap>(50)
    
    data class AtlasRegion(
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val u1: Float,
        val v1: Float,
        val u2: Float,
        val v2: Float
    )
    
    fun addBitmap(name: String, bitmap: Bitmap): Boolean {
        val paddedWidth = bitmap.width + padding * 2
        val paddedHeight = bitmap.height + padding * 2
        
        if (currentX + paddedWidth > maxSize) {
            currentX = 0
            currentY += rowHeight + padding
            rowHeight = 0
        }
        
        if (currentY + paddedHeight > maxSize) {
            Log.w(TAG, "Atlas full, cannot add $name")
            return false
        }
        
        canvas.drawBitmap(bitmap, (currentX + padding).toFloat(), (currentY + padding).toFloat(), paint)
        
        val region = AtlasRegion(
            name = name,
            x = currentX + padding,
            y = currentY + padding,
            width = bitmap.width,
            height = bitmap.height,
            u1 = (currentX + padding).toFloat() / maxSize,
            v1 = (currentY + padding).toFloat() / maxSize,
            u2 = (currentX + padding + bitmap.width).toFloat() / maxSize,
            v2 = (currentY + padding + bitmap.height).toFloat() / maxSize
        )
        
        regions[name] = region
        currentX += paddedWidth
        rowHeight = maxOf(rowHeight, paddedHeight)
        
        bitmapCache.put(name, bitmap)
        return true
    }
    
    fun getRegion(name: String): AtlasRegion? = regions[name]
    
    fun getAtlasBitmap(): Bitmap = atlasBitmap
    
    fun clear() {
        regions.clear()
        bitmapCache.evictAll()
        canvas.drawColor(0)
        currentX = 0
        currentY = 0
        rowHeight = 0
    }
}

/**
 * Compose Render-Thread Offloading - Optimization #18
 * 
 * Moves expensive composable computations to background thread
 * using SideEffect + LaunchedEffect pattern.
 * 
 * Target: 60fps UI, no jank during heavy compositions
 */
class ComposeRenderOffloader {
    
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val computeCache = ConcurrentHashMap<String, Any>()
    
    @Composable
    fun <T> offloadComputation(
        key: String,
        compute: () -> T,
        onResult: (T) -> Unit
    ) {
        var result by remember { mutableStateOf<T?>(null) }
        var computing by remember { mutableStateOf(false) }
        
        LaunchedEffect(key) {
            if (computeCache.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                onResult(computeCache[key] as T)
                return@LaunchedEffect
            }
            
            computing = true
            backgroundScope.launch {
                val computed = compute()
                computeCache[key] = computed as Any
                backgroundScope.launch(Dispatchers.Main) {
                    result = computed
                    computing = false
                    onResult(computed)
                }
            }
        }
        
        SideEffect {
            if (result != null && !computing) {
                onResult(result!!)
            }
        }
    }
    
    fun clearCache() {
        computeCache.clear()
    }
    
    fun invalidateKey(key: String) {
        computeCache.remove(key)
    }
}

/**
 * File System Alignment - Optimization #14
 * 
 * Ensures all file writes are aligned to 4KB block boundaries
 * to minimize I/O overhead on flash storage.
 * 
 * Target: <50ms seek latency, reduced write amplification
 */
class AlignedFileWriter(
    private val blockSize: Int = 4096
) {
    private val TAG = "AlignedFileWriter"
    private val writeExecutor = Executors.newSingleThreadExecutor()
    
    fun writeAligned(file: java.io.File, data: ByteArray, offset: Long = 0) {
        val alignedOffset = (offset / blockSize) * blockSize
        val alignedLength = ((data.size + (offset - alignedOffset) + blockSize - 1) / blockSize) * blockSize
        
        val alignedData = ByteArray(alignedLength.toInt())
        data.copyInto(alignedData, (offset - alignedOffset).toInt())
        
        writeExecutor.execute {
            try {
                file.parentFile?.mkdirs()
                val raf = java.io.RandomAccessFile(file, "rw")
                raf.channel.position(alignedOffset)
                raf.channel.write(java.nio.ByteBuffer.wrap(alignedData))
                raf.close()
            } catch (e: Exception) {
                Log.e(TAG, "Aligned write failed", e)
            }
        }
    }
    
    fun writeAlignedBlocking(file: java.io.File, data: ByteArray, offset: Long = 0) {
        val alignedOffset = (offset / blockSize) * blockSize
        val alignedLength = ((data.size + (offset - alignedOffset) + blockSize - 1) / blockSize) * blockSize
        
        val alignedData = ByteArray(alignedLength.toInt())
        data.copyInto(alignedData, (offset - alignedOffset).toInt())
        
        try {
            file.parentFile?.mkdirs()
            val raf = java.io.RandomAccessFile(file, "rw")
            raf.channel.position(alignedOffset)
            raf.channel.write(java.nio.ByteBuffer.wrap(alignedData))
            raf.close()
        } catch (e: Exception) {
            Log.e(TAG, "Aligned write failed", e)
        }
    }
    
    fun shutdown() {
        writeExecutor.shutdown()
    }
}

/**
 * Content-Adaptive Bitrate Ladder - Optimization #19
 * 
 * Generates custom ABR ladder per anime based on content complexity
 * using pre-computed database, avoiding unnecessary high-bitrate downloads.
 * 
 * Target: <80MB RAM, reduced bandwidth usage
 */
class AdaptiveBitrateLadder {
    
    private val ladderCache = ConcurrentHashMap<String, BitrateLadder>()
    private val complexityDb = ConcurrentHashMap<String, ContentComplexity>()
    
    data class BitrateLadder(
        val contentId: String,
        val rungs: List<BitrateRung>,
        val defaultRung: Int
    )
    
    data class BitrateRung(
        val bitrateBps: Long,
        val resolution: String,
        val fps: Float,
        val codec: String,
        val estimatedQuality: Float // 0-1 VMAF score
    )
    
    data class ContentComplexity(
        val contentId: String,
        val spatialComplexity: Float, // 0-1
        val temporalComplexity: Float, // 0-1
        val recommendedMaxBitrate: Long,
        val optimalRungs: List<BitrateRung>
    )
    
    fun getLadder(contentId: String, availableBitrates: List<Long>): BitrateLadder {
        return ladderCache.computeIfAbsent(contentId) { 
            generateLadder(contentId, availableBitrates) 
        }
    }
    
    private fun generateLadder(contentId: String, availableBitrates: List<Long>): BitrateLadder {
        val complexity = complexityDb[contentId] ?: estimateComplexity(contentId)
        
        val rungs = availableBitrates
            .filter { it <= complexity.recommendedMaxBitrate }
            .sortedDescending()
            .mapIndexed { index, bitrate ->
                val resolution = when {
                    bitrate >= 20_000_000 -> "3840x2160"
                    bitrate >= 10_000_000 -> "2560x1440"
                    bitrate >= 5_000_000 -> "1920x1080"
                    bitrate >= 2_500_000 -> "1280x720"
                    else -> "854x480"
                }
                
                BitrateRung(
                    bitrateBps = bitrate,
                    resolution = resolution,
                    fps = 23.976f,
                    codec = "av1",
                    estimatedQuality = estimateQuality(bitrate, complexity)
                )
            }
        
        // Find default rung (highest quality under 80% of max bitrate)
        val defaultRung = rungs.indexOfFirst { it.bitrateBps <= complexity.recommendedMaxBitrate * 0.8 }
        
        return BitrateLadder(
            contentId = contentId,
            rungs = rungs,
            defaultRung = if (defaultRung >= 0) defaultRung else 0
        )
    }
    
    private fun estimateComplexity(contentId: String): ContentComplexity {
        // In production: query pre-computed database
        // For now: return defaults based on content type
        return ContentComplexity(
            contentId = contentId,
            spatialComplexity = 0.6f,
            temporalComplexity = 0.5f,
            recommendedMaxBitrate = 15_000_000,
            optimalRungs = emptyList()
        )
    }
    
    private fun estimateQuality(bitrate: Long, complexity: ContentComplexity): Float {
        // Simplified VMAF estimation
        val spatialFactor = 1.0 - complexity.spatialComplexity * 0.3
        val temporalFactor = 1.0 - complexity.temporalComplexity * 0.2
        val bitrateFactor = minOf(1.0, bitrate.toDouble() / 20_000_000)
        return (spatialFactor * temporalFactor * bitrateFactor * 100).toFloat()
    }
    
    fun setComplexity(contentId: String, complexity: ContentComplexity) {
        complexityDb[contentId] = complexity
        ladderCache.remove(contentId) // Invalidate cache
    }
}

/**
 * Global Metadata Deduplication - Optimization #20
 * 
 * Caches metadata globally across all providers so same anime
 * appears only once in unified database.
 * 
 * Target: Reduced storage, faster sync, <80MB RAM
 */
class GlobalMetadataDeduplicator {
    private val TAG = "GlobalMetadataDeduplicator"
    private val metadataCache = ConcurrentHashMap<String, UnifiedMetadata>()
    private val providerMapping = ConcurrentHashMap<String, MutableSet<String>>()
    private val canonicalIds = ConcurrentHashMap<String, String>()
    private val accessTime = ConcurrentHashMap<String, Long>()
    private val MAX_CACHE_SIZE = 10000
    
    data class UnifiedMetadata(
        val canonicalId: String,
        val title: String,
        val originalTitle: String?,
        val synonyms: List<String>,
        val providers: Map<String, ProviderMetadata>,
        val metadata: Map<String, Any>,
        val lastUpdated: Long,
        val confidence: Float // 0-1
    )
    
    data class ProviderMetadata(
        val providerId: String,
        val providerSpecificId: String,
        val url: String,
        val extra: Map<String, Any>
    )
    
    fun addOrUpdateMetadata(providerId: String, metadata: UnifiedMetadata): UnifiedMetadata {
        val canonicalKey = generateCanonicalKey(metadata.title, metadata.synonyms)
        val existing = metadataCache[canonicalKey]
        
        val updated = if (existing != null) {
            mergeMetadata(existing, providerId, metadata)
        } else {
            val canonicalId = "kuro_${canonicalKey.hashCode().toString(36)}"
            metadata.copy(
                canonicalId = canonicalId,
                providers = mapOf(providerId to metadata.providers[providerId]!!)
            )
        }
        
        metadataCache[canonicalKey] = updated
        canonicalIds[metadata.canonicalId] = canonicalKey
        accessTime[canonicalKey] = System.currentTimeMillis()
        
        // Update provider mapping
        providerMapping.computeIfAbsent(providerId) { mutableSetOf() }.add(canonicalKey)
        
        // Evict if over limit
        if (metadataCache.size > MAX_CACHE_SIZE) {
            evictOldest()
        }
        
        return updated
    }
    
    private fun mergeMetadata(existing: UnifiedMetadata, providerId: String, newMeta: UnifiedMetadata): UnifiedMetadata {
        val mergedProviders = existing.providers + (providerId to newMeta.providers[providerId]!!)
        val mergedSynonyms = (existing.synonyms + newMeta.synonyms).distinct()
        val mergedMetadata = existing.metadata + newMeta.metadata
        
        return existing.copy(
            providers = mergedProviders,
            synonyms = mergedSynonyms,
            metadata = mergedMetadata,
            lastUpdated = maxOf(existing.lastUpdated, newMeta.lastUpdated),
            confidence = minOf(1.0f, existing.confidence + 0.1f)
        )
    }
    
    private fun generateCanonicalKey(title: String, synonyms: List<String>): String {
        val allNames = (listOf(title) + synonyms)
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }
            .sorted()
        return allNames.joinToString("|")
    }
    
    fun findByTitle(title: String): UnifiedMetadata? {
        val key = generateCanonicalKey(title, emptyList())
        return metadataCache[key]?.also { accessTime[key] = System.currentTimeMillis() }
    }
    
    fun findByCanonicalId(canonicalId: String): UnifiedMetadata? {
        val key = canonicalIds[canonicalId]
        return key?.let { k: String -> metadataCache[k]?.also { accessTime[k] = System.currentTimeMillis() } }
    }
    
    fun getAllForProvider(providerId: String): List<UnifiedMetadata> {
        return providerMapping[providerId]?.mapNotNull { metadataCache[it] } ?: emptyList()
    }
    
private fun evictOldest() {
        val oldest = accessTime.minByOrNull { it.value }?.key
        oldest?.let { itKey: String ->
            metadataCache.remove(itKey)
            canonicalIds.remove(itKey)
            accessTime.remove(itKey)
            providerMapping.values.forEach { set: MutableSet<String> -> set.remove(itKey) }
        }
    }

    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalEntries = metadataCache.size,
            totalProviders = providerMapping.size,
            oldestEntry = accessTime.values.minOrNull() ?: 0,
            newestEntry = accessTime.values.maxOrNull() ?: 0
        )
    }
    
    data class CacheStats(
        val totalEntries: Int,
        val totalProviders: Int,
        val oldestEntry: Long,
        val newestEntry: Long
    )
}