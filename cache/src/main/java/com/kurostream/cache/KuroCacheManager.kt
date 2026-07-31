// This file is part of KuroStream.
//
// KuroCacheManager — unified cache abstraction for VOD cache.
// Stores:
//   - network segments
//   - torrent pieces
//   - thumbnails
//   - metadata
//   - subtitles
//
// Target: 475MB VOD cache.
// Never uses disk cache for active frame processing.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroCacheManager @Inject constructor(
    private val context: android.content.Context,
) : CacheNamespaceManager {

    private val rootDir = File(context.cacheDir, "kurostream")
    private val memoryCache = ConcurrentHashMap<String, ByteArray>()
    private val maxMemoryBytes = 50 * 1024 * 1024
    private var currentMemoryBytes = 0

    override suspend fun put(key: String, value: ByteArray) = withContext(Dispatchers.IO) {
        val file = namespaceFile(key)
        file.parentFile?.mkdirs()
        file.writeBytes(value)
        if (currentMemoryBytes + value.size <= maxMemoryBytes) {
            synchronized(memoryCache) {
                currentMemoryBytes += value.size
                memoryCache[key] = value
            }
        }
    }

    override suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        memoryCache[key] ?: runCatching { File(rootDir, key).readBytes() }.getOrNull()
    }

    override suspend fun invalidateNamespace(namespace: String) = withContext(Dispatchers.IO) {
        val dir = File(rootDir, namespace)
        dir.deleteRecursively()
        synchronized(memoryCache) {
            memoryCache.keys.removeAll { it.startsWith(namespace) }
        }
    }

    override suspend fun namespaceSizeBytes(namespace: String): Long = withContext(Dispatchers.IO) {
        val dir = File(rootDir, namespace)
        dir.walk().sumOf { it.length() }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        rootDir.deleteRecursively()
        memoryCache.clear()
        currentMemoryBytes = 0
    }

    private fun namespaceFile(key: String): File = File(rootDir, key)

    suspend fun totalCacheSize(): Long = withContext(Dispatchers.IO) {
        rootDir.walk().sumOf { it.length() }
    }

    suspend fun enforceBudget(maxBytes: Long = 475L * 1024 * 1024) = withContext(Dispatchers.IO) {
        val current = totalCacheSize()
        if (current > maxBytes) {
            val files = rootDir.walk().filter { it.isFile }.sortedBy { it.lastModified() }.toList()
            var freed = 0L
            for (file in files) {
                if (current - freed <= maxBytes * 0.8) break
                val len = file.length()
                file.delete()
                freed += len
            }
        }
    }
}
