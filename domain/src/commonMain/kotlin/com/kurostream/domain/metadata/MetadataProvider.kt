package com.kurostream.domain.metadata
import com.kurostream.core.platform.platformCurrentTimeMillis

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface MetadataProvider {
    val providerId: String
    val providerName: String
    val priority: Int
    val isEnabled: Boolean

    suspend fun getAnime(id: String): MetadataResult<AnimeMetadata>
    suspend fun searchAnime(query: String, page: Int = 1, limit: Int = 20): MetadataResult<List<AnimeMetadata>>
    suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata>
    suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>>
    suspend fun getTrendingAnime(limit: Int = 20): MetadataResult<List<AnimeMetadata>>
}

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
    val lastUpdated: Long = platformCurrentTimeMillis(),
    val providerId: String,
)

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
    val providerData: Map<String, String> = emptyMap(),
    val lastUpdated: Long = platformCurrentTimeMillis(),
)

enum class MediaType { TV, MOVIE, OVA, ONA, SPECIAL, MUSIC, UNKNOWN }
enum class AiringStatus { AIRING, FINISHED, NOT_YET_AIRED, CANCELLED, UNKNOWN }

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
    val relatedAnimeId: String,
    val relatedTitle: String,
    val targetId: String = "",
    val targetTitle: String = "",
    val targetType: MediaType? = null,
)

@Serializable
data class AnimeThemes(
    val openings: List<ThemeSong> = emptyList(),
    val endings: List<ThemeSong> = emptyList(),
    val openingThemes: List<String> = emptyList(),
    val endingThemes: List<String> = emptyList(),
)

@Serializable
data class ThemeSong(
    val title: String,
    val artist: String,
    val episodes: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
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
    val totalMembers: Int = 0,
    val totalFavorites: Int = 0,
)

sealed interface MetadataResult<out T> {
    data class Success<T>(val data: T) : MetadataResult<T>
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val providerErrors: Map<String, String> = emptyMap(),
    ) : MetadataResult<Nothing>
    data class RateLimited(val retryAfterMs: Long) : MetadataResult<Nothing>
    data class Partial<T>(
        val data: T,
        val missingProviders: List<String>,
        val providerErrors: Map<String, String>,
    ) : MetadataResult<T>
    object NotFound : MetadataResult<Nothing>
}
