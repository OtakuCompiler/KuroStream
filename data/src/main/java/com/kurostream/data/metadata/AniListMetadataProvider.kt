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

import com.kurostream.data.remote.dto.anilist.AniListDtos
import com.kurostream.data.remote.api.AniListApi
import com.kurostream.domain.metadata.*
import com.kurostream.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

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

    override suspend fun getAnime(id: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "anilist_anime_$id"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getAnime(id)
                if (response.data?.media == null) {
                    return@withContext MetadataResult.NotFound
                }
                mapToDomain(response.data!!.media!!)
            } catch (e: Exception) {
                Timber.e(e, "AniList getAnime failed")
                throw e
            }
        }
    }

    override suspend fun searchAnime(query: String, page: Int, limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "anilist_search_${query}_$page"
        cache.getOrFetch(cacheKey, 60 * 60 * 1000L) { // 1 hour cache for searches
            try {
                val response = api.searchAnime(query, page, limit)
                response.data?.page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "AniList searchAnime failed")
                throw e
            }
        }
    }

    override suspend fun getAnimeByExternalId(type: ExternalIdType, value: String): MetadataResult<AnimeMetadata> = withContext(Dispatchers.IO) {
        val cacheKey = "anilist_external_${type.name}_$value"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val query = when (type) {
                    ExternalIdType.MAL_ID -> "idMal:$value"
                    ExternalIdType.ANILIST_ID -> "id:$value"
                    ExternalIdType.TMDB_ID -> "idTmdb:$value"
                    else -> return@withContext MetadataResult.NotFound
                }
                val response = api.getAnimeByExternalId(query)
                response.data?.media?.let { mapToDomain(it) } ?: MetadataResult.NotFound
            } catch (e: Exception) {
                Timber.e(e, "AniList getAnimeByExternalId failed")
                throw e
            }
        }
    }

    override suspend fun getSeasonalAnime(year: Int, season: Season): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "anilist_seasonal_${year}_${season.name}"
        cache.getOrFetch(cacheKey, cacheTtlMs) {
            try {
                val response = api.getSeasonalAnime(year, season.name.lowercase())
                response.data?.page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "AniList getSeasonalAnime failed")
                throw e
            }
        }
    }

    override suspend fun getTrendingAnime(limit: Int): MetadataResult<List<AnimeMetadata>> = withContext(Dispatchers.IO) {
        val cacheKey = "anilist_trending_$limit"
        cache.getOrFetch(cacheKey, 6 * 60 * 60 * 1000L) { // 6 hours
            try {
                val response = api.getTrendingAnime(limit)
                response.data?.page?.media?.mapNotNull { mapToDomain(it) } ?: emptyList()
            } catch (e: Exception) {
                Timber.e(e, "AniList getTrendingAnime failed")
                throw e
            }
        }
    }

    private fun mapToDomain(dto: AniListDtos.Media): AnimeMetadata {
        val coverImage = dto.coverImage?.extraLarge ?: dto.coverImage?.large ?: dto.coverImage?.medium
        val bannerImage = dto.bannerImage
        
        val externalLinks = dto.externalLinks?.map { link ->
            ExternalLink(link.site, link.url)
        } ?: emptyList()

        val characters = dto.characters?.edges?.mapNotNull { edge ->
            edge.node?.let { char ->
                val voiceActors = char.voiceActors?.mapNotNull { va ->
                    va.person?.let { person ->
                        VoiceActor(
                            id = person.id.toString(),
                            name = person.name.full,
                            language = va.languageV2 ?: "Japanese",
                            imageUrl = person.image?.large
                        )
                    }
                } ?: emptyList()
                
                Character(
                    id = char.id.toString(),
                    name = char.name.full,
                    role = char.role?.name ?: "Main",
                    imageUrl = char.image?.large,
                    voiceActors = voiceActors
                )
            }
        } ?: emptyList()

        val staff = dto.staff?.edges?.mapNotNull { edge ->
            edge.node?.let { s ->
                Staff(
                    id = s.id.toString(),
                    name = s.name.full,
                    role = s.primaryOccupations?.firstOrNull() ?: "Staff",
                    imageUrl = s.image?.large
                )
            }
        } ?: emptyList()

        val relations = dto.relations?.edges?.mapNotNull { edge ->
            edge.node?.let { rel ->
                AnimeRelation(
                    relationType = rel.relationType?.name ?: "Related",
                    relatedAnimeId = rel.id.toString(),
                    relatedTitle = rel.title?.romaji ?: rel.title?.english ?: "Unknown"
                )
            }
        } ?: emptyList()

        val themes = AnimeThemes(
            openings = dto.streamingEpisodes?.mapNotNull { ep ->
                ep.title?.let { ThemeSong(title = it, artist = "", videoUrl = ep.url) }
            } ?: emptyList(),
            endings = emptyList()
        )

        val stats = dto.statistics?.let { s ->
            AnimeStatistics(
                watching = s.statusDistribution?.getValue("CURRENT") ?: 0,
                completed = s.statusDistribution?.getValue("COMPLETED") ?: 0,
                onHold = s.statusDistribution?.getValue("PAUSED") ?: 0,
                dropped = s.statusDistribution?.getValue("DROPPED") ?: 0,
                planToWatch = s.statusDistribution?.getValue("PLANNING") ?: 0,
                scoreDistribution = s.scoreDistribution?.associate { it.score to it.amount } ?: emptyMap(),
                statusDistribution = s.statusDistribution ?: emptyMap(),
            )
        }

        return AnimeMetadata(
            id = "anilist_${dto.id}",
            title = dto.title.romaji,
            titleEnglish = dto.title.english,
            titleJapanese = dto.title.native,
            synonyms = dto.synonyms ?: emptyList(),
            description = dto.description?.replace("<br>", "\n").replace("<i>", "").replace("</i>", ""),
            coverImageUrl = coverImage,
            bannerImageUrl = bannerImage,
            type = mapMediaType(dto.format),
            status = mapStatus(dto.status),
            episodes = dto.episodes,
            durationMinutes = dto.duration,
            startDate = dto.startDate?.toEpochMillis(),
            endDate = dto.endDate?.toEpochMillis(),
            seasonYear = dto.seasonYear,
            season = dto.season?.let { Season.valueOf(it.uppercase()) },
            genres = dto.genres ?: emptyList(),
            studios = dto.studios?.nodes?.map { it.name } ?: emptyList(),
            score = dto.averageScore?.div(10.0),
            scoredBy = dto.meanScore,
            rank = dto.popularityRank,
            popularity = dto.popularity,
            favorites = dto.favourites,
            ageRating = dto.ageRating,
            sourceMaterial = dto.source?.name,
            trailerUrl = dto.trailer?.let { "https://youtube.com/watch?v=${it.id}" },
            externalLinks = externalLinks,
            characters = characters,
            staff = staff,
            relations = relations,
            themes = themes,
            statistics = stats,
            providerId = providerId,
        )
    }

    private fun mapMediaType(format: AniListDtos.MediaFormat?): MediaType = when (format) {
        AniListDtos.MediaFormat.TV -> MediaType.TV
        AniListDtos.MediaFormat.MOVIE -> MediaType.MOVIE
        AniListDtos.MediaFormat.OVA -> MediaType.OVA
        AniListDtos.MediaFormat.ONA -> MediaType.ONA
        AniListDtos.MediaFormat.SPECIAL -> MediaType.SPECIAL
        AniListDtos.MediaFormat.MUSIC -> MediaType.MUSIC
        else -> MediaType.TV
    }

    private fun mapStatus(status: AniListDtos.MediaStatus?): AiringStatus = when (status) {
        AniListDtos.MediaStatus.RELEASING -> AiringStatus.AIRING
        AniListDtos.MediaStatus.FINISHED -> AiringStatus.FINISHED
        AniListDtos.MediaStatus.NOT_YET_RELEASED -> AiringStatus.NOT_YET_AIRED
        AniListDtos.MediaStatus.CANCELLED -> AiringStatus.CANCELLED
        AniListDtos.MediaStatus.HIATUS -> AiringStatus.AIRING
        else -> AiringStatus.UNKNOWN
    }
}