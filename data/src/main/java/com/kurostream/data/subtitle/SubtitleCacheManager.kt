// This file is part of KuroStream.
//
// SubtitleCacheManager — subtitle caching using the existing KuroCacheManager.
// Stores downloaded subtitles, metadata, and language info.
// Reuses CacheNamespaceManager to avoid duplicate cache systems.
// Target: <20MB memory.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle

import com.kurostream.cache.CacheNamespaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleCacheManager @Inject constructor(
    private val cache: CacheNamespaceManager,
) {

    suspend fun cacheSubtitle(
        mediaId: String,
        languageCode: String,
        providerId: String,
        content: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val key = "subtitle/$mediaId/$languageCode/$providerId"
        cache.put(key, content)
        key
    }

    suspend fun getCachedSubtitle(
        mediaId: String,
        languageCode: String,
        providerId: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val key = "subtitle/$mediaId/$languageCode/$providerId"
        cache.get(key)
    }

    suspend fun clearCache(mediaId: String? = null) = withContext(Dispatchers.IO) {
        if (mediaId == null) {
            cache.invalidateNamespace("subtitle")
        } else {
            cache.invalidateNamespace("subtitle/$mediaId")
        }
    }

    suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        cache.namespaceSizeBytes("subtitle")
    }
}
