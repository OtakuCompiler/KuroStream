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

import com.kurostream.domain.model.AnimeDetails
import com.kurostream.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface UnifiedMetadataRepository {
    suspend fun getAnimeDetails(id: String): MetadataResult<UnifiedAnimeDetails>
    suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<UnifiedAnimeDetails>>
    suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<UnifiedAnimeDetails>>
    suspend fun getTrendingAnime(limit: Int): MetadataResult<List<UnifiedAnimeDetails>>
    suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<UnifiedAnimeDetails>
    
    fun observeEnabledProviders(): Flow<List<MetadataProvider>>
    suspend fun setProviderEnabled(providerId: String, enabled: Boolean)
    suspend fun setProviderPriority(providerId: String, priority: Int)
}

@Serializable
data class UnifiedAnimeDetails(
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
    val startDate: Long? = null,
    val endDate: Long? = null,
    val season: Season? = null,
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val score: Double? = null,
    val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val favorites: Int? = null,
    val ageRating: String? = null,
    val sourceMaterial: String? = null,
    val durationMinutes: Int? = null,
    val episodeCount: Int? = null,
    val trailerUrl: String? = null,
    val externalLinks: List<ExternalLink> = emptyList(),
    val characters: List<Character> = emptyList(),
    val staff: List<Staff> = emptyList(),
    val relations: List<AnimeRelation> = emptyList(),
    val themes: AnimeThemes = AnimeThemes(),
    val statistics: AnimeStatistics? = null,
    val providerData: Map<String, Any> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Serializable
data class ExternalLink(
    val site: String,
    val url: String,
    val type: String = "info",
)

@Serializable
data class Character(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String? = null,
    val voiceActors: List<VoiceActor> = emptyList(),
)

@Serializable
data class VoiceActor(
    val id: String,
    val name: String,
    val language: String,
    val imageUrl: String? = null,
)

@Serializable
data class Staff(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String? = null,
)

@Serializable
data class AnimeRelation(
    val relationType: String,
    val targetId: String,
    val targetTitle: String,
    val targetType: MediaType,
)

@Serializable
data class AnimeThemes(
    val openingThemes: List<String> = emptyList(),
    val endingThemes: List<String> = emptyList(),
)

@Serializable
data class AnimeStatistics(
    val scoreDistribution: Map<Int, Int> = emptyMap(),
    val statusDistribution: Map<String, Int> = emptyMap(),
    val totalMembers: Int = 0,
    val totalFavorites: Int = 0,
)

sealed interface MetadataResult<out T> {
    data class Success<T>(val data: T) : MetadataResult<T>
    data class Error(val message: String, val providerErrors: Map<String, String> = emptyMap()) : MetadataResult<Nothing>
    data class Partial<T>(val data: T, val missingProviders: List<String>, val providerErrors: Map<String, String>) : MetadataResult<T>
}

enum class MediaType { TV, MOVIE, OVA, ONA, SPECIAL, MUSIC, UNKNOWN }
enum class AiringStatus { AIRING, FINISHED, NOT_YET_AIRED, CANCELLED, UNKNOWN }
enum class Season { WINTER, SPRING, SUMMER, FALL }
enum class ExternalIdType { MAL_ID, ANILIST_ID, KITSU_ID, TMDB_ID, TVDB_ID, IMDB_ID }