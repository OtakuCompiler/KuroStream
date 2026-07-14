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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheNamespaceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir = File(context.cacheDir, "kurostream_cache")
    private val managers = mutableMapOf<String, CacheManager>()
    private val lock = Any()

    val artwork: CacheManager get() = getOrCreate("artwork", 64, 100L * 1024 * 1024)
    val metadata: CacheManager get() = getOrCreate("metadata", 128, 20L * 1024 * 1024)
    val subtitles: CacheManager get() = getOrCreate("subtitles", 32, 30L * 1024 * 1024)
    val searchResults: CacheManager get() = getOrCreate("search_results", 48, 10L * 1024 * 1024)
    val userData: CacheManager get() = getOrCreate("user_data", 16, 5L * 1024 * 1024)
    val plugin: CacheManager get() = getOrCreate("plugin", 16, 5L * 1024 * 1024)

    private fun getOrCreate(namespace: String, memoryMaxSize: Int, diskMaxSizeBytes: Long): CacheManager {
        synchronized(lock) {
            return managers.getOrPut(namespace) {
                CacheManagerImpl(namespace, cacheDir, memoryMaxSize, diskMaxSizeBytes)
            }
        }
    }

    suspend fun clearAll() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        synchronized(lock) {
            managers.values.forEach { it.clear() }
        }
    }

    fun getAllStats(): List<CacheStats> = managers.values.map { it.stats.value }
}
