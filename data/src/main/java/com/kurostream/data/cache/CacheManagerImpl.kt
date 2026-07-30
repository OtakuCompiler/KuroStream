package com.kurostream.data.cache

import com.kurostream.cache.CacheNamespace
import com.kurostream.cache.CacheNamespaceManager
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManagerImpl @Inject constructor() : CacheManager {

    override val searchResults: CacheNamespace = ThreadSafeCacheNamespace()
    override val metadata: CacheNamespace = ThreadSafeCacheNamespace()

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
}
