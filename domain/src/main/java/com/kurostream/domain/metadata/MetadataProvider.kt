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

package com.kurostream.domain.metadata

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Unified metadata provider interface
 */
interface MetadataProvider {
    val providerId: String
    val providerName: String
    val priority: Int  // Lower = higher priority
    val isEnabled: Boolean
    
    suspend fun getAnime(id: String): MetadataResult<AnimeMetadata>
    suspend fun searchAnime(query: String, page: Int = 1, limit: Int = 20): MetadataResult<List<AnimeMetadata>>
    suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata>
    suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>>
    suspend fun getTrendingAnime(limit: Int = 20): MetadataResult<List<AnimeMetadata>>
}

enum class ExternalIdType { MAL_ID, ANILIST_ID, KITSU_ID, TMDB_ID, TVDB_ID, IMDB_ID }

enum class Season { WINTER, SPRING, SUMMER, FALL }

@Serializable
data class AnimeMetadata(
    val id: String,
    val title: String,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val synonyms: List<String> = emptyList(),
    val description: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val type: MediaType = MediaType.TV,
    val status: AiringStatus = AiringStatus.UNKNOWN,
    val episodes: Int? = null,
    val durationMinutes: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val seasonYear: Int? = null,
    val season: Season? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val score: Double? = null,
    val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val favorites: Int? = null,
    val ageRating: String? = null,
    val sourceMaterial: String? = null,
    val trailerUrl: String? = null,
    val externalLinks: List<ExternalLink> = emptyList(),
    val characters: List<Character> = emptyList(),
    val staff: List<Staff> = emptyList(),
    val relations: List<AnimeRelation> = emptyList(),
    val themes: AnimeThemes = AnimeThemes(),
    val statistics: AnimeStatistics? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val providerId: String,
)

enum class MediaType { TV, MOVIE, OVA, ONA, SPECIAL, MUSIC }
enum class AiringStatus { AIRING, FINISHED, NOT_YET_AIRED, CANCELLED, UNKNOWN }

@Serializable
data class ExternalLink(val site: String, val url: String)

@Serializable
data class Character(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String? = null,
    val voiceActors: List<VoiceActor> = emptyList()
)

@Serializable
data class VoiceActor(
    val id: String,
    val name: String,
    val language: String,
    val imageUrl: String? = null
)

@Serializable
data class Staff(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String? = null
)

@Serializable
data class AnimeRelation(
    val relationType: String,
    val relatedAnimeId: String,
    val relatedTitle: String
)

@Serializable
data class AnimeThemes(
    val openings: List<ThemeSong> = emptyList(),
    val endings: List<ThemeSong> = emptyList()
)

@Serializable
data class ThemeSong(
    val title: String,
    val artist: String,
    val episodes: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null
)

@Serializable
data class AnimeStatistics(
    val watching: Int = 0,
    val completed: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
    val planToWatch: Int = 0,
    val scoreDistribution: Map<Int, Int> = emptyMap(),
    val statusDistribution: Map<String, Int> = emptyMap(),
)

sealed interface MetadataResult<out T> {
    data class Success<T>(val data: T) : MetadataResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : MetadataResult<Nothing>
    data class RateLimited(val retryAfterMs: Long) : MetadataResult<Nothing>
    object NotFound : MetadataResult<Nothing>
}

object MetadataResult {
    fun <T> success(data: T): MetadataResult<T> = Success(data)
    fun <T> error(message: String, throwable: Throwable? = null): MetadataResult<T> = Error(message, throwable)
    fun <T> rateLimited(retryAfterMs: Long): MetadataResult<T> = RateLimited(retryAfterMs)
}