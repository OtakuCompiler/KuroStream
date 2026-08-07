// This file is part of KuroStream.
//
// Shared base for TMDB watch-provider-backed metadata providers.
// Netflix, Prime Video, and Disney+ all resolve via TMDB's
// `discoverTvByProvider` endpoint and map the same TvShow DTO shape.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.data.remote.dto.tmdb.TvShow
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber

abstract class TmdbWatchProviderMetadataProvider(
    private val api: TmdbApi,
    private val cache: CacheRepository,
    providerId: String,
    providerName: String,
    priority: Int,
    private val watchProviderId: String,
) : MetadataProvider {

    override val providerId = providerId
    override val providerName = providerName
    override val priority = priority
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("${providerId}_anime_$id", cacheTtlMs) {
            try {
                val tmdbId = id.removePrefix("tmdb_").toIntOrNull() ?: return@getOrFetch MetadataResult.NotFound
                val response = api.getTvDetails(tmdbId)
                response.body()?.let { MetadataResult.Success(mapToDomain(it)) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "$providerName getAnime failed")
                MetadataResult.Error(e.message ?: "$providerName error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("${providerId}_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchTv(query, page)
                MetadataResult.Success(response.body()?.results?.map { mapToDomain(it) } ?: emptyList())
            } catch (e: Exception) {
                Timber.e(e, "$providerName searchAnime failed")
                MetadataResult.Error(e.message ?: "$providerName error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return MetadataResult.NotFound
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("${providerId}_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.discoverTvByProvider(
                    withWatchProviders = watchProviderId,
                    watchRegion = "US",
                    page = 1,
                )
                MetadataResult.Success(response.body()?.results?.map { mapToDomain(it) } ?: emptyList())
            } catch (e: Exception) {
                Timber.e(e, "$providerName getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "$providerName error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("${providerId}_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.discoverTvByProvider(
                    withWatchProviders = watchProviderId,
                    watchRegion = "US",
                    page = 1,
                )
                MetadataResult.Success(response.body()?.results?.take(limit)?.map { mapToDomain(it) } ?: emptyList())
            } catch (e: Exception) {
                Timber.e(e, "$providerName getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "$providerName error", throwable = e)
            }
        }
    }

    protected fun mapToDomain(dto: TvShow): AnimeMetadata {
        val posterPath = dto.posterPath
        val backdropPath = dto.backdropPath
        val coverImage = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val bannerImage = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }

        val externalLinks = dto.externalIds?.let { ids ->
            mutableListOf<ExternalLink>().apply {
                ids.imdbId?.let { add(ExternalLink("imdb", "https://imdb.com/title/$it")) }
                ids.facebookId?.let { add(ExternalLink("facebook", "https://facebook.com/$it")) }
                ids.instagramId?.let { add(ExternalLink("instagram", "https://instagram.com/$it")) }
                ids.twitterId?.let { add(ExternalLink("twitter", "https://twitter.com/$it")) }
            }
        } ?: emptyList()

        return AnimeMetadata(
            id = "tmdb_${dto.id}",
            title = dto.name,
            titleEnglish = dto.originalName,
            titleJapanese = if (dto.originCountry.contains("JP")) dto.name else null,
            synonyms = emptyList(),
            description = dto.overview,
            coverImageUrl = coverImage,
            bannerImageUrl = bannerImage,
            type = MediaType.TV,
            status = mapStatus(dto.status),
            episodes = dto.numberOfEpisodes,
            durationMinutes = dto.episodeRunTime?.firstOrNull(),
            startDate = dto.firstAirDate?.let { parseDate(it) },
            endDate = dto.lastAirDate?.let { parseDate(it) },
            seasonYear = dto.firstAirDate?.take(4)?.toIntOrNull(),
            season = dto.firstAirDate?.take(7)?.let { parseSeason(it) },
            genres = dto.genres.map { it.name },
            studios = dto.networks.map { it.name },
            score = dto.voteAverage,
            scoredBy = dto.voteCount,
            rank = dto.popularity?.toInt(),
            popularity = dto.popularity?.toInt(),
            favorites = null,
            ageRating = dto.contentRatings?.results?.firstOrNull()?.rating,
            sourceMaterial = null,
            trailerUrl = dto.videos?.results?.firstOrNull { it.site == "YouTube" }?.let { "https://youtube.com/watch?v=${it.key}" },
            externalLinks = externalLinks,
            characters = emptyList(),
            staff = emptyList(),
            relations = emptyList(),
            themes = AnimeThemes(),
            statistics = null,
            providerId = providerId,
        )
    }

    private fun mapStatus(status: String?): AiringStatus = when (status) {
        "Returning Series" -> AiringStatus.AIRING
        "Ended" -> AiringStatus.FINISHED
        "In Production" -> AiringStatus.AIRING
        "Planned" -> AiringStatus.NOT_YET_AIRED
        "Canceled" -> AiringStatus.CANCELLED
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
