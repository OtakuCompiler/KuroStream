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

interface PlatformCrypto {
    suspend fun encrypt(data: ByteArray, key: String): ByteArray
    suspend fun decrypt(data: ByteArray, key: String): ByteArray
    suspend fun generateKey(alias: String): String
    suspend fun getKey(alias: String): String?
    suspend fun deleteKey(alias: String)
    suspend fun encryptString(text: String, key: String): String
    suspend fun decryptString(encrypted: String, key: String): String
    fun hash(data: String, algorithm: HashAlgorithm = HashAlgorithm.SHA_256): String
    suspend fun hashAsync(data: String, algorithm: HashAlgorithm = HashAlgorithm.SHA_256): Flow<String>
}

enum class HashAlgorithm { MD5, SHA_1, SHA_256, SHA_512 }