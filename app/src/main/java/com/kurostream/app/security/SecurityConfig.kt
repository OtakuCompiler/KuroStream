package com.kurostream.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SecurityConfig @Inject constructor(
    private val context: Context
) {
    val isDebugBuild: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val isEmulator: Boolean
        get() = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator", ignoreCase = true)
                || Build.MODEL.contains("Emulator", ignoreCase = true)
                || Build.MODEL.contains("sdk", ignoreCase = true)
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

    suspend fun checkRootedAsync(): Boolean = withContext(Dispatchers.IO) {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
        )
        paths.any { File(it).exists() }
    }

    fun logSecurityStatus() {
        if (isDebugBuild) Timber.w("Security: Debug build detected")
        if (isEmulator) Timber.w("Security: Emulator detected")
    }
}
