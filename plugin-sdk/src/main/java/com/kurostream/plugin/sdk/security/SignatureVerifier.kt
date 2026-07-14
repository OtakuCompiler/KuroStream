package com.kurostream.plugin.sdk.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.kurostream.core.common.result.Result
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.util.jar.JarFile
import javax.inject.Inject

interface SignatureVerifier {
    suspend fun verify(path: String): Result<String>
    fun isTrusted(fingerprint: String): Boolean
    suspend fun trustFingerprint(fingerprint: String): Result<Unit>
    suspend fun revokeFingerprint(fingerprint: String): Result<Unit>
}

class PermissiveSignatureVerifier @Inject constructor() : SignatureVerifier {
    override suspend fun verify(path: String): Result<String> = Result.Success("DEV_TRUSTED")
    override fun isTrusted(fingerprint: String): Boolean = true
    override suspend fun trustFingerprint(fingerprint: String): Result<Unit> = Result.Success(Unit)
    override suspend fun revokeFingerprint(fingerprint: String): Result<Unit> = Result.Success(Unit)
}

class RealSignatureVerifier @Inject constructor(
    private val context: Context
) : SignatureVerifier {

    private val trustedFingerprints = mutableSetOf<String>()
    private val tag = "SignatureVerifier"

    override suspend fun verify(path: String): Result<String> {
        return try {
            val jarFile = JarFile(java.io.File(path))
            val entry = jarFile.getJarEntry("META-INF/MANIFEST.MF")
                ?: return Result.Error(Exception("No MANIFEST.MF found"))

            val certs = entry.certificates
            if (certs == null || certs.isEmpty()) {
                return Result.Error(Exception("No signatures found in JAR"))
            }

            val cert = certs[0]
            val fingerprint = sha256Fingerprint(cert.encoded)
            jarFile.close()

            if (isTrusted(fingerprint)) {
                Result.Success(fingerprint)
            } else {
                Result.Error(Exception("Untrusted signature: $fingerprint"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Signature verification failed for $path", e)
            Result.Error(e)
        }
    }

    override fun isTrusted(fingerprint: String): Boolean {
        return trustedFingerprints.contains(fingerprint)
    }

    override suspend fun trustFingerprint(fingerprint: String): Result<Unit> {
        trustedFingerprints.add(fingerprint)
        return Result.Success(Unit)
    }

    override suspend fun revokeFingerprint(fingerprint: String): Result<Unit> {
        trustedFingerprints.remove(fingerprint)
        return Result.Success(Unit)
    }

    private fun sha256Fingerprint(certBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certBytes)
        return hash.joinToString(":") { "%02X".format(it) }
    }
}
