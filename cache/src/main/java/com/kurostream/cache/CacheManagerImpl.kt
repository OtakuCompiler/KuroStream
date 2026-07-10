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

package com.kurostream.cache

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

    // Memory cache removed - all caching goes to disk for low memory footprint
    // Use Room + Flow as source of truth for metadata

    private val diskCache = DiskCache(File(cacheDir, namespace), diskMaxSizeBytes)
    private val mutex = Mutex()
    private val _stats = MutableStateFlow(CacheStats(namespace, 0, 0, 0, 0, 0, 0, 0))
    override val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    override suspend fun <T : Any> put(key: String, value: T, ttlMs: Long) {
        mutex.withLock {
            diskCache.put(key, value, ttlMs)
            updateStats()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: String, clazz: Class<T>): T? {
        return mutex.withLock {
            val diskValue = diskCache.get<T>(key)
            if (diskValue != null) {
                updateStats(diskHit = true)
                return@withLock diskValue
            }
            updateStats(miss = true)
            null
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            diskCache.remove(key)
            updateStats()
        }
    }

    override suspend fun contains(key: String): Boolean {
        return mutex.withLock { diskCache.contains(key) }
    }

    override suspend fun clear() {
        mutex.withLock {
            diskCache.clear()
            updateStats()
        }
    }

    override fun memorySize(): Int = 0 // No memory cache

    override fun diskSize(): Long = diskCache.size()

    private fun updateStats(memoryHit: Boolean = false, diskHit: Boolean = false, miss: Boolean = false) {
        val c = _stats.value
        _stats.value = c.copy(
            memoryHits = if (memoryHit) c.memoryHits + 1 else c.memoryHits,
            memoryMisses = if (miss && !diskHit) c.memoryMisses + 1 else c.memoryMisses,
            diskHits = if (diskHit) c.diskHits + 1 else c.diskHits,
            diskMisses = if (miss) c.diskMisses + 1 else c.diskMisses,
            evictions = diskCache.evictionCount,
            totalMemoryEntries = 0,
            totalDiskEntries = 0
        )
    }

    fun close() { diskCache.close() }
}
