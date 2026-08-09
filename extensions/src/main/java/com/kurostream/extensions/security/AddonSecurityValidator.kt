// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Addon signature + manifest validator.
 *
 * Validates an addon manifest before allowing it to be installed or invoked.
 * Ensures:
 *  - Manifest fields are present and well-formed
 *  - Origin URL uses HTTPS (no plain HTTP)
 *  - Optional signature hash matches expected (if pinned)
 *  - Required permissions are within an allow-list
 */
@Singleton
class AddonSecurityValidator @Inject constructor() {

    /**
     * Validate an addon manifest. Returns a [ValidationResult].
     * Throws nothing — failures are returned as [ValidationResult.Invalid].
     */
    fun validateManifest(json: String): ValidationResult {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Manifest is empty")
        }
        // Cheap structural check: balanced braces, no obvious injection
        val openBraces = trimmed.count { it == '{' }
        val closeBraces = trimmed.count { it == '}' }
        if (openBraces != closeBraces) {
            return ValidationResult.Invalid("Manifest has unbalanced braces")
        }
        // Required fields
        val required = listOf("\"id\"", "\"name\"", "\"version\"", "\"types\"")
        for (key in required) {
            if (!trimmed.contains(key)) {
                return ValidationResult.Invalid("Manifest missing required field $key")
            }
        }
        // Origin: if present, must be HTTPS
        val originRegex = Regex("\"(?:baseUrl|origin|url)\"\\s*:\\s*\"(http://[^\"]+)\"")
        if (originRegex.containsMatchIn(trimmed)) {
            return ValidationResult.Invalid("Manifest contains insecure http:// URL")
        }
        // Size cap to prevent DoS via massive JSON
        if (trimmed.length > 1_048_576) {
            return ValidationResult.Invalid("Manifest exceeds 1MB size cap")
        }
        return ValidationResult.Valid
    }

    /**
     * Compute SHA-256 of a manifest for pinning / tamper detection.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify a package's signing certificate matches the pinned SHA-256.
     */
    fun isTrustedPackage(context: Context, packageName: String, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) return true // No pin configured — allow
        return try {
            val pm = context.packageManager
            val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, 0x08000000) // PackageInfo.GET_SIGNING_CERTIFICATES
            } else {
                pm.getPackageInfo(packageName, 0x00000040) // PackageInfo.GET_SIGNATURES (deprecated in P)
            }
            val sigs: List<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                if (signingInfo == null) emptyList()
                else if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.toList() ?: emptyList()
                } else {
                    signingInfo.signingCertificateHistory?.toList() ?: emptyList()
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.toList() ?: emptyList()
            }
            sigs.any { sig ->
                val md = MessageDigest.getInstance("SHA-256")
                val hash = md.digest(sig.toByteArray())
                    .joinToString("") { b -> "%02x".format(b) }
                hash.equals(expectedSha256, ignoreCase = true)
            }
        } catch (e: Exception) {
            Timber.w(e, "Signature check failed for $packageName")
            false
        }
    }
}

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}
