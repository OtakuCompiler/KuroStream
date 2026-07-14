package com.kurostream.launcher.firebase.remoteconfig

data class FeatureFlag(
    val key: String,
    val enabled: Boolean,
    val description: String = ""
)
