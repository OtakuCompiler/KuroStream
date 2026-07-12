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

package com.kurostream.domain.home
import com.kurostream.core.platform.platformCurrentTimeMillis

import com.kurostream.domain.entity.AiringStatus
import com.kurostream.domain.entity.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class CustomHomeRow(
    val id: String,
    val title: String,
    val filter: RowFilter,
    val sortBy: SortOption = SortOption.POPULARITY_DESC,
    val limit: Int = 20,
    val isVisible: Boolean = true,
    val displayStyle: DisplayStyle = DisplayStyle.POSTER_GRID,
    val createdAt: Long = platformCurrentTimeMillis(),
    val updatedAt: Long = platformCurrentTimeMillis(),
)

@Serializable
data class RowFilter(
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val yearRange: YearRange? = null,
    val ratingRange: RatingRange? = null,
    val status: List<AiringStatus> = emptyList(),
    val mediaTypes: List<MediaType> = emptyList(),
    val keywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val minEpisodes: Int? = null,
    val maxEpisodes: Int? = null,
)

@Serializable
data class YearRange(
    val start: Int,
    val end: Int,
)

@Serializable
data class RatingRange(
    val min: Double,
    val max: Double,
)

enum class SortOption {
    POPULARITY_DESC,
    POPULARITY_ASC,
    RATING_DESC,
    RATING_ASC,
    RECENTLY_ADDED,
    RECENTLY_UPDATED,
    TITLE_AZ,
    TITLE_ZA,
    EPISODE_COUNT_DESC,
    EPISODE_COUNT_ASC,
}

enum class DisplayStyle {
    POSTER_GRID,
    BANNER_ROW,
    LIST_COMPACT,
    LIST_DETAILED,
    CAROUSEL,
}

@Serializable
data class RowPreview(
    val row: CustomHomeRow,
    val sampleItems: List<PreviewItem>,
    val totalCount: Int,
)

@Serializable
data class PreviewItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

@Serializable
data class HomeRowConfig(
    val rows: List<CustomHomeRow> = emptyList(),
    val version: Int = 1,
)