package com.kurostream.cache

import com.google.gson.Gson
import com.kurostream.cache.internal.CacheEntrySerializable
import com.kurostream.cache.internal.DiskCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class CacheManagerImpl(
    override val namespace: String,
    cacheDir: File,
    diskMaxSizeBytes: Long = DiskCache.DEFAULT_MAX_SIZE_BYTES
) : CacheManager {

    private val diskCache = DiskCache(File(cacheDir, namespace), diskMaxSizeBytes)
    private val mutex = Mutex()
    private val gson = Gson()
    private val _stats = MutableStateFlow(CacheStats(namespace, 0, 0, 0, 0, 0, 0, 0))
    override val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> put(key: String, value: T, ttlMs: Long) {
        mutex.withLock {
            val valueStr = gson.toJson(value)
            val entry = CacheEntrySerializable(
                value = valueStr,
                expiresAt = if (ttlMs > 0) System.currentTimeMillis() + ttlMs else 0
            )
            diskCache.put(key, entry)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: String, clazz: Class<T>): T? {
        return mutex.withLock {
            val entry = diskCache.get(key)
            if (entry != null) {
                _stats.value = _stats.value.copy(diskHits = _stats.value.diskHits + 1)
                return@withLock gson.fromJson(entry.value, clazz)
            }
            _stats.value = _stats.value.copy(diskMisses = _stats.value.diskMisses + 1)
            null
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            diskCache.remove(key)
        }
    }

    override suspend fun contains(key: String): Boolean {
        return mutex.withLock { diskCache.contains(key) }
    }

    override suspend fun clear() {
        mutex.withLock {
            diskCache.clear()
            _stats.value = CacheStats(namespace, 0, 0, 0, 0, 0, 0, 0)
        }
    }

    override fun memorySize(): Int = 0

    override fun diskSize(): Long = diskCache.size()

    fun close() { diskCache.close() }
}
