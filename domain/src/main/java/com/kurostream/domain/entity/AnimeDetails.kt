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

import kotlinx.serialization.Serializable

@Serializable
data class AnimeDetails(
    val mediaItem: MediaItem,
    val episodes: List<Episode> = emptyList(),
    val relatedAnime: List<MediaItem> = emptyList(),
    val characters: List<Character> = emptyList(),
    val staff: List<Staff> = emptyList(),
    val themes: AnimeThemes = AnimeThemes(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val statistics: AnimeStatistics? = null
)

@Serializable
data class Episode(
    val id: String, val mediaId: String, val number: Int,
    val title: String? = null, val synopsis: String? = null,
    val thumbnailUrl: String? = null, val durationSeconds: Int? = null,
    val airedAt: Long? = null, val isFiller: Boolean = false,
    val isRecap: Boolean = false, val videoSources: List<VideoSource> = emptyList()
)

@Serializable
data class VideoSource(
    val id: String, val quality: VideoQuality, val url: String,
    val headers: Map<String, String> = emptyMap(),
    val isHls: Boolean = false, val isDash: Boolean = false
)

@Serializable
data class Character(val id: String, val name: String, val role: String, val imageUrl: String? = null, val voiceActor: String? = null)

@Serializable
data class Staff(val id: String, val name: String, val role: String, val imageUrl: String? = null)

@Serializable
data class AnimeThemes(val openingThemes: List<String> = emptyList(), val endingThemes: List<String> = emptyList())

@Serializable
data class ExternalLink(val siteName: String, val url: String)

@Serializable
data class AnimeStatistics(
    val scoreDistribution: Map<Int, Int> = emptyMap(),
    val statusDistribution: Map<String, Int> = emptyMap(),
    val totalMembers: Int = 0, val totalFavorites: Int = 0
)
