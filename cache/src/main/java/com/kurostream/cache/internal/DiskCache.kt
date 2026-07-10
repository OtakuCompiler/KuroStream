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

import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class DiskCache(
    private val cacheDir: File,
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    private val appVersion: Int = 1
) {
    companion object { const val DEFAULT_MAX_SIZE_BYTES = 50L * 1024 * 1024 }

    private var diskLruCache: DiskLruCache? = null

    init {
        require(maxSizeBytes > 0)
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    private fun getCache(): DiskLruCache? {
        if (diskLruCache == null || diskLruCache?.isClosed == true) {
            diskLruCache = DiskLruCache.open(cacheDir, appVersion, 1, maxSizeBytes)
        }
        return diskLruCache
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(key: String): T? = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache() ?: return@withContext null
        val snapshot = cache.get(safeKey) ?: return@withContext null
        try {
            snapshot.getInputStream(0).use { stream ->
                ObjectInputStream(stream).use { ois ->
                    val entry = ois.readObject() as? CacheEntry<T>
                    if (entry == null || entry.isExpired()) {
                        cache.remove(safeKey)
                        return@withContext null
                    }
                    entry.value
                }
            }
        } catch (e: Exception) {
            cache.remove(safeKey)
            null
        }
    }

    suspend fun <T : Any> put(key: String, value: T, ttlMs: Long = 0L) = withContext(Dispatchers.IO) {
        val safeKey = sanitizeKey(key)
        val cache = getCache() ?: return@withContext
        val editor = cache.edit(safeKey) ?: return@withContext
        try {
            editor.newOutputStream(0).use { stream ->
                ObjectOutputStream(stream).use { oos ->
                    oos.writeObject(CacheEntry(value, ttlMs = ttlMs))
                }
            }
            editor.commit()
        } catch (e: Exception) { editor.abort() }
    }

    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        getCache()?.remove(sanitizeKey(key))
    }

    suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        val snapshot = getCache()?.get(sanitizeKey(key)) ?: return@withContext false
        snapshot.close()
        true
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        getCache()?.delete()
        diskLruCache = null
    }

    fun size(): Long = getCache()?.size() ?: 0L

    private fun sanitizeKey(key: String): String {
        return key.hashCode().toString(16).padStart(32, '0').take(120)
    }

    fun close() {
        diskLruCache?.close()
        diskLruCache = null
    }
}
