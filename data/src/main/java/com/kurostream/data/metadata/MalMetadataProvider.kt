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

import com.kurostream.data.remote.api.MalApi
import com.kurostream.data.remote.dto.mal.*
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MalMetadataProvider @Inject constructor(
    private val api: MalApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "mal"
    override val providerName = "MyAnimeList"
    override val priority = 3
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("mal_anime_$id", cacheTtlMs) {
            try {
                val response = api.getAnimeDetails(id)
                val anime = response.body()
                if (anime != null) {
                    MetadataResult.Success(mapToDomain(anime))
                } else {
                    MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "MAL getAnime failed")
                MetadataResult.Error(e.message ?: "MAL error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("mal_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query, limit, (page - 1) * limit)
                val list = response.body()?.data?.mapNotNull { result ->
                    mapSearchNodeToDomain(result.node)
                } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "MAL searchAnime failed")
                MetadataResult.Error(e.message ?: "MAL error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return MetadataResult.NotFound
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("mal_seasonal_${year}_${season.name}", cacheTtlMs) {
            try {
                val response = api.getSeasonalAnime(year, season.name.lowercase(), 50, 0)
                val list = response.body()?.data?.mapNotNull { seasonalNode ->
                    mapSearchNodeToDomain(seasonalNode.node)
                } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "MAL getSeasonalAnime failed")
                MetadataResult.Error(e.message ?: "MAL error", throwable = e)
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("mal_trending_$limit", 6 * 60 * 60 * 1000L) {
            try {
                val response = api.getTopAnime("all", limit, 0)
                val list = response.body()?.data?.mapNotNull { topNode ->
                    mapSearchNodeToDomain(topNode.node)
                } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "MAL getTrendingAnime failed")
                MetadataResult.Error(e.message ?: "MAL error", throwable = e)
            }
        }
    }

    private fun mapSearchNodeToDomain(node: SearchNode): AnimeMetadata = AnimeMetadata(
        id = "mal_${node.id}",
        title = node.title,
        titleEnglish = node.alternativeTitles?.en,
        titleJapanese = node.alternativeTitles?.ja,
        synonyms = node.alternativeTitles?.synonyms ?: emptyList(),
        description = node.synopsis?.replace("<br>", "\n"),
        coverImageUrl = node.mainPicture?.large ?: node.mainPicture?.medium,
        bannerImageUrl = null,
        type = mapMediaType(node.mediaType),
        status = mapStatus(node.status),
        episodes = node.numEpisodes,
        durationMinutes = null,
        startDate = node.startDate?.let { parseDate(it) },
        endDate = node.endDate?.let { parseDate(it) },
        seasonYear = node.startDate?.take(4)?.toIntOrNull(),
        season = node.startDate?.let { parseSeason(it) },
        genres = emptyList(),
        studios = emptyList(),
        score = node.mean,
        scoredBy = node.numScoringUsers,
        rank = node.rank,
        popularity = node.popularity,
        favorites = node.numFavorites,
        ageRating = null,
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

    private fun mapToDomain(anime: Anime): AnimeMetadata = AnimeMetadata(
        id = "mal_${anime.id}",
        title = anime.title,
        titleEnglish = anime.alternativeTitles?.en,
        titleJapanese = anime.alternativeTitles?.ja,
        synonyms = anime.alternativeTitles?.synonyms ?: emptyList(),
        description = anime.synopsis?.replace("<br>", "\n"),
        coverImageUrl = anime.mainPicture?.large ?: anime.mainPicture?.medium,
        bannerImageUrl = anime.pictures?.firstOrNull()?.large,
        type = mapMediaType(anime.mediaType),
        status = mapStatus(anime.status),
        episodes = anime.numEpisodes,
        durationMinutes = anime.averageEpisodeDuration?.div(60),
        startDate = anime.startDate?.let { parseDate(it) },
        endDate = anime.endDate?.let { parseDate(it) },
        seasonYear = anime.startDate?.take(4)?.toIntOrNull(),
        season = anime.startDate?.let { parseSeason(it) },
        genres = anime.genres?.map { it.name } ?: emptyList(),
        studios = anime.studios?.map { it.name } ?: emptyList(),
        score = anime.mean,
        scoredBy = anime.numScoringUsers,
        rank = anime.rank,
        popularity = anime.popularity,
        favorites = anime.numFavorites,
        ageRating = anime.rating,
        sourceMaterial = anime.source,
        trailerUrl = null,
        externalLinks = emptyList(),
        characters = emptyList(),
        staff = emptyList(),
        relations = emptyList(),
        themes = AnimeThemes(),
        statistics = null,
        providerId = providerId,
    )

    private fun mapMediaType(type: String?): MediaType = when (type) {
        "TV" -> MediaType.TV
        "movie" -> MediaType.MOVIE
        "OVA" -> MediaType.OVA
        "ONA" -> MediaType.ONA
        "special" -> MediaType.SPECIAL
        "music" -> MediaType.MUSIC
        else -> MediaType.TV
    }

    private fun mapStatus(status: String?): AiringStatus = when (status) {
        "currently_airing" -> AiringStatus.AIRING
        "finished_airing" -> AiringStatus.FINISHED
        "not_yet_aired" -> AiringStatus.NOT_YET_AIRED
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
