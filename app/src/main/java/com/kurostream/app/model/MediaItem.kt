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

package com.kurostream.app.model

data class MediaItem(
    val id: String,
    val title: String,
    val synopsis: String,
    val posterImage: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val genres: List<String> = emptyList(),
    val rating: Double = 0.0,
    val year: Int?,
    val format: String?,
    val status: String?,
    val episodeCount: Int?,
    val trailerUrl: String? = null,
    val lastWatchedEpisodeId: String? = null,
    val lastWatchedEpisode: Int? = null,
    val lastPositionMs: Long = 0L,
    val watchProgressPercent: Float = 0f
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String?,
    val thumbnail: String?,
    val durationMinutes: Int?
)

data class PlaybackUrl(
    val url: String,
    val title: String,
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class SubtitleTrack(
    val language: String,
    val url: String,
    val label: String
)
