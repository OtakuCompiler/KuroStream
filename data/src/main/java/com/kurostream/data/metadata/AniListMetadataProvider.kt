// This file is part of KuroStream.
//
// AniListMetadataProvider — AniList GraphQL v2 integration.
// Fixes applied in this pass:
//   - getAnimeByExternalId now passes proper Int variables (idMal/id) instead
//     of embedding the type prefix in the id string (type mismatch crash).
//   - mapMediaType now reads media.format instead of media.status so the
//     anime type (TV/MOVIE/OVA/etc.) is mapped correctly.
//   - getAnime parses the id parameter to Int (AniList id field is Int).
//   - GraphQL queries now request the `format` field.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import com.kurostream.data.remote.api.AniListApi
import com.kurostream.data.remote.dto.anilist.*
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListMetadataProvider @Inject constructor(
    private val api: AniListApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "anilist"
    override val providerName = "AniList"
    override val priority = 1
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L // 24 hours

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("anilist_anime_$id", cacheTtlMs) {
            try {
                val anilistId = id.toIntOrNull()
                if (anilistId == null) {
                    return@getOrFetch MetadataResult.Error("Invalid AniList id: $id")
                }
                val response = api.getAnimeDetails(
                    AniListAnimeDetailsRequest(variables = mapOf("id" to anilistId))
                )
                val media = response.body()?.data?.Media
                if (media == null) MetadataResult.NotFound
                else MetadataResult.Success(mapToDomain(media))
            } catch (e: Exception) {
                Timber.e(e, "AniList getAnime failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anilist_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(
                    AniListSearchRequest(variables = mapOf("search" to query, "page" to page, "perPage" to limit))
                )
                val list = response.body()?.data?.Page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniList searchAnime failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("anilist_external_${type.name}_$value", cacheTtlMs) {
            try {
                when (type) {
                    ExternalIdType.MAL_ID -> {
                        val malId = value.toIntOrNull()
                        if (malId == null) return@getOrFetch MetadataResult.Error("Invalid MAL id: $value")
                        val response = api.getAnimeDetails(
                            AniListAnimeDetailsRequest(variables = mapOf("idMal" to malId))
                        )
                        val media = response.body()?.data?.Media
                        if (media != null) MetadataResult.Success(mapToDomain(media)) else MetadataResult.NotFound
                    }
                    ExternalIdType.ANILIST_ID -> {
                        val alId = value.toIntOrNull()
                        if (alId == null) return@getOrFetch MetadataResult.Error("Invalid AniList id: $value")
                        val response = api.getAnimeDetails(
                            AniListAnimeDetailsRequest(variables = mapOf("id" to alId))
                        )
                        val media = response.body()?.data?.Media
                        if (media != null) MetadataResult.Success(mapToDomain(media)) else MetadataResult.NotFound
                    }
                    ExternalIdType.TMDB_ID -> {
                        // AniList doesn't expose idTmdb as a direct Media filter; fall back
                        // to search by title once we have it — for now return NotFound so
                        // the next provider in the chain (TMDB itself) can take over.
                        return@getOrFetch MetadataResult.NotFound
                    }
                    else -> return@getOrFetch MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "AniList getAnimeByExternalId failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anilist_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.searchAnime(
                    AniListSearchRequest(variables = mapOf(
                        "search" to "",
                        "page" to 1,
                        "perPage" to 50,
                        "season" to season.name.lowercase(),
                        "seasonYear" to year
                    ))
                )
                val list = response.body()?.data?.Page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniList getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anilist_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.getTrendingAnime(
                    AniListTrendingRequest(variables = mapOf("page" to 1, "perPage" to limit))
                )
                val list = response.body()?.data?.Page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "AniList getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    private fun mapToDomain(media: AniListMedia): AnimeMetadata {
        return AnimeMetadata(
            id = "anilist_${media.id}",
            title = media.title?.romaji ?: "Unknown",
            titleEnglish = media.title?.english,
            titleJapanese = media.title?.native,
            description = media.description?.replace("<br>", "\n")?.replace("<i>", "")?.replace("</i>", ""),
            coverImageUrl = media.coverImage?.large ?: media.coverImage?.medium,
            bannerImageUrl = media.bannerImage,
            type = mapMediaType(media.format),
            status = mapStatus(media.status),
            episodes = media.episodes,
            durationMinutes = media.duration,
            startDate = null,
            endDate = null,
            seasonYear = media.seasonYear,
            season = media.season?.let { Season.valueOf(it.uppercase()) },
            genres = media.genres ?: emptyList(),
            studios = emptyList(),
            score = media.averageScore?.div(10.0),
            scoredBy = null,
            rank = null,
            popularity = null,
            favorites = null,
            ageRating = null,
            sourceMaterial = null,
            trailerUrl = null,
            externalLinks = emptyList(),
            characters = emptyList(),
            staff = emptyList(),
            relations = emptyList(),
            themes = AnimeThemes(),
            statistics = null,
            synonyms = emptyList(),
            providerId = providerId,
        )
    }

    private fun mapMediaType(format: String?): MediaType = when (format?.uppercase()) {
        "TV" -> MediaType.TV
        "MOVIE" -> MediaType.MOVIE
        "OVA" -> MediaType.OVA
        "ONA" -> MediaType.ONA
        "SPECIAL" -> MediaType.SPECIAL
        "MUSIC" -> MediaType.MUSIC
        "ONE_SHOT" -> MediaType.SPECIAL
        "MANGA" -> MediaType.MANGA
        "NOVEL" -> MediaType.NOVEL
        else -> MediaType.UNKNOWN
    }

    private fun mapStatus(status: String?): AiringStatus = when (status?.uppercase()) {
        "RELEASING" -> AiringStatus.AIRING
        "FINISHED" -> AiringStatus.FINISHED
        "NOT_YET_RELEASED" -> AiringStatus.NOT_YET_AIRED
        "CANCELLED" -> AiringStatus.CANCELLED
        "HIATUS" -> AiringStatus.AIRING
        else -> AiringStatus.UNKNOWN
    }
}
