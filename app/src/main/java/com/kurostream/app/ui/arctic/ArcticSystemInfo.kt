package com.kurostream.app.ui.arctic

data class ArcticSystemInfo(
    val version: String = "1.0.0",
    val device: String = android.os.Build.MODEL,
    val storage: String = "",
    val memory: String = "",
    val uptime: String = "",
)
