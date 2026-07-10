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

import com.kurostream.data.remote.api.TvdbApi
import com.kurostream.data.remote.dto.tvdb.TvdbDtos
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TvdbMetadataProvider @Inject constructor(
    private val api: TvdbApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "tvdb"
    override val providerName = "TVDB"
    override val priority = 5
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "tvdb_anime_$id"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getSeries(id)
                response.data?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "TVDB getAnime failed")
                throw e
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "tvdb_search_${query}_$page"
        cache.getOrFetch(cacheKey, 60 * 60 * 1000L) {
            try {
                val response = api.searchSeries(query, page, limit)
                MetadataResult.Success(response.data?.map { mapToDomain(it) } ?: emptyList())
            } catch (e: Exception) {
                Timber.e(e, "TVDB searchAnime failed")
                throw e
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        MetadataResult.NotFound
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        MetadataResult.Success(emptyList())
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        MetadataResult.Success(emptyList())
    }

    private fun mapToDomain(dto: TvdbDtos.Series): AnimeMetadata {
        return AnimeMetadata(
            id = "tvdb_${dto.id}",
            title = dto.name,
            titleEnglish = dto.aliases?.firstOrNull { it.language == "en" }?.name,
            titleJapanese = dto.aliases?.firstOrNull { it.language == "ja" }?.name,
            synonyms = dto.aliases?.map { it.name } ?: emptyList(),
            description = dto.overview,
            coverImageUrl = dto.imageUrl?.let { "https://artworks.thetvdb.com/$it" },
            bannerImageUrl = dto.bannerUrl?.let { "https://artworks.thetvdb.com/$it" },
            type = mapMediaType(dto.type),
            status = mapStatus(dto.status),
            startDate = dto.firstAired?.let { parseDate(it) },
            endDate = dto.lastAired?.let { parseDate(it) },
            seasonYear = dto.firstAired?.take(4)?.toIntOrNull(),
            season = dto.firstAired?.take(7)?.let { parseSeason(it) },
            genres = dto.genres?.map { it.name } ?: emptyList(),
            studios = dto.network?.let { listOf(it) } ?: emptyList(),
            score = dto.siteRating?.toDouble()?.div(10.0),
            scoredBy = null,
            rank = null,
            popularity = dto.scoreCount,
            favorites = null,
            ageRating = dto.contentRating,
            sourceMaterial = null,
            durationMinutes = dto.runtime?.div(60)?.toInt(),
            episodeCount = dto.episodeCount,
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

    private fun mapMediaType(type: String?): MediaType = when (type?.lowercase()) {
        "series" -> MediaType.TV
        "movie" -> MediaType.MOVIE
        "miniseries" -> MediaType.SPECIAL
        else -> MediaType.TV
    }

    private fun mapStatus(status: String?): AiringStatus = when (status?.lowercase()) {
        "continuing" -> AiringStatus.AIRING
        "ended" -> AiringStatus.FINISHED
        "upcoming" -> AiringStatus.NOT_YET_AIRED
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