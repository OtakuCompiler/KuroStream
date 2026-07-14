package com.kurostream.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val episodeId: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val durationWatchedMs: Long,
    val completionPercentage: Float,
    val watchedAt: Long,
    val source: String = ""
)
