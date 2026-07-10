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

import com.kurostream.data.remote.dto.mal.MalDtos
import com.kurostream.data.remote.api.MalApi
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

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

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "mal_anime_$id"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getAnimeDetails(id)
                response.data?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "MAL getAnime failed")
                throw e
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "mal_search_${query}_$page"
        cache.getOrFetch(cacheKey, 60 * 60 * 1000L) {
            try {
                val response = api.searchAnime(query, limit, (page - 1) * limit)
                response.data?.map { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "MAL searchAnime failed")
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

    private fun mapToDomain(dto: MalDtos.AnimeDetail): AnimeMetadata {
        val node = dto.node ?: dto
        val mainPicture = node.mainPicture
        val coverImage = mainPicture?.large ?: mainPicture?.medium

        return AnimeMetadata(
            id = "mal_${node.id}",
            title = node.title,
            titleEnglish = node.alternativeTitles?.en,
            titleJapanese = node.alternativeTitles?.ja,
            synonyms = node.alternativeTitles?.synonyms ?: emptyList(),
            description = node.synopsis?.replace("<br>", "\n"),
            coverImageUrl = coverImage,
            bannerImageUrl = node.pictures?.firstOrNull()?.large,
            type = mapMediaType(node.mediaType),
            status = mapStatus(node.status),
            episodes = node.numEpisodes,
            durationMinutes = node.duration?.div(60)?.toInt(),
            startDate = node.startDate?.let { parseDate(it) },
            endDate = node.endDate?.let { parseDate(it) },
            seasonYear = node.startDate?.take(4)?.toIntOrNull(),
            season = node.startDate?.take(7)?.let { parseSeason(it) },
            genres = node.genres?.map { it.name } ?: emptyList(),
            studios = node.studios?.map { it.name } ?: emptyList(),
            score = node.mean?.toDouble(),
            scoredBy = node.numScoredUsers,
            rank = node.rank,
            popularity = node.popularity,
            favorites = node.numFavorites,
            ageRating = node.rating,
            sourceMaterial = node.source,
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