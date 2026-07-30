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

    private val fetchCache = ConcurrentHashMap<String, Any?>()
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
        if (cached != null) return cached

        return mutex.withLock {
            @Suppress("UNCHECKED_CAST")
            val existing = fetchCache[key] as? T
            existing ?: run {
                val result = fetch()
                fetchCache[key] = result
                searchResults.put(key, result, ttlMs)
                result
            }
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
