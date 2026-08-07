package com.kurostream.data.metadata

import com.kurostream.data.remote.api.AnnApi
import com.kurostream.data.remote.dto.ann.AnnAnimeResponse
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnMetadataProvider @Inject constructor(
    private val api: AnnApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "ann"
    override val providerName = "Anime News Network"
    override val priority = 8
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("ann_anime_$id", cacheTtlMs) {
            try {
                val response = api.getAnime(id)
                val anime = response.body()
                if (anime != null) {
                    MetadataResult.Success(mapToDomain(anime))
                } else {
                    MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "ANN getAnime failed")
                MetadataResult.Error(e.message ?: "ANN error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("ann_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query, page, limit)
                val list = response.body()?.results?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "ANN searchAnime failed")
                MetadataResult.Error(e.message ?: "ANN error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("ann_external_${type.name}_$value", cacheTtlMs) {
            try {
                when (type) {
                    ExternalIdType.ANILIST_ID, ExternalIdType.MAL_ID -> {
                        val response = api.searchAnime(value)
                        response.body()?.results?.firstOrNull()?.let { MetadataResult.Success(mapToDomain(it)) } ?: MetadataResult.NotFound
                    }
                    else -> MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "ANN getAnimeByExternalId failed")
                MetadataResult.Error(e.message ?: "ANN error", throwable = e)
            }
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("ann_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.searchAnime("", 1, 50)
                val list = response.body()?.results?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "ANN getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "ANN error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("ann_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime("", 1, limit)
                val list = response.body()?.results?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "ANN getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "ANN error", throwable = e)
            }
        }
    }

    private fun mapToDomain(dto: AnnAnimeResponse): AnimeMetadata {
        return AnimeMetadata(
            id = "ann_${dto.id}",
            title = dto.title ?: "Unknown",
            titleEnglish = dto.titleEnglish,
            titleJapanese = dto.titleJapanese,
            synonyms = dto.synonyms,
            description = dto.description,
            coverImageUrl = dto.image,
            bannerImageUrl = dto.banner,
            type = mapMediaType(dto.type),
            status = mapStatus(dto.status),
            episodes = dto.episodes,
            durationMinutes = null,
            startDate = null,
            endDate = null,
            seasonYear = dto.year,
            season = dto.season?.let { parseSeason(it) },
            genres = dto.genres,
            studios = dto.studios,
            score = dto.score,
            scoredBy = null,
            rank = null,
            popularity = dto.popularity,
            favorites = null,
            ageRating = dto.rating,
            sourceMaterial = null,
            trailerUrl = dto.trailer,
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

    private fun parseSeason(season: String): Season = when (season.lowercase()) {
        "winter" -> Season.WINTER
        "spring" -> Season.SPRING
        "summer" -> Season.SUMMER
        "fall", "autumn" -> Season.FALL
        else -> Season.WINTER
    }
}
