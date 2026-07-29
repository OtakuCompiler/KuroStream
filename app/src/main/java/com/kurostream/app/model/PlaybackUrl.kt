package com.kurostream.app.model

data class PlaybackUrl(
    val title: String = "",
    val url: String,
    val quality: String = "auto",
    val headers: Map<String, String> = emptyMap()
)
