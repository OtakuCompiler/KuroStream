// This file is part of KuroStream.
//
// SubtitleCacheManager — subtitle caching with disk backing.
// Stores downloaded subtitles, metadata, and language info.
// Reuses CacheNamespaceManager to avoid duplicate cache systems.
// Target: <20MB memory + 10MB disk cache.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle

import com.kurostream.cache.CacheNamespaceManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleCacheManager @Inject constructor(
    private val cache: CacheNamespaceManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {

    private val cacheDir = File(context.cacheDir, "subtitles").apply { mkdirs() }
    private val maxCacheBytes = 10L * 1024 * 1024

    fun cacheSubtitle(
        mediaId: String,
        languageCode: String,
        providerId: String,
        content: ByteArray,
    ): String {
        val key = "subtitle/$mediaId/$languageCode/$providerId"
        cache.metadata.put(key, content, 0)

        val file = File(cacheDir, key.replace('/', '_'))
        file.parentFile?.mkdirs()
        file.writeBytes(content)
        enforceLimit()
        return key
    }

    fun getCachedSubtitle(
        mediaId: String,
        languageCode: String,
        providerId: String,
    ): ByteArray? {
        val key = "subtitle/$mediaId/$languageCode/$providerId"
        val file = File(cacheDir, key.replace('/', '_'))
        if (file.exists()) return file.readBytes()
        return cache.metadata.get(key)
    }

    fun clearCache(mediaId: String? = null) {
        if (mediaId == null) {
            cache.metadata.clear()
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun cacheSizeBytes(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun enforceLimit() {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxCacheBytes) break
            total -= file.length()
            file.delete()
        }
    }
}
