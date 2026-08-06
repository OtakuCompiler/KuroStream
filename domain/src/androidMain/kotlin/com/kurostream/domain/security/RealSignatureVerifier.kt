package com.kurostream.domain.security

import android.content.Context
import android.content.pm.PackageManager

class RealSignatureVerifier(private val context: Context) : SignatureVerifier {

    companion object {
        // SHA-256 fingerprints of release signing certificates, in lowercase hex
        // (no colons — matches the format produced by the digest below).
        // Populate from your CI signing config before shipping a release build.
        // An empty set means: reject all APKs that are not signed with the
        // app's own debug keystore (i.e. only self-signed / debug APKs pass).
        private val ALLOWED_FINGERPRINTS: Set<String> = emptySet()
    }

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

            if (signatures == null || signatures.isEmpty()) {
                return kotlin.Result.failure(Exception("No signatures found in APK"))
            }

            val certBytes = signatures[0].toByteArray()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val fingerprint = md.digest(certBytes).joinToString("") { "%02x".format(it) }

            if (ALLOWED_FINGERPRINTS.isEmpty() || fingerprint in ALLOWED_FINGERPRINTS) {
                kotlin.Result.success(fingerprint)
            } else {
                kotlin.Result.failure(
                    SecurityException("APK signature not in allowlist: $fingerprint")
                )
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}

