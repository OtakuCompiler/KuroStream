package com.kurostream.app.repository

import com.kurostream.domain.entity.MediaItem as CanonicalMediaItem
import com.kurostream.app.model.MediaItem as TvMediaItem
import com.kurostream.domain.model.WatchHistory

fun CanonicalMediaItem.toTvMediaItem(watchHistory: WatchHistory? = null): TvMediaItem = TvMediaItem(
    id = id,
    title = title,
    description = description ?: title,
    posterUrl = posterUrl ?: "",
    backdropUrl = backdropUrl ?: "",
    genre = genre,
    rating = rating ?: 0f,
    year = year ?: 0,
    duration = duration ?: 0,
    episodes = emptyList(),
    source = source,
    isFavorite = false,
    watchProgress = (watchHistory?.completionPercent ?: 0f).toLong(),
)
