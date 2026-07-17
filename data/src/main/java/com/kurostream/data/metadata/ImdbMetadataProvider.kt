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

import com.kurostream.data.remote.api.ImdbApi
import com.kurostream.data.remote.dto.imdb.Title
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ImdbMetadataProvider @Inject constructor(
    private val api: ImdbApi,
    private val cache: CacheRepository,
) : MetadataProvider {

    override val providerId = "imdb"
    override val providerName = "IMDb"
    override val priority = 6
    override val isEnabled = true

    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> {
        return cache.getOrFetch("imdb_anime_$id", cacheTtlMs) {
            try {
                val response = api.getTitle(id)
                val title = response.body()?.data
                if (title != null) {
                    MetadataResult.Success(mapToDomain(title))
                } else {
                    MetadataResult.NotFound
                }
            } catch (e: Exception) {
                Timber.e(e, "IMDb getAnime failed")
                MetadataResult.Error(e.message ?: "IMDb error", throwable = e)
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> {
        return cache.getOrFetch("imdb_search_${query}_$page", 60 * 60 * 1000L) {
            try {
                val response = api.searchTitles(query, limit)
                val list = response.body()?.data?.map { mapToDomain(it) } ?: emptyList()
                MetadataResult.Success(list)
            } catch (e: Exception) {
                Timber.e(e, "IMDb searchAnime failed")
                MetadataResult.Error(e.message ?: "IMDb error", throwable = e)
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> {
        return when (type) {
            ExternalIdType.IMDB_ID -> getAnime(value)
            else -> MetadataResult.NotFound
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> {
        return MetadataResult.Success(emptyList())
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> {
        return MetadataResult.Success(emptyList())
    }

    private fun mapToDomain(dto: Title): AnimeMetadata {
        return AnimeMetadata(
            id = "imdb_${dto.id}",
            title = dto.titleText?.text ?: dto.originalTitleText?.text ?: "",
            titleEnglish = dto.titleText?.text,
            titleJapanese = null,
            synonyms = dto.alternateTitles?.map { it.titleText?.text }?.filterNotNull() ?: emptyList(),
            description = dto.plot?.plotText?.plainText,
            coverImageUrl = dto.primaryImage?.url,
            bannerImageUrl = null,
            type = mapMediaType(dto.titleType),
            status = AiringStatus.UNKNOWN,
            startDate = dto.releaseYear?.let { java.time.LocalDate.of(it, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() },
            endDate = null,
            seasonYear = dto.releaseYear,
            season = null,
            genres = dto.genres?.genres?.map { it.text } ?: emptyList(),
            studios = dto.productionCompany?.edges?.map { it.node?.name }?.filterNotNull() ?: emptyList(),
            score = dto.ratingsSummary?.aggregateRating?.toDouble()?.div(10.0),
            scoredBy = dto.ratingsSummary?.voteCount?.toInt(),
            rank = null,
            popularity = dto.ratingsSummary?.popularityScore?.toInt(),
            favorites = null,
            ageRating = dto.certificates?.edges?.firstOrNull()?.node?.rating,
            sourceMaterial = null,
            durationMinutes = dto.runtime?.seconds?.div(60)?.toInt(),
            episodes = dto.episodes?.episodes?.firstOrNull()?.totalEpisodes,
            trailerUrl = dto.primaryVideos?.edges?.firstOrNull()?.node?.playbackURLs?.firstOrNull()?.url,
            externalLinks = listOf(ExternalLink("imdb", "https://imdb.com/title/${dto.id}")),
            characters = emptyList(),
            staff = emptyList(),
            relations = emptyList(),
            themes = AnimeThemes(),
            statistics = null,
            providerId = providerId,
        )
    }

    private fun mapMediaType(type: String?): MediaType = when (type?.lowercase()) {
        "tvseries", "tvMiniSeries", "tvSeries" -> MediaType.TV
        "movie", "tvMovie" -> MediaType.MOVIE
        "tvSpecial" -> MediaType.SPECIAL
        "tvShort" -> MediaType.ONA
        "videoGame" -> MediaType.UNKNOWN
        else -> MediaType.UNKNOWN
    }
}
