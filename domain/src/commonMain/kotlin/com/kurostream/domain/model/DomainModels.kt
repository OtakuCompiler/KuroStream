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

package com.kurostream.domain.model

enum class MediaCategory { ANIME, MOVIE, TV_SHOW, DOCUMENTARY, KIDS, GENERAL }

data class WatchHistory(
    val id: String, val mediaItemId: String, val profileId: String,
    val position: Long = 0L, val duration: Long = 0L,
    val watchedAt: Long = System.currentTimeMillis(), val completionPercent: Float = 0f,
    val episodeNumber: Int? = null, val seasonNumber: Int? = null
)

data class Favorite(
    val id: String, val mediaItemId: String, val profileId: String,
    val addedAt: Long = System.currentTimeMillis(), val category: String = "general"
)

enum class DownloadStatus { PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED }

data class DownloadItem(
    val id: String, val mediaItemId: String, val profileId: String, val localPath: String,
    val status: DownloadStatus = DownloadStatus.PENDING, val progress: Float = 0f,
    val totalBytes: Long = 0L, val downloadedBytes: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(), val completedAt: Long? = null,
    val errorMessage: String? = null
)

data class EpisodeInfo(
    val seasonNumber: Int? = null, val episodeNumber: Int? = null, val episodeTitle: String? = null
)

data class PlaybackUrl(
    val title: String,
    val url: String,
    val quality: String = "auto",
    val headers: Map<String, String> = emptyMap(),
)

data class SubtitleResult(
    val id: String, val language: String, val fileName: String,
    val downloadCount: Int, val rating: Float, val fps: Double,
    val hearingImpaired: Boolean, val downloadUrl: String?
)

data class CatalogItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val year: String?,
    val rating: Float?,
    val genres: List<String>,
    val source: String
)

data class Episode(
    val id: String,
    val title: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val thumbnail: String?,
    val overview: String?,
    val released: String?
)

data class MediaDetail(
    val id: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val rating: Float?,
    val genres: List<String>,
    val cast: List<String>,
    val director: List<String>,
    val year: String?,
    val runtime: String?,
    val episodes: List<Episode>
)

data class StreamSource(
    val url: String?,
    val name: String,
    val quality: String,
    val headers: Map<String, String>,
    val isHls: Boolean,
    val isDash: Boolean
)
