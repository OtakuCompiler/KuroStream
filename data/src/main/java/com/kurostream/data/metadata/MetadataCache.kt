// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import androidx.collection.LruCache
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.domain.metadata.UnifiedAnimeDetails
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory LRU cache that sits in front of the network providers.
 *
 * Why: each metadata provider round-trip costs ~200-800 ms. When the user
 * navigates back to a detail page they already opened, the LRU returns in
 * <1 ms with zero network usage.
 *
 * Size is [LowRamDevice.metadataLruSize] entries — 20 on low-RAM, 150 on
 * high-RAM — so we never exceed a few hundred KB of heap for metadata objects.
 *
 * TTL: 10 minutes per entry. Stale entries are transparently removed on get().
 */
@Singleton
class MetadataCache @Inject constructor() {

    private data class Entry(val data: UnifiedAnimeDetails, val timestampMs: Long)

    private val ttlMs: Long = 10 * 60 * 1000L // 10 minutes

    private val lru: LruCache<String, Entry> by lazy {
        LruCache(LowRamDevice.metadataLruSize)
    }

    fun get(key: String): UnifiedAnimeDetails? {
        val entry = lru[key] ?: return null
        if (System.currentTimeMillis() - entry.timestampMs > ttlMs) {
            lru.remove(key)
            return null
        }
        return entry.data
    }

    fun put(key: String, data: UnifiedAnimeDetails) {
        lru.put(key, Entry(data, System.currentTimeMillis()))
    }

    fun invalidate(key: String) { lru.remove(key) }

    fun clear() { lru.evictAll() }
}
