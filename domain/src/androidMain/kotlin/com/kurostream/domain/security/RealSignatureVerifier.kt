package com.kurostream.domain.security

import android.content.Context
import android.content.pm.PackageManager

class RealSignatureVerifier(private val context: Context) : SignatureVerifier {
    override fun verify(apkPath: String): kotlin.Result<String> {
        return try {
            val pm = context.packageManager
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageArchiveInfo(apkPath, flags)
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo?.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.signatures
            }
            if (signatures != null && signatures.isNotEmpty()) {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val certBytes = signatures[0].toByteArray()
                val fingerprint = digest.digest(certBytes).joinToString("") { "%02x".format(it) }
                kotlin.Result.success(fingerprint)
            } else {
                kotlin.Result.failure(Exception("No signatures found in APK"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
