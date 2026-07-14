package com.kurostream.backup.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCryptoService {

    private val saltSize = 16
    private val ivSize = 12
    private val keySize = 32
    private val iterationCount = 100_000
    private val algorithm = "PBKDF2WithHmacSHA256"

    fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(saltSize).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(ivSize).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data)
        return salt + iv + encrypted
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        val salt = data.copyOfRange(0, saltSize)
        val iv = data.copyOfRange(saltSize, saltSize + ivSize)
        val encrypted = data.copyOfRange(saltSize + ivSize, data.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize * 8)
        val factory = SecretKeyFactory.getInstance(algorithm)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
