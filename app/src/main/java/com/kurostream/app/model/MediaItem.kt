package com.kurostream.app.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class MediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val genre: List<String> = emptyList(),
    val rating: Float = 0f,
    val year: Int = 0,
    val duration: Int = 0,
    val episodes: List<Episode> = emptyList(),
    val source: String = "",
    val isFavorite: Boolean = false,
    val watchProgress: Long = 0L
) : Parcelable

@Immutable
@Parcelize
data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val duration: Long = 0L,
    val watchedDuration: Long = 0L
) : Parcelable
