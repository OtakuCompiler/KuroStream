package com.kurostream.domain.security

import android.content.Context
import android.content.pm.PackageManager

class RealSignatureVerifier(private val context: Context) : SignatureVerifier {
    override fun verify(apkPath: String): Result<String> {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
            val signatures = pkgInfo?.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val certBytes = signatures[0].toByteArray()
                val fingerprint = digest.digest(certBytes).joinToString("") { "%02x".format(it) }
                Result.success(fingerprint)
            } else {
                Result.failure(Exception("No signatures found in APK"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Signature verification failed: ${e.message}"))
        }
    }
}
