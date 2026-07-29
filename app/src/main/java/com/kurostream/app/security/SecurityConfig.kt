package com.kurostream.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security configuration and runtime checks.
 * Detects debug builds, emulators, rooted devices, and tampering.
 */
@Singleton
class SecurityConfig @Inject constructor(
    private val context: Context
) {
    val isDebugBuild: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val isEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.lowercase().contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK"))

    val isRooted: Boolean
        get() {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            return paths.any { File(it).exists() }
        }

    val isFridaDetected: Boolean
        get() {
            // Check for Frida server port
            return try {
                java.net.Socket("127.0.0.1", 27042).use { it.close(); true }
            } catch (_: Exception) {
                false
            }
        }

    fun logSecurityStatus() {
        if (isDebugBuild) Timber.w("Security: Debug build detected")
        if (isEmulator) Timber.w("Security: Emulator detected")
        if (isRooted) Timber.e("Security: Rooted device detected")
        if (isFridaDetected) Timber.e("Security: Frida detected")
    }
}
