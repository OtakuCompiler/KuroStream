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

import com.kurostream.data.remote.api.KitsuApi
import com.kurostream.data.remote.dto.kitsu.*
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KitsuMetadataProvider @Inject constructor(
    private val api: KitsuApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "kitsu"
    override val providerName = "Kitsu"
    override val priority = 2
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("kitsu_anime_$id", cacheTtlMs) {
            try {
                val response = api.getAnimeDetails(id)
                val data = response.body()?.data
                if (data != null) {
                    MetadataResult.Success(mapToDomain(data))
                } else {
                    MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getAnime failed")
                MetadataResult.Error(e.message ?: "Kitsu error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("kitsu_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query, limit)
                val list = response.body()?.data?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "Kitsu searchAnime failed")
                MetadataResult.Error(e.message ?: "Kitsu error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return MetadataResult.NotFound
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("kitsu_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.searchAnime("", 50)
                val list = response.body()?.data?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "Kitsu error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("kitsu_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime("", limit)
                val list = response.body()?.data?.mapNotNull { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "Kitsu error", throwable = e)
            }
        }
    }

    private fun mapToDomain(dto: AnimeData): AnimeMetadata {
        val attr = dto.attributes ?: return AnimeMetadata(
            id = "kitsu_${dto.id}",
            title = "Unknown",
            type = MediaType.UNKNOWN,
            status = AiringStatus.UNKNOWN,
            providerId = providerId,
        )

        val coverImage = attr.coverImage?.original ?: attr.posterImage?.original
        val bannerImage = attr.coverImage?.original

        return AnimeMetadata(
            id = "kitsu_${dto.id}",
            title = attr.canonicalTitle ?: "Unknown",
            titleEnglish = attr.titles?.en_jp ?: attr.titles?.en,
            titleJapanese = attr.titles?.ja_jp,
            description = attr.synopsis,
            coverImageUrl = coverImage,
            bannerImageUrl = bannerImage,
            type = mapMediaType(attr.subtype),
            status = mapStatus(attr.status),
            episodes = attr.episodeCount,
            durationMinutes = attr.episodeLength,
            startDate = attr.startDate?.let { parseDate(it) },
            endDate = attr.endDate?.let { parseDate(it) },
            seasonYear = attr.startDate?.take(4)?.toIntOrNull(),
            season = attr.startDate?.let { parseSeason(it) },
            genres = emptyList(),
            studios = emptyList(),
            score = attr.averageRating?.toDoubleOrNull()?.div(10.0),
            scoredBy = null,
            rank = null,
            popularity = null,
            favorites = null,
            ageRating = attr.ageRating,
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

    private fun mapMediaType(subtype: String?): MediaType = when (subtype) {
        "TV" -> MediaType.TV
        "movie" -> MediaType.MOVIE
        "OVA" -> MediaType.OVA
        "ONA" -> MediaType.ONA
        "special" -> MediaType.SPECIAL
        "music" -> MediaType.MUSIC
        else -> MediaType.TV
    }

    private fun mapStatus(status: String?): AiringStatus = when (status) {
        "current" -> AiringStatus.AIRING
        "finished" -> AiringStatus.FINISHED
        "upcoming" -> AiringStatus.NOT_YET_AIRED
        "unreleased" -> AiringStatus.NOT_YET_AIRED
        "canceled" -> AiringStatus.CANCELLED
        "on_hold" -> AiringStatus.AIRING
        else -> AiringStatus.UNKNOWN
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
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
