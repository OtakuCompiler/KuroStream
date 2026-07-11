package com.kurostream.lint.checks

object ArchitectureDetektRule {
    const val DOMAIN_PACKAGE = "com.kurostream.domain"
    const val DATA_PACKAGE = "com.kurostream.data"
    const val APP_PACKAGE = "com.kurostream.app"
    const val PLAYBACK_PACKAGE = "com.kurostream.players"
    const val PLUGIN_SDK_PACKAGE = "com.kurostream.plugin.sdk"
    const val COMMON_PACKAGE = "com.kurostream.common"
    const val CACHE_PACKAGE = "com.kurostream.cache"
    const val EXTENSIONS_PACKAGE = "com.kurostream.extensions"
    const val LAUNCHER_PACKAGE = "com.kurostream.launcher"
    const val BACKUP_PACKAGE = "com.kurostream.backup"

    val MODULE_BOUNDARIES = mapOf(
        "domain" to listOf(DOMAIN_PACKAGE, COMMON_PACKAGE),
        "data" to listOf(DATA_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE, CACHE_PACKAGE),
        "app" to listOf(APP_PACKAGE, DOMAIN_PACKAGE, DATA_PACKAGE, COMMON_PACKAGE, CACHE_PACKAGE, PLAYBACK_PACKAGE, PLUGIN_SDK_PACKAGE, EXTENSIONS_PACKAGE, LAUNCHER_PACKAGE, BACKUP_PACKAGE),
        "playback" to listOf(PLAYBACK_PACKAGE, DOMAIN_PACKAGE),
        "plugin-sdk" to listOf(PLUGIN_SDK_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE),
        "common" to listOf(COMMON_PACKAGE),
        "cache" to listOf(CACHE_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE),
        "extensions" to listOf(EXTENSIONS_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE),
        "launcher" to listOf(LAUNCHER_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE),
        "backup" to listOf(BACKUP_PACKAGE, DOMAIN_PACKAGE, COMMON_PACKAGE, DATA_PACKAGE),
    )

    val DOMAIN_FORBIDDEN_IMPORTS = listOf(
        "android.", "androidx.", "dagger.", "com.google.",
        "com.squareup.", "okhttp3.", "retrofit2.",
        "com.kurostream.data", "com.kurostream.app",
        "com.kurostream.cache", "com.kurostream.players",
        "com.kurostream.extensions", "com.kurostream.launcher",
        "com.kurostream.backup",
    )

    val DATA_FORBIDDEN_IMPORTS = listOf(
        "com.kurostream.app", "com.kurostream.players",
        "androidx.compose.", "com.kurostream.launcher",
        "com.kurostream.extensions",
    )
}
