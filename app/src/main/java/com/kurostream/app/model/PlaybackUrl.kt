package com.kurostream.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlaybackUrl(
    val title: String = "",
    val url: String,
    val quality: String = "auto",
    val headers: Map<String, String> = emptyMap()
)
