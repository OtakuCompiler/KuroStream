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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidCrypto : PlatformCrypto {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override suspend fun encrypt(data: ByteArray, key: String): ByteArray {
        val secretKey = getSecretKey(key) ?: generateAndroidKey(key)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data)
        val iv = cipher.iv
        return iv + encryptedBytes
    }

    override suspend fun decrypt(data: ByteArray, key: String): ByteArray {
        val secretKey = getSecretKey(key) ?: throw IllegalArgumentException("Key not found")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = data.copyOfRange(0, 12)
        val encryptedBytes = data.copyOfRange(12, data.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedBytes)
    }

    override suspend fun generateKey(alias: String): String {
        generateAndroidKey(alias)
        return alias
    }

    override suspend fun getKey(alias: String): String? {
        return if (keyStore.containsAlias(alias)) alias else null
    }

    override suspend fun deleteKey(alias: String) {
        keyStore.deleteEntry(alias)
    }

    override suspend fun encryptString(text: String, key: String): String {
        val encrypted = encrypt(text.toByteArray(), key)
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    override suspend fun decryptString(encrypted: String, key: String): String {
        val decoded = Base64.decode(encrypted, Base64.DEFAULT)
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

    private fun getSecretKey(alias: String): SecretKey? {
        return keyStore.getKey(alias, null) as? SecretKey
    }

    private fun generateAndroidKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}