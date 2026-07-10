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

package com.kurostream.cache.internal

import android.util.LruCache

internal class MemoryCache(maxSize: Int = DEFAULT_MAX_SIZE) {
    companion object { const val DEFAULT_MAX_SIZE = 256 }

    private val cache = LruCache<String, CacheEntry<Any>>(maxSize)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? {
        val entry = cache.get(key) ?: return null
        if (entry.isExpired()) { cache.remove(key); return null }
        return entry.value as? T
    }

    fun <T : Any> put(key: String, value: T, ttlMs: Long = 0L) {
        cache.put(key, CacheEntry(value, ttlMs = ttlMs))
    }

    fun remove(key: String) { cache.remove(key) }

    fun contains(key: String): Boolean {
        val entry = cache.get(key) ?: return false
        if (entry.isExpired()) { cache.remove(key); return false }
        return true
    }

    fun clear() { cache.evictAll() }
    fun size(): Int = cache.size()
    fun hitCount(): Long = cache.hitCount().toLong()
    fun missCount(): Long = cache.missCount().toLong()
    fun evictionCount(): Long = cache.evictionCount().toLong()
}
