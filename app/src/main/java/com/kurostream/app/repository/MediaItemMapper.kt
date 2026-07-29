package com.kurostream.app.repository

import com.kurostream.domain.entity.MediaItem as CanonicalMediaItem
import com.kurostream.app.model.MediaItem as TvMediaItem
import com.kurostream.domain.model.WatchHistory

fun CanonicalMediaItem.toTvMediaItem(watchHistory: WatchHistory? = null): TvMediaItem = TvMediaItem(
    id = id,
    title = title,
    description = synopsis ?: originalTitle ?: "",
    posterUrl = coverImageUrl ?: "",
    backdropUrl = bannerImageUrl ?: "",
    genre = genres,
    rating = score?.toFloat() ?: 0f,
    year = seasonYear ?: 0,
    duration = durationMinutes ?: 0,
    episodes = emptyList(),
    source = sourceExtensionId,
    isFavorite = false,
    watchProgress = (watchHistory?.completionPercent ?: 0f).toLong(),
)
