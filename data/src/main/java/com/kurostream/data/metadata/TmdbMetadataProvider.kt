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

import com.kurostream.data.remote.dto.tmdb.TmdbDtos
import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TmdbMetadataProvider @Inject constructor(
    private val api: TmdbApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "tmdb"
    override val providerName = "TMDB"
    override val priority = 4
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_anime_$id"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getTvDetails(id)
                response.data?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "TMDB getAnime failed")
                throw e
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_search_${query}_$page"
        cache.getOrFetch(cacheKey, 60 * 60 * 1000L) {
            try {
                val response = api.searchTv(query, page)
                response.data?.results?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "TMDB searchAnime failed")
                throw e
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_external_${type.name}_$value"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val source = when (type) {
                    ExternalIdType.IMDB_ID -> "imdb_id"
                    ExternalIdType.TVDB_ID -> "tvdb_id"
                    else -> return@withContext MetadataResult.NotFound
                }
                val response = api.findByExternalId(source, value)
                response.data?.tvResults?.firstOrNull()?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "TMDB getAnimeByExternalId failed")
                throw e
            }
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_seasonal_${year}_${season.name}"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getDiscoverTv(page = 1, withKeywords = "anime", firstAirDateYear = year, airDateGte = "${year}-${seasonStartMonth(season)}-01", airDateLte = "${year}-${seasonEndMonth(season)}-31")
                response.data?.results?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "TMDB getSeasonalAnime failed")
                throw e
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "tmdb_trending_$limit"
        cache.getOrFetch(cacheKey, 6 * 60 * 60 * 1000L) {
            try {
                val response = api.getTrendingTv("week")
                response.data?.results?.take(limit)?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "TMDB getTrendingAnime failed")
                throw e
            }
        }
    }

    private fun mapToDomain(dto: TmdbDtos.TvDetail): AnimeMetadata {
        val posterPath = dto.posterPath
        val backdropPath = dto.backdropPath
        val coverImage = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val bannerImage = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }

        val externalLinks = dto.externalIds?.let { ids ->
            mutableListOf<ExternalLink>().apply {
                ids.imdbId?.let { add(ExternalLink("imdb", "https://imdb.com/title/$it")) }
                ids.tvdbId?.let { add(ExternalLink("tvdb", "https://thetvdb.com/series/$it")) }
                ids.facebookId?.let { add(ExternalLink("facebook", "https://facebook.com/$it")) }
                ids.instagramId?.let { add(ExternalLink("instagram", "https://instagram.com/$it")) }
                ids.twitterId?.let { add(ExternalLink("twitter", "https://twitter.com/$it")) }
            }
        } ?: emptyList()

        val characters = dto.credits?.cast?.take(20)?.map { actor ->
            Character(
                id = actor.id.toString(),
                name = actor.name,
                role = actor.character,
                imageUrl = actor.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" },
                voiceActors = emptyList()
            )
        } ?: emptyList()

        val staff = dto.credits?.crew?.take(20)?.map { crew ->
            Staff(
                id = crew.id.toString(),
                name = crew.name,
                role = crew.job,
                imageUrl = crew.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
            )
        } ?: emptyList()

        val themes = AnimeThemes(
            openings = dto.networks?.map { it.name } ?: emptyList(),
            endings = emptyList()
        )

        val stats = AnimeStatistics(
            scoreDistribution = emptyMap(),
            statusDistribution = emptyMap(),
            totalMembers = dto.numberOfEpisodes ?: 0,
            totalFavorites = dto.voteCount ?: 0,
        )

        return AnimeMetadata(
            id = "tmdb_${dto.id}",
            title = dto.name,
            titleEnglish = dto.originalName,
            titleJapanese = dto.originCountry?.firstOrNull()?.let { if (it == "JP") dto.name else null },
            synonyms = emptyList(),
            description = dto.overview,
            coverImageUrl = coverImage,
            bannerImageUrl = bannerImage,
            type = mapMediaType(dto.type),
            status = mapStatus(dto.status),
            episodes = dto.numberOfEpisodes,
            durationMinutes = dto.episodeRunTime?.firstOrNull(),
            startDate = dto.firstAirDate?.let { parseDate(it) },
            endDate = dto.lastAirDate?.let { parseDate(it) },
            seasonYear = dto.firstAirDate?.take(4)?.toIntOrNull(),
            season = dto.firstAirDate?.take(7)?.let { parseSeason(it) },
            genres = dto.genres?.map { it.name } ?: emptyList(),
            studios = dto.networks?.map { it.name } ?: emptyList(),
            score = dto.voteAverage?.toDouble(),
            scoredBy = dto.voteCount,
            rank = dto.popularity?.toInt(),
            popularity = dto.popularity?.toInt(),
            favorites = null,
            ageRating = dto.contentRatings?.results?.firstOrNull()?.rating,
            sourceMaterial = null,
            trailerUrl = dto.videos?.results?.firstOrNull { it.site == "YouTube" }?.let { "https://youtube.com/watch?v=${it.key}" },
            externalLinks = externalLinks,
            characters = characters,
            staff = staff,
            relations = emptyList(),
            themes = themes,
            statistics = stats,
            providerId = providerId,
        )
    }

    private fun mapMediaType(type: String?): MediaType = when (type) {
        "Scripted" -> MediaType.TV
        "Documentary" -> MediaType.TV
        "News" -> MediaType.TV
        "Miniseries" -> MediaType.SPECIAL
        "Animation" -> MediaType.TV
        else -> MediaType.TV
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

    private fun seasonStartMonth(season: Season): String = when (season) {
        Season.WINTER -> "01"
        Season.SPRING -> "04"
        Season.SUMMER -> "07"
        Season.FALL -> "10"
    }

    private fun seasonEndMonth(season: Season): String = when (season) {
        Season.WINTER -> "03"
        Season.SPRING -> "06"
        Season.SUMMER -> "09"
        Season.FALL -> "12"
    }
}