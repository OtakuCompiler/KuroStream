package com.kurostream.launcher.firebase.remoteconfig

object RemoteConfigDefaults {
    val defaults: Map<String, Any> = mapOf(
        RemoteConfigKeys.KEY_ENABLE_ANALYTICS to true,
        RemoteConfigKeys.KEY_ENABLE_CRASHLYTICS to true,
        RemoteConfigKeys.KEY_ENABLE_PRE_CACHE to true,
        RemoteConfigKeys.KEY_MAX_CACHE_SIZE_MB to 2048L,
        RemoteConfigKeys.KEY_ENABLE_RECOMMENDATIONS to true,
        RemoteConfigKeys.KEY_ENABLE_EXTENSIONS to true,
        RemoteConfigKeys.KEY_STREAMING_QUALITY to "auto",
        RemoteConfigKeys.KEY_ENABLE_DEBUG_MODE to false,
        RemoteConfigKeys.KEY_MAINTENANCE_MODE to false,
        RemoteConfigKeys.KEY_MIN_APP_VERSION to "1.0.0"
    )
}
