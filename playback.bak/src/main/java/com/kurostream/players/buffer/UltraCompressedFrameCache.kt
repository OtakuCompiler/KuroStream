package com.kurostream.players.buffer

import androidx.collection.LruCache
import com.github.luben.zstd.Zstd
import java.util.concurrent.atomic.AtomicReference

class UltraCompressedFrameCache(private val maxCacheSize: Int = 5 * 1024 * 1024) {
    private val cache = LruCache<String, ByteArray>(maxCacheSize)
    private val previousFrame = AtomicReference<ByteArray>()
    private val dict = loadDictionary("4k_dict.zstd")

    fun store(key: String, frameData: ByteArray) {
        val delta = if (previousFrame.get() != null) {
            computeDelta(previousFrame.get(), frameData)
        } else {
            frameData // Store full frame for first frame
        }
        previousFrame.set(frameData)
        cache.put(key, Zstd.compress(delta, dict))
    }

    fun retrieve(key: String): ByteArray? {
        return cache.get(key)?.let { Zstd.decompress(it, dict, 4 * 1024 * 1024) }
    }

    private fun computeDelta(prev: ByteArray, curr: ByteArray): ByteArray {
        return prev.zip(curr).map { (a, b) -> (a - b).toByte() }.toByteArray()
    }

    private fun loadDictionary(path: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(path)?.readBytes()
            ?: ByteArray(0)
    }
}