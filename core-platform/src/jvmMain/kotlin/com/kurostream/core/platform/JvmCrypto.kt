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

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JvmCrypto : PlatformCrypto {
    private val keys = mutableMapOf<String, SecretKey>()

    override suspend fun encrypt(data: ByteArray, key: String): ByteArray {
        val secretKey = getOrCreateKey(key)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(data)
        val iv = cipher.iv
        return iv + encrypted
    }

    override suspend fun decrypt(data: ByteArray, key: String): ByteArray {
        val secretKey = keys[key] ?: throw IllegalArgumentException("Key not found")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    override suspend fun generateKey(alias: String): String {
        getOrCreateKey(alias)
        return alias
    }

    override suspend fun getKey(alias: String): String? {
        return if (keys.containsKey(alias)) alias else null
    }

    override suspend fun deleteKey(alias: String) {
        keys.remove(alias)
    }

    override suspend fun encryptString(text: String, key: String): String {
        val encrypted = encrypt(text.toByteArray(), key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    override suspend fun decryptString(encrypted: String, key: String): String {
        val decoded = Base64.getDecoder().decode(encrypted)
        return String(decrypt(decoded, key))
    }

    override fun hash(data: String, algorithm: HashAlgorithm): String {
        val md = MessageDigest.getInstance(algorithm.name.replace("_", "-"))
        val digest = md.digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    override suspend fun hashAsync(data: String, algorithm: HashAlgorithm): Flow<String> = flow {
        emit(hash(data, algorithm))
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        return keys.getOrPut(alias) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }
    }
}