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

package com.kurostream.domain.entity
import com.kurostream.domain.platform.platformCurrentTimeMillis

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val synopsis: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val type: MediaType,
    val status: AiringStatus,
    val episodeNumber: Int? = null,
    val totalEpisodes: Int? = null,
    val durationMinutes: Int? = null,
    val seasonYear: Int? = null,
    val seasonQuarter: Season? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val rating: ContentRating = ContentRating.UNRATED,
    val score: Double? = null,
    val sourceExtensionId: String,
    val deepLink: String? = null,
    val lastUpdated: Long = platformCurrentTimeMillis()
)

enum class MediaType { TV, MOVIE, OVA, ONA, SPECIAL, MUSIC }
enum class AiringStatus { AIRING, FINISHED, NOT_YET_AIRED, CANCELLED }
enum class Season { WINTER, SPRING, SUMMER, FALL }
enum class ContentRating { G, PG, PG13, R17, R, UNRATED }
