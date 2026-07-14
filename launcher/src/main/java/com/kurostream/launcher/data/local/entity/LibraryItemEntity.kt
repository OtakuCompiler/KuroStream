package com.kurostream.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kurostream.launcher.data.model.MediaType

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey val id: String,
    val externalId: String,
    val source: String,
    val title: String,
    val overview: String?,
    val mediaType: MediaType,
    val libraryId: String,
    val libraryName: String,
    val seriesName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val durationMs: Long,
    val progressMs: Long,
    val isWatched: Boolean,
    val posterPath: String?,
    val backdropPath: String?,
    val streamUrl: String?,
    val lastSynced: Long
)
