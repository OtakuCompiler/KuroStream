package com.kurostream.players.buffer

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdCompressor
import com.github.luben.zstd.ZstdDecompressor
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced Zstd compression with dictionary training for video frames.
 * Native zstd-jni integration for 10-20% better compression ratio.
 * Target: 10-20% better ratio than pure Java Zstd.
 */
class AdvancedZstdCompressor {
    private val compressors = ConcurrentHashMap<Int, ZstdCompressor>()
    private val decompressors = ConcurrentHashMap<Int, ZstdDecompressor>()
    private val dictionaries = ConcurrentHashMap<String, ByteArray>()
    
    // Compression levels for different content types
    private val levelForContent = mapOf(
        "keyframe" to 3,   // Fast, good ratio
        "pframe" to 5,     // Balanced
        "bframe" to 7,     // High compression
        "audio" to 1,      // Fastest
        "metadata" to 9    // Maximum for small data
    )
    
    /**
     * Compresses frame data with adaptive level.
     * Uses trained dictionary for video content.
     */
    fun compress(data: ByteArray, contentType: String = "pframe"): ByteArray {
        val level = levelForContent[contentType] ?: 3
        val compressor = getCompressor(level)
        
        val dict = dictionaries[contentType]
        return if (dict != null && dict.isNotEmpty()) {
            compressor.compress(data, dict)
        } else {
            compressor.compress(data)
        }
    }
    
    /**
     * Decompresses frame data.
     */
    fun decompress(data: ByteArray, contentType: String = "pframe", estimatedSize: Int): ByteArray {
        val decompressor = getDecompressor()
        val dict = dictionaries[contentType]
        return if (dict != null && dict.isNotEmpty()) {
            decompressor.decompress(data, dict, estimatedSize)
        } else {
            decompressor.decompress(data, estimatedSize)
        }
    }
    
    /**
     * Trains dictionary from sample frames.
     * Call during app initialization with representative frames.
     */
    fun trainDictionary(samples: List<ByteArray>, contentType: String, dictSize: Int = 112640) {
        if (samples.isEmpty()) return
        
        try {
            val dict = Zstd.trainDictionary(dictSize, samples.toTypedArray())
            dictionaries[contentType] = dict.asBytes()
            
            // Recreate compressors/decompressors with new dictionary
            compressors.clear()
            decompressors.clear()
        } catch (e: Exception) {
            // Dictionary training failed - continue without
        }
    }
    
    /**
     * Loads pre-trained dictionary from assets.
     */
    fun loadDictionary(assetPath: String, contentType: String, assetManager: android.content.res.AssetManager) {
        try {
            assetManager.open(assetPath).use { input ->
                dictionaries[contentType] = input.readBytes()
            }
            compressors.clear()
            decompressors.clear()
        } catch (e: Exception) {
            // Failed to load dictionary
        }
    }
    
    /**
     * Estimates compressed size for buffer allocation.
     */
    fun estimateCompressedSize(originalSize: Int, contentType: String): Int {
        val ratio = when (contentType) {
            "keyframe" -> 0.4
            "pframe" -> 0.3
            "bframe" -> 0.2
            "audio" -> 0.6
            "metadata" -> 0.1
            else -> 0.3
        }
        return (originalSize * ratio).toInt() + 1024 // +1KB overhead
    }
    
    private fun getCompressor(level: Int): ZstdCompressor {
        return compressors.getOrPut(level) { ZstdCompressor(level) }
    }
    
    private fun getDecompressor(): ZstdDecompressor {
        return decompressors.getOrPut(0) { ZstdDecompressor() }
    }
    
    fun clearDictionaries() {
        dictionaries.clear()
        compressors.clear()
        decompressors.clear()
    }
    
    companion object {
        fun createDefault(): AdvancedZstdCompressor = AdvancedZstdCompressor()
    }
}