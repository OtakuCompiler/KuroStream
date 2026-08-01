package com.kurostream.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val duration: Long = 0L,
    val watchedDuration: Long = 0L
)
