package com.kurostream.cache.internal

import com.google.gson.Gson
import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class DiskCache(
    private val cacheDir: File,
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    private val appVersion: Int = 1,
    private val shardCount: Int = 4,
) {
    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 50L * 1024 * 1024
        private fun shardMask(sc: Int) = sc - 1
    }

    private val shards = ConcurrentHashMap<Int, DiskLruCache>()
    private val shardMutexes = Array(shardCount) { Any() }
    private val gson = Gson()

    init {
        require(maxSizeBytes > 0)
        require(shardCount in 1..16 && (shardCount and (shardCount - 1)) == 0) { "shardCount must be power of 2" }
        if (!cacheDir.exists()) cacheDir.mkdirs()
        for (i in 0 until shardCount) {
            File(cacheDir, "shard_$i").mkdirs()
        }
    }

    private fun getCache(key: String): DiskLruCache? {
        val shardIndex = key.hashCode() and shardMask(shardCount)
        val shardDir = File(cacheDir, "shard_$shardIndex")
        val shardMaxSize = maxSizeBytes / shardCount
        val cache = shards[shardIndex]
        if (cache == null || cache.isClosed) {
            val newCache = DiskLruCache.open(shardDir, appVersion, 1, shardMaxSize)
            shards[shardIndex] = newCache
            return newCache
        }
        return cache
    }

    suspend fun get(key: String): CacheEntrySerializable? = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext null
        val shardIndex = safeKey.hashCode() and shardMask(shardCount)
        synchronized(shardMutexes[shardIndex]) {
            val snapshot = cache.get(safeKey) ?: return@synchronized null
            try {
                snapshot.getInputStream(0).use { stream ->
                    val jsonStr = stream.bufferedReader().readText()
                    val entry = gson.fromJson(jsonStr, CacheEntrySerializable::class.java)
                    if (entry == null || entry.isExpired()) {
                        cache.remove(safeKey)
                        return@synchronized null
                    }
                    entry
                }
            } catch (e: Exception) {
                cache.remove(safeKey)
                null
            }
        }
    }

    suspend fun put(key: String, entry: CacheEntrySerializable) = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext
        val shardIndex = safeKey.hashCode() and shardMask(shardCount)
        synchronized(shardMutexes[shardIndex]) {
            val editor = cache.edit(safeKey) ?: return@synchronized
            try {
                editor.newOutputStream(0).use { stream ->
                    stream.write(gson.toJson(entry).encodeToByteArray())
                }
                editor.commit()
            } catch (e: Exception) {
                editor.abort()
            }
        }
    }

    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext
        val shardIndex = safeKey.hashCode() and shardMask(shardCount)
        synchronized(shardMutexes[shardIndex]) {
            cache.remove(safeKey)
        }
    }

    suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext false
        val shardIndex = safeKey.hashCode() and shardMask(shardCount)
        synchronized(shardMutexes[shardIndex]) {
            val snapshot = cache.get(safeKey) ?: return@synchronized false
            snapshot.close()
            true
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        shards.values.forEach { it.delete() }
        shards.clear()
        for (i in 0 until shardCount) {
            val shardDir = File(cacheDir, "shard_$i")
            shardDir.deleteRecursively()
            shardDir.mkdirs()
        }
    }

    fun size(): Long = shards.values.sumOf { it.size() }

    var evictionCount: Long = 0

    private fun sanitizeKey(key: String): String {
        val hash = key.hashCode()
        return hash.toString(16).padStart(32, '0').take(120)
    }

    fun close() {
        shards.values.forEach { it.close() }
        shards.clear()
    }
}
