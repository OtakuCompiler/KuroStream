package com.kurostream.data.metadata

import com.kurostream.data.remote.api.AniDbApi
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniDbMetadataProvider @Inject constructor(
    private val api: AniDbApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "anidb"
    override val providerName = "AniDB"
    override val priority = 7
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("anidb_anime_$id", cacheTtlMs) {
            try {
                val response = api.getAnime(id)
                val anime = response.body()
                if (anime != null) {
                    MetadataResult.Success(mapToDomain(anime))
                } else {
                    MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "AniDB getAnime failed")
                MetadataResult.Error(e.message ?: "AniDB error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anidb_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query)
                val list = response.body()?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniDB searchAnime failed")
                MetadataResult.Error(e.message ?: "AniDB error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("anidb_external_${type.name}_$value", cacheTtlMs) {
            try {
                when (type) {
                    ExternalIdType.ANILIST_ID -> {
                        val response = api.getAnimeByAniListId(value.toIntOrNull() ?: return@getOrFetch MetadataResult.NotFound)
                        response.body()?.firstOrNull()?.let { MetadataResult.Success(mapToDomain(it)) } ?: MetadataResult.NotFound
                    }
                    ExternalIdType.MAL_ID -> {
                        val response = api.getAnimeByMalId(value.toIntOrNull() ?: return@getOrFetch MetadataResult.NotFound)
                        response.body()?.firstOrNull()?.let { MetadataResult.Success(mapToDomain(it)) } ?: MetadataResult.NotFound
                    }
                    else -> MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "AniDB getAnimeByExternalId failed")
                MetadataResult.Error(e.message ?: "AniDB error", throwable = e)
            }
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anidb_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.getSeasonalAnime(year, season.name.lowercase())
                val list = response.body()?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniDB getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "AniDB error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anidb_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.getPopularAnime(limit)
                val list = response.body()?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniDB getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "AniDB error", throwable = e)
            }
        }
    }

    private fun mapToDomain(dto: com.kurostream.data.remote.dto.anidb.AniDbAnime): AnimeMetadata {
        return AnimeMetadata(
            id = "anidb_${dto.id}",
            title = dto.title ?: "Unknown",
            titleEnglish = dto.titleEnglish,
            titleJapanese = dto.titleJapanese,
            synonyms = dto.synonymsList ?: emptyList(),
            description = dto.description,
            coverImageUrl = dto.image ?: dto.picture,
            bannerImageUrl = null,
            type = mapMediaType(dto.type),
            status = mapStatus(dto.status),
            episodes = dto.episodeCount,
            durationMinutes = dto.episodeLength,
            startDate = dto.startDate?.let { parseDate(it) },
            endDate = dto.endDate?.let { parseDate(it) },
            seasonYear = dto.startDate?.take(4)?.toIntOrNull(),
            season = dto.startDate?.let { parseSeason(it) },
            genres = dto.genres ?: emptyList(),
            studios = dto.studios ?: emptyList(),
            score = dto.rating?.toDoubleOrNull(),
            scoredBy = dto.ratingCount,
            rank = null,
            popularity = null,
            favorites = null,
            ageRating = dto.rating,
            sourceMaterial = null,
            trailerUrl = null,
            externalLinks = emptyList(),
            characters = emptyList(),
            staff = emptyList(),
            relations = emptyList(),
            themes = AnimeThemes(),
            statistics = null,
            providerId = providerId,
        )
    }

    private fun mapMediaType(type: String?): MediaType = when (type?.uppercase()) {
        "TV" -> MediaType.TV
        "MOVIE" -> MediaType.MOVIE
        "OVA" -> MediaType.OVA
        "ONA" -> MediaType.ONA
        "SPECIAL" -> MediaType.SPECIAL
        "MUSIC" -> MediaType.MUSIC
        else -> MediaType.UNKNOWN
    }

    private fun mapStatus(status: String?): AiringStatus = when (status?.uppercase()) {
        "FINISHED" -> AiringStatus.FINISHED
        "ONGOING" -> AiringStatus.AIRING
        "UPCOMING" -> AiringStatus.NOT_YET_AIRED
        "CANCELLED" -> AiringStatus.CANCELLED
        else -> AiringStatus.UNKNOWN
    }

    private fun parseDate(dateStr: String): Long = try {
        java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    private fun parseSeason(dateStr: String): Season {
        val month = dateStr.substring(5, 7).toIntOrNull() ?: return Season.WINTER
        return when (month) {
            1, 2, 3 -> Season.WINTER
            4, 5, 6 -> Season.SPRING
            7, 8, 9 -> Season.SUMMER
            else -> Season.FALL
        }
    }
}
