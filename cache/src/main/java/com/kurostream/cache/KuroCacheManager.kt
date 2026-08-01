// KuroStream - Anime Streaming for Android TV
// Copyright (C) 2026 KuroStream Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.kurostream.common.memory.LowRamDevice
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroCacheManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val MAX_CACHE_SIZE_MB_DEFAULT = 500L
        private const val MAX_CACHE_SIZE_MB_LOW_RAM = 200L
        private const val RAM_DISK_SIZE_MB = 125L
    }

    val videoCache: Cache by lazy {
        createVideoCache()
    }

    private fun createVideoCache(): Cache {
        val isLowRam = LowRamDevice.isLowRamDevice(context)
        val maxBytes = if (isLowRam) {
            MAX_CACHE_SIZE_MB_LOW_RAM * 1024 * 1024
        } else {
            MAX_CACHE_SIZE_MB_DEFAULT * 1024 * 1024
        }

        val cacheDir = File(context.cacheDir, "kurostream_vod_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val evictor = LeastRecentlyUsedCacheEvictor(maxBytes)
        val databaseProvider = StandaloneDatabaseProvider(context)

        Timber.d("Video cache initialized: ${maxBytes / (1024 * 1024)}MB at ${cacheDir.absolutePath}")
        
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    fun getRamDiskCacheDir(): File {
        val ramDiskDir = File(context.cacheDir, "ram_disk_cache")
        if (!ramDiskDir.exists()) {
            ramDiskDir.mkdirs()
        }
        return ramDiskDir
    }

    fun clearAllCaches() {
        try {
            videoCache.release()
            val cacheDir = File(context.cacheDir, "kurostream_vod_cache")
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            Timber.d("All caches cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear caches")
        }
    }

    fun getCacheStats(): CacheStats {
        val cacheSpace = videoCache.cacheSpace
        val keys = videoCache.keys.size
        return CacheStats(
            usedBytes = cacheSpace,
            maxBytes = if (LowRamDevice.isLowRamDevice(context)) {
                MAX_CACHE_SIZE_MB_LOW_RAM * 1024 * 1024
            } else {
                MAX_CACHE_SIZE_MB_DEFAULT * 1024 * 1024
            },
            fileCount = keys,
        )
    }

    data class CacheStats(
        val usedBytes: Long,
        val maxBytes: Long,
        val fileCount: Int,
    )
}
