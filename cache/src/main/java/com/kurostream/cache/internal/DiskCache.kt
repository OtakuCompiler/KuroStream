package com.kurostream.cache.internal

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.ConcurrentHashMap

internal class DiskCache(
    private val cacheDir: File,
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    private val appVersion: Int = 1,
    private val shardCount: Int = 4,
) {
    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 50L * 1024 * 1024
        private const val SHARD_MASK = 0x03
    }

    private val shards = ConcurrentHashMap<Int, DiskLruCache>()
    private val shardMutexes = Array(shardCount) { Any() }

    init {
        require(maxSizeBytes > 0)
        require(shardCount in 1..16)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        for (i in 0 until shardCount) {
            File(cacheDir, "shard_$i").mkdirs()
        }
    }

    private fun getCache(key: String): DiskLruCache? {
        val shardIndex = key.hashCode() and SHARD_MASK
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

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(key: String): T? = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext null
        val shardIndex = safeKey.hashCode() and SHARD_MASK
        synchronized(shardMutexes[shardIndex]) {
            val snapshot = cache.get(safeKey) ?: return@synchronized null
            try {
                snapshot.getInputStream(0).use { stream ->
                    ObjectInputStream(stream).use { ois ->
                        val entry = ois.readObject() as? CacheEntry<T>
                        if (entry == null || entry.isExpired()) {
                            cache.remove(safeKey)
                            return@synchronized null
                        }
                        entry.value
                    }
                }
            } catch (e: Exception) {
                cache.remove(safeKey)
                null
            }
        }
    }

    suspend fun <T : Any> put(key: String, value: T, ttlMs: Long = 0L) = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext
        val shardIndex = safeKey.hashCode() and SHARD_MASK
        synchronized(shardMutexes[shardIndex]) {
            val editor = cache.edit(safeKey) ?: return@synchronized
            try {
                editor.newOutputStream(0).use { stream ->
                    ObjectOutputStream(stream).use { oos ->
                        oos.writeObject(CacheEntry(value, ttlMs = ttlMs))
                    }
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
        val shardIndex = safeKey.hashCode() and SHARD_MASK
        synchronized(shardMutexes[shardIndex]) {
            cache.remove(safeKey)
        }
    }

    suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache(safeKey) ?: return@withContext false
        val shardIndex = safeKey.hashCode() and SHARD_MASK
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
