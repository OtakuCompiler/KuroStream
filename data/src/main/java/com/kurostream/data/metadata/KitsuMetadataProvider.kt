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

import com.kurostream.data.remote.dto.kitsu.KitsuModels
import com.kurostream.data.remote.api.KitsuApi
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

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

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "kitsu_anime_$id"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getAnime(id)
                response.data?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getAnime failed")
                throw e
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "kitsu_search_${query}_$page"
        cache.getOrFetch(cacheKey, 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query, page, limit)
                response.data?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Kitsu searchAnime failed")
                throw e
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        MetadataResult.NotFound // Kitsu doesn't support external ID lookup easily
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "kitsu_seasonal_${year}_${season.name}"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getSeasonalAnime(year, season.name.lowercase())
                response.data?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getSeasonalAnime failed")
                throw e
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "kitsu_trending_$limit"
        cache.getOrFetch(cacheKey, 6 * 60 * 60 * 1000L) {
            try {
                val response = api.getTrendingAnime(limit)
                response.data?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Kitsu getTrendingAnime failed")
                throw e
            }
        }
    }

    private fun mapToDomain(dto: KitsuModels.Anime): AnimeMetadata {
        val attributes = dto.attributes
        val coverImage = attributes.coverImage?.original ?: attributes.posterImage?.original
        val bannerImage = attributes.coverImage?.original

        val externalLinks = attributes.links?.map { link ->
            ExternalLink(link.key, link.value)
        } ?: emptyList()

        val characters = emptyList<Character>()
        val staff = emptyList<Staff>()
        val relations = emptyList<AnimeRelation>()
        val themes = AnimeThemes()
        val stats = attributes.popularityRank?.let { 
            AnimeStatistics(
                popularity = it,
                rank = attributes.ratingRank
            )
        }

        return AnimeMetadata(
            id = "kitsu_${dto.id}",
            title = attributes.canonicalTitle,
            titleEnglish = attributes.titles.en_jp ?: attributes.titles.en,
            titleJapanese = attributes.titles.ja_jp,
            synonyms = attributes.abbreviatedTitles ?: emptyList(),
            description = attributes.synopsis,
            coverImageUrl = coverImage,
            bannerImageUrl = bannerImage,
            type = mapMediaType(attributes.subtype),
            status = mapStatus(attributes.status),
            episodes = attributes.episodeCount,
            durationMinutes = attributes.episodeLength,
            startDate = attributes.startDate?.let { parseDate(it) },
            endDate = attributes.endDate?.let { parseDate(it) },
            seasonYear = attributes.startDate?.take(4)?.toIntOrNull(),
            season = attributes.startDate?.take(7)?.let { parseSeason(it) },
            genres = attributes.tags?.map { it.attributes?.name }.filterNotNull() ?: emptyList(),
            studios = emptyList(),
            score = attributes.averageRating?.toDouble()?.div(10.0),
            scoredBy = attributes.userCount,
            rank = attributes.ratingRank,
            popularity = attributes.popularityRank,
            favorites = attributes.favoritesCount,
            ageRating = attributes.ageRating,
            sourceMaterial = attributes.showType,
            trailerUrl = attributes.youtubeVideoId?.let { "https://youtube.com/watch?v=$it" },
            externalLinks = externalLinks,
            characters = characters,
            staff = staff,
            relations = relations,
            themes = themes,
            statistics = stats,
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