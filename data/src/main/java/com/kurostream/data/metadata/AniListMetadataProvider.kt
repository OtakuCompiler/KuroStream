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
                val response = api.getAnimeDetails(AniListAnimeDetailsRequest(variables = mapOf("id" to id)))
                val media = response.body()?.data?.Media
                if (media == null) {
                    MetadataResult.NotFound
                } else {
                    MetadataResult.Success(mapToDomain(media))
                }
            } catch (e: Exception) {
                Timber.e(e, "AniList getAnime failed")
                MetadataResult.Error(e.message ?: "AniList error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("anilist_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(AniListSearchRequest(variables = mapOf("search" to query, "page" to page, "perPage" to limit)))
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
                val query = when (type) {
                    ExternalIdType.MAL_ID -> "idMal:$value"
                    ExternalIdType.ANILIST_ID -> "id:$value"
                    ExternalIdType.TMDB_ID -> "idTmdb:$value"
                    else -> return@getOrFetch MetadataResult.NotFound
                }
                val response = api.getAnimeDetails(AniListAnimeDetailsRequest(variables = mapOf("id" to query)))
                val media = response.body()?.data?.Media
                if (media != null) MetadataResult.Success(mapToDomain(media)) else MetadataResult.NotFound
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
                val response = api.getTrendingAnime(AniListTrendingRequest(variables = mapOf("page" to 1, "perPage" to limit)))
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
            type = mapMediaType(media.status),
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

    private fun mapMediaType(status: String?): MediaType = when (status?.uppercase()) {
        "TV" -> MediaType.TV
        "MOVIE" -> MediaType.MOVIE
        "OVA" -> MediaType.OVA
        "ONA" -> MediaType.ONA
        "SPECIAL" -> MediaType.SPECIAL
        "MUSIC" -> MediaType.MUSIC
        else -> MediaType.TV
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
