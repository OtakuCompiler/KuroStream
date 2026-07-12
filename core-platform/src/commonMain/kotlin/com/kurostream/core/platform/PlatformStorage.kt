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

package com.kurostream.core.platform

import kotlinx.coroutines.flow.Flow

interface PlatformStorage {
    suspend fun writeFile(path: String, data: ByteArray)
    suspend fun readFile(path: String): ByteArray
    suspend fun deleteFile(path: String)
    suspend fun fileExists(path: String): Boolean
    suspend fun listFiles(path: String): List<String>
    suspend fun createDirectory(path: String)
    suspend fun getFileSize(path: String): Long
    fun observeFile(path: String): Flow<ByteArray>
    
    // Key-value storage
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String): String?
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String): Boolean
    suspend fun putInt(key: String, value: Int)
    suspend fun getInt(key: String): Int
    suspend fun putLong(key: String, value: Long)
    suspend fun getLong(key: String): Long
    suspend fun remove(key: String)
    suspend fun clear()
}