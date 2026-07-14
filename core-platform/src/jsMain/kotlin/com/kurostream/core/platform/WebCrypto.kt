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
import kotlinx.coroutines.flow.flow

class WebCrypto : PlatformCrypto {
    private val keys = mutableMapOf<String, String>()

    override suspend fun encrypt(data: ByteArray, key: String): ByteArray {
        val keyData = keys[key]?.toByteArray() ?: throw IllegalArgumentException("Key not found")
        return xorCrypt(data, keyData)
    }

    override suspend fun decrypt(data: ByteArray, key: String): ByteArray {
        val keyData = keys[key]?.toByteArray() ?: throw IllegalArgumentException("Key not found")
        return xorCrypt(data, keyData)
    }

    override suspend fun generateKey(alias: String): String {
        val randomKey = generateRandomString(32)
        keys[alias] = randomKey
        return alias
    }

    override suspend fun getKey(alias: String): String? {
        return keys[alias]
    }

    override suspend fun deleteKey(alias: String) {
        keys.remove(alias)
    }

    override suspend fun encryptString(text: String, key: String): String {
        val encrypted = encrypt(text.toByteArray(), key)
        return encrypted.joinToString("") { "%02x".format(it) }
    }

    override suspend fun decryptString(encrypted: String, key: String): String {
        val bytes = encrypted.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return String(decrypt(bytes, key))
    }

    override fun hash(data: String, algorithm: HashAlgorithm): String {
        return simpleHash(data, algorithm)
    }

    override suspend fun hashAsync(data: String, algorithm: HashAlgorithm): Flow<String> = flow {
        emit(hash(data, algorithm))
    }

    private fun xorCrypt(data: ByteArray, key: ByteArray): ByteArray {
        return ByteArray(data.size) { i ->
            (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
    }

    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun simpleHash(data: String, algorithm: HashAlgorithm): String {
        var hash = 0L
        for (char in data) {
            hash = (hash * 31 + char.code) and 0xFFFFFFFFFFFFFFFFL
        }
        return when (algorithm) {
            HashAlgorithm.MD5 -> hash.toString(16).padStart(32, '0')
            HashAlgorithm.SHA_1 -> hash.toString(16).padStart(40, '0')
            HashAlgorithm.SHA_256 -> hash.toString(16).padStart(64, '0')
            HashAlgorithm.SHA_512 -> hash.toString(16).padStart(128, '0')
        }
    }
}