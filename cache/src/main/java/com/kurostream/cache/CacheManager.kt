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

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

interface CacheManager {
    val namespace: String
    suspend fun <T : Any> put(key: String, value: T, ttlMs: Long = 0L)
    suspend fun <T : Any> get(key: String, clazz: Class<T>): T?
    suspend inline fun <reified T : Any> get(key: String): T? = get(key, T::class.java)
    suspend fun remove(key: String)
    suspend fun contains(key: String): Boolean
    suspend fun clear()
    fun memorySize(): Int
    fun diskSize(): Long
    val stats: Flow<CacheStats>
}

@Immutable
data class CacheStats(
    val namespace: String,
    val memoryHits: Long,
    val memoryMisses: Long,
    val diskHits: Long,
    val diskMisses: Long,
    val evictions: Long,
    val totalMemoryEntries: Int,
    val totalDiskEntries: Int
)
