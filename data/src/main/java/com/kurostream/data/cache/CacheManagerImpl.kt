package com.kurostream.data.cache

import com.kurostream.cache.CacheNamespace
import com.kurostream.cache.CacheNamespaceManager
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManagerImpl @Inject constructor() : CacheManager, CacheRepository {

    override val searchResults: CacheNamespace = ThreadSafeCacheNamespace()
    override val metadata: CacheNamespace = ThreadSafeCacheNamespace()

    private val fetchCache = object : LinkedHashMap<String, Any?>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any?>): Boolean {
            return size > 100
        }
    }
    private val fetchCacheTtl = ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()

    private class ThreadSafeCacheNamespace : CacheNamespace {
        private val store = ConcurrentHashMap<String, CacheEntry>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: String): T? {
            val entry = store[key] ?: return null
            if (System.currentTimeMillis() > entry.expiresAt) {
                store.remove(key)
                return null
            }
            return entry.value as? T
        }

        override fun put(key: String, value: Any?, ttlMillis: Long) {
            if (value == null) {
                store.remove(key)
                return
            }
            store[key] = CacheEntry(value, System.currentTimeMillis() + ttlMillis)
        }

        override fun clear() {
            store.clear()
        }

        private data class CacheEntry(
            val value: Any,
            val expiresAt: Long
        )
    }

    override suspend fun <T> getOrFetch(key: String, ttlMs: Long, fetch: suspend () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val cached = fetchCache[key] as? T
        val ttl = fetchCacheTtl[key]
        if (cached != null && ttl != null && System.currentTimeMillis() < ttl) return cached

        return mutex.withLock {
            @Suppress("UNCHECKED_CAST")
            val existing = fetchCache[key] as? T
            val existingTtl = fetchCacheTtl[key]
            if (existing != null && existingTtl != null && System.currentTimeMillis() < existingTtl) {
                return@withLock existing
            }
            val result = fetch()
            synchronized(fetchCache) {
                fetchCache[key] = result
                fetchCacheTtl[key] = System.currentTimeMillis() + ttlMs
            }
            searchResults.put(key, result, ttlMs)
            result
        }
    }

    override suspend fun <T> get(key: String): T? {
        val entry = searchResults.get<String>(key)
        @Suppress("UNCHECKED_CAST")
        return entry as? T
    }

    override suspend fun put(key: String, value: Any, ttlMs: Long) {
        searchResults.put(key, value, ttlMs)
        fetchCache[key] = value
    }

    override suspend fun invalidate(key: String) {
        searchResults.put(key, null, 0)
        fetchCache.remove(key)
    }

    override suspend fun clearAll() {
        searchResults.clear()
        metadata.clear()
        fetchCache.clear()
    }
}
