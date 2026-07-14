package com.kurostream.launcher.firebase.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    fun getFlag(key: String): FeatureFlag {
        val enabled = remoteConfig.getBoolean(key)
        return FeatureFlag(key = key, enabled = enabled)
    }

    fun getString(key: String): String = remoteConfig.getString(key)

    fun getLong(key: String): Long = remoteConfig.getLong(key)
}
