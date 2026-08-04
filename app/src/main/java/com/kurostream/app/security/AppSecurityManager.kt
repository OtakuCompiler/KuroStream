package com.kurostream.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class AppSecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("security", Context.MODE_PRIVATE)
    private val playIntegrity = PlayIntegrityChecker(context)

    suspend fun runSecurityChecks(): SecurityReport = withContext(Dispatchers.IO) {
        val report = SecurityReport(
            isPlayInstalled = checkPlayInstallation(),
            isRooted = detectRoot(),
            isEmulator = detectEmulator(),
            isDebugBuild = detectDebugBuild(),
            isTampered = detectTampering(),
            hasMockLocation = detectMockLocation(),
            hasUsbDebugging = detectUsbDebugging(),
            hasDeveloperOptions = detectDeveloperOptions(),
        )
        prefs.edit().putBoolean("last_check_passed", report.isSecure)
            .putLong("last_check_time", System.currentTimeMillis()).apply()
        Timber.d("Security check: $report")
        report
    }

    fun shouldAllowPlayback(): Boolean {
        val lastCheck = prefs.getLong("last_check_time", 0)
        val passed = prefs.getBoolean("last_check_passed", false)
        return System.currentTimeMillis() - lastCheck < 3_600_000 && passed
    }

    private fun checkPlayInstallation(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending"
    }

    private fun detectRoot(): Boolean {
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/su/bin/su", "/magisk/.core/bin/su", "/system/app/SuperSU",
            "/system/etc/init.d", "/system/bin/.ext/.su", "/data/adb/magisk",
            "/data/adb/ksu", "/data/adb/apatch",
        )
        if (paths.any { File(it).exists() }) return true
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true
        return try {
            Runtime.getRuntime().exec("su").destroy()
            true
        } catch (e: Exception) { false }
    }

    private fun detectEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("qemu"))
    }

    private fun detectDebugBuild(): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun detectTampering(): Boolean {
        // Check for common hooking/instrumentation frameworks by probing their known classes.
        val hookingClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook",
            "com.saurik.substrate.MS",
            "me.weishu.exposed.entry.Main",
        )
        for (className in hookingClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) { /* expected */ }
        }
        // Check for frida server artifacts
        val fridaPaths = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
        )
        if (fridaPaths.any { java.io.File(it).exists() }) return true
        return false
    }

    private fun detectMockLocation(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1
    }

    private fun detectUsbDebugging(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    private fun detectDeveloperOptions(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    }

    data class SecurityReport(
        val isPlayInstalled: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebugBuild: Boolean,
        val isTampered: Boolean,
        val hasMockLocation: Boolean,
        val hasUsbDebugging: Boolean,
        val hasDeveloperOptions: Boolean,
    ) {
        val isSecure: Boolean
            get() = isPlayInstalled && !isRooted && !isEmulator && !isDebugBuild && !isTampered
    }
}