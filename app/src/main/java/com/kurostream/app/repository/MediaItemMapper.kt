// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.repository

import com.kurostream.domain.model.WatchHistory
import com.kurostream.app.model.MediaItem as TvMediaItem
import com.kurostream.domain.model.MediaItem as CanonicalMediaItem

/**
 * Maps the canonical, data-backed domain model onto the tv UI's own presentation model.
 *
 * The two shapes aren't a clean 1:1: the canonical model has no per-series episode list
 * (a single MediaItem is one watchable unit — see MERGE_REPORT_2.md), so `episodeCount`,
 * `format`, `status`, `year`, `genres` have no canonical source and default to null/empty.
 * Watch-progress fields are folded in from a matching [WatchHistory] row, if provided,
 * since the tv model bakes progress directly onto the item rather than keeping it separate.
 */
fun CanonicalMediaItem.toTvMediaItem(watchHistory: WatchHistory? = null): TvMediaItem = TvMediaItem(
    id = id,
    title = title,
    synopsis = description ?: "",
    posterImage = posterUrl,
    coverImage = bannerUrl,
    bannerImage = bannerUrl,
    genres = emptyList(),
    rating = rating ?: 0.0,
    year = releaseDate?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).year },
    format = category.name,
    status = null,
    episodeCount = null,
    trailerUrl = null,
    lastWatchedEpisodeId = watchHistory?.episodeNumber?.toString(),
    lastWatchedEpisode = watchHistory?.episodeNumber,
    lastPositionMs = watchHistory?.position ?: 0L,
    watchProgressPercent = watchHistory?.completionPercent ?: 0f
)
