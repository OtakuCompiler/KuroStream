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

package com.kurostream.data.repository

import com.kurostream.cache.CacheNamespaceManager
import com.kurostream.domain.result.Result
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.entity.*
import com.kurostream.data.remote.api.*
import com.kurostream.data.remote.dto.anilist.*
import com.kurostream.data.remote.dto.mal.*
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.metadata.MediaType
import com.kurostream.domain.metadata.AiringStatus
import com.kurostream.domain.model.*
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val anilistApi: AniListApi,
    private val malApi: MalApi,
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val cacheManager: CacheNamespaceManager
) : MediaRepository {

    companion object {
        private const val SEARCH_CACHE_TTL = 60 * 60 * 1000L
        private const val METADATA_CACHE_TTL = 24 * 60 * 60 * 1000L
    }

    override fun observeMediaByCategory(category: MediaCategory): Flow<List<MediaItem>> {
        return mediaItemDao.observeByCategory(category.name).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun getMediaById(id: String): MediaItem? = mediaItemDao.getById(id)?.toDomain()
    override suspend fun searchLocal(query: String): List<MediaItem> = mediaItemDao.search(query).map { it.toDomain() }
    override suspend fun saveMediaItem(item: MediaItem) { mediaItemDao.insert(item.toEntity()) }
    override suspend fun saveMediaItems(items: List<MediaItem>) { mediaItemDao.insertAll(items.map { it.toEntity() }) }
    override suspend fun deleteMediaItem(id: String) { mediaItemDao.deleteById(id) }

    override suspend fun searchRemote(query: String, source: String?): List<MediaItem> {
        val cacheKey = "search_${source}_${query}"
        cacheManager.searchResults.get<List<MediaItem>>(cacheKey)?.let { return it }
        val results = when (source) {
            "anilist" -> searchAniList(query)
            "mal" -> searchMal(query)
            else -> searchAllSources(query)
        }
        cacheManager.searchResults.put(cacheKey, results, SEARCH_CACHE_TTL)
        return results
    }

    private suspend fun searchAniList(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val request = AniListSearchRequest(variables = mapOf("search" to query, "page" to 1, "perPage" to 20))
            val response = anilistApi.searchAnime(request)
            if (response.isSuccessful) {
                response.body()?.data?.Page?.media?.mapNotNull { it.toDomain() } ?: emptyList()
            } else {
                Timber.w("AniList search failed: HTTP ${response.code()} — ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "AniList search exception for query: $query")
            emptyList()
        }
    }

    private suspend fun searchMal(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = malApi.searchAnime(query = query, limit = 20)
            if (response.isSuccessful) {
                response.body()?.data?.mapNotNull { it.node.toDomain() } ?: emptyList()
            } else {
                Timber.w("MAL search failed: HTTP ${response.code()} — ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "MAL search exception for query: $query")
            emptyList()
        }
    }

    private suspend fun searchAllSources(query: String): List<MediaItem> {
        return (searchAniList(query) + searchMal(query)).distinctBy { it.id }
    }

    override suspend fun getRemoteDetails(mediaId: String, source: String): MediaItem? {
        val cacheKey = "details_${source}_${mediaId}"
        cacheManager.metadata.get<MediaItem>(cacheKey)?.let { return it }
        return when (source) {
            "anilist" -> getAniListDetails(mediaId.toIntOrNull() ?: return null)
            "mal" -> getMalDetails(mediaId.toIntOrNull() ?: return null)
            else -> {
                Timber.w("Unknown source: $source, returning null")
                null
            }
        }?.also { cacheManager.metadata.put(cacheKey, it, METADATA_CACHE_TTL) }
    }

    private suspend fun getAniListDetails(id: Int): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val request = AniListAnimeDetailsRequest(variables = mapOf("id" to id))
            val response = anilistApi.getAnimeDetails(request)
            if (response.isSuccessful) response.body()?.data?.Media?.toDomain()
            else null
        } catch (e: Exception) { null }
    }

    private suspend fun getMalDetails(id: Int): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val response = malApi.getAnimeDetails(id = id.toString())
            if (response.isSuccessful) {
                val anime = response.body() ?: return@withContext null
                MediaItem(
                    id = "mal_${anime.id}",
                    title = anime.title,
                    description = anime.synopsis ?: "",
                    posterUrl = anime.mainPicture?.large ?: anime.mainPicture?.medium ?: "",
                    backdropUrl = "",
                    genre = anime.genres?.map { it.name } ?: emptyList(),
                    rating = (anime.mean ?: 0.0).toFloat(),
                    year = anime.startDate?.let { it.split("-").firstOrNull()?.toIntOrNull() } ?: 0,
                    duration = anime.averageEpisodeDuration?.let { it / 60 } ?: 0,
                    source = "mal",
                )
            } else null
        } catch (e: Exception) { null }
    }

    override suspend fun getTrending(source: String?): List<MediaItem> {
        val cacheKey = "trending_${source ?: "all"}"
        cacheManager.searchResults.get<List<MediaItem>>(cacheKey)?.let { return it }
        val results = when (source) {
            "anilist" -> getAniListTrending()
            "mal" -> getMalRanking()
            else -> getAniListTrending() + getMalRanking()
        }
        cacheManager.searchResults.put(cacheKey, results, SEARCH_CACHE_TTL)
        return results
    }

    private suspend fun getAniListTrending(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = anilistApi.getTrendingAnime(AniListTrendingRequest())
            if (response.isSuccessful) response.body()?.data?.Page?.media?.mapNotNull { it.toDomain() } ?: emptyList()
            else {
                Timber.w("AniList trending failed: HTTP ${response.code()} — ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "AniList trending exception")
            emptyList()
        }
    }

    private suspend fun getMalRanking(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = malApi.getTopAnime()
            if (response.isSuccessful) {
                response.body()?.data?.mapNotNull { rankingNode ->
                    val anime = rankingNode.node
                    val titleStr = anime.title
                    val coverUrl = anime.mainPicture?.large ?: anime.mainPicture?.medium
                    val seasonYearVal = anime.startDate?.let { it.split("-").firstOrNull()?.toIntOrNull() }
                    MediaItem(
                        id = "mal_${anime.id}",
                        title = titleStr,
                        description = anime.synopsis ?: "",
                        posterUrl = coverUrl ?: "",
                        backdropUrl = "",
                        genre = emptyList(),
                        rating = (anime.mean ?: 0.0).toFloat(),
                        year = seasonYearVal ?: 0,
                        duration = 0,
                        source = "mal",
                    )
                } ?: emptyList()
            } else {
                Timber.w("MAL ranking failed: HTTP ${response.code()} — ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "MAL ranking exception")
            emptyList()
        }
    }

    override fun observeWatchHistory(profileId: String): Flow<List<WatchHistory>> {
        return watchHistoryDao.observeByProfile(profileId).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun getWatchHistory(mediaItemId: String, profileId: String): WatchHistory? {
        return watchHistoryDao.getByMediaAndProfile(mediaItemId, profileId)?.toDomain()
    }

    override suspend fun saveWatchHistory(history: WatchHistory) { watchHistoryDao.insert(history.toEntity()) }
    override suspend fun deleteWatchHistory(mediaItemId: String, profileId: String) {
        watchHistoryDao.deleteByMediaAndProfile(mediaItemId, profileId)
    }

    override fun observeFavorites(profileId: String): Flow<List<Favorite>> {
        return favoriteDao.observeByProfile(profileId).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun isFavorite(mediaItemId: String, profileId: String): Boolean {
        return favoriteDao.isFavorite(mediaItemId, profileId)
    }

    override suspend fun addFavorite(favorite: Favorite) { favoriteDao.insert(favorite.toEntity()) }
    override suspend fun removeFavorite(mediaItemId: String, profileId: String) {
        favoriteDao.deleteByMediaAndProfile(mediaItemId, profileId)
    }


    override suspend fun searchSubtitles(query: String, languages: List<String>, episodeInfo: EpisodeInfo?): List<SubtitleResult> = withContext(Dispatchers.IO) {
        try {
            val response = openSubtitlesApi.searchSubtitles(
                query = query,
                languages = languages.joinToString(","),
                seasonNumber = episodeInfo?.seasonNumber,
                episodeNumber = episodeInfo?.episodeNumber
            )
            if (response.isSuccessful) {
                response.body()?.data?.mapNotNull { item ->
                    item.attributes?.let { attr ->
                        SubtitleResult(
                            id = item.id ?: return@mapNotNull null,
                            language = attr.language ?: "unknown",
                            fileName = attr.files?.firstOrNull()?.fileName ?: "unknown",
                            downloadCount = attr.downloadCount ?: 0,
                            rating = attr.ratings?.toFloat() ?: 0f,
                            fps = attr.fps ?: 0.0,
                            hearingImpaired = attr.hearing_impaired ?: false,
                            downloadUrl = attr.url
                        )
                    }
                } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun MediaItemEntity.toDomain(): MediaItem {
        return MediaItem(
            id = id,
            title = title,
            description = description ?: "",
            posterUrl = posterUrl ?: "",
            backdropUrl = bannerUrl ?: "",
            genre = emptyList(),
            rating = rating?.toFloat() ?: 0f,
            year = releaseDate?.let { (it / 10000).toInt() } ?: 0,
            duration = duration?.let { (it / 60000).toInt() } ?: 0,
            source = sourceType
        )
    }

    private fun MediaItem.toEntity(): MediaItemEntity {
        return MediaItemEntity(
            id = id,
            sourceId = id,
            sourceType = source,
            title = title,
            description = description,
            posterUrl = posterUrl,
            bannerUrl = backdropUrl,
            category = MediaType.TV.name,
            releaseDate = year.takeIf { it > 0 }?.let { it * 10000L },
            rating = rating.toDouble(),
            duration = duration.takeIf { it > 0 }?.let { it.toLong() * 60000 },
            streamUrl = null,
            metadataJson = null,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun AniListMedia.toDomain(): MediaItem {
        val titleStr = title?.english ?: title?.romaji ?: title?.native ?: "Unknown"
        val coverUrl = coverImage?.large ?: coverImage?.medium
        val scoreVal = averageScore?.let { it / 10.0 }
        val durationMin = duration?.let { it / 60 }

        return MediaItem(
            id = "anilist_$id",
            title = titleStr,
            description = description ?: "",
            posterUrl = coverUrl ?: "",
            backdropUrl = bannerImage ?: "",
            genre = genres ?: emptyList(),
            rating = scoreVal?.toFloat() ?: 0f,
            year = seasonYear ?: 0,
            duration = durationMin ?: 0,
            source = "anilist"
        )
    }

    private fun SearchNode.toDomain(): MediaItem {
        val titleStr = title
        val coverUrl = mainPicture?.large ?: mainPicture?.medium
        val scoreVal = mean
        val seasonYearVal = startDate?.let { it.split("-").firstOrNull()?.toIntOrNull() }

        return MediaItem(
            id = "mal_$id",
            title = titleStr,
            description = synopsis ?: "",
            posterUrl = coverUrl ?: "",
            backdropUrl = "",
            genre = emptyList(),
            rating = scoreVal?.toFloat() ?: 0f,
            year = seasonYearVal ?: 0,
            duration = 0,
            source = "mal"
        )
    }

    private fun safeParseAiringStatus(status: String?): AiringStatus {
        return when (status?.uppercase()) {
            "FINISHED", "FINISHED_AIRING", "ENDED" -> AiringStatus.FINISHED
            "AIRING", "CURRENTLY_AIRING", "RELEASING" -> AiringStatus.AIRING
            "NOT_YET_AIRED", "NOT_YET_RELEASED", "UPCOMING" -> AiringStatus.NOT_YET_AIRED
            "CANCELLED", "CANCELED" -> AiringStatus.CANCELLED
            else -> AiringStatus.UNKNOWN
        }
    }

    private fun parseMalDate(dateStr: String): Long? = try {
        val p = dateStr.split("-")
        if (p.size >= 3) java.util.Calendar.getInstance().apply { set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) }.timeInMillis else null
    } catch (e: Exception) { null }

    private fun WatchHistoryEntity.toDomain() = WatchHistory(id, mediaItemId, profileId, position, duration, watchedAt, completionPercent, episodeNumber, seasonNumber)
    private fun WatchHistory.toEntity() = WatchHistoryEntity(id, mediaItemId, profileId, position, duration, watchedAt, completionPercent, episodeNumber, seasonNumber)
    private fun FavoriteEntity.toDomain() = Favorite(id, mediaItemId ?: "", profileId ?: "", addedAt, category)
    private fun Favorite.toEntity() = FavoriteEntity(id, mediaItemId, profileId, addedAt, category)

    override suspend fun getMediaItems(): List<String> = mediaItemDao.search("").map { it.id }
    override suspend fun getMediaItem(id: String): String? = mediaItemDao.getById(id)?.id
    override fun observeAllMediaItems(): Flow<List<MediaItem>> {
        return mediaItemDao.observeAll().map { list -> list.map { it.toDomain() } }
    }
    override suspend fun search(query: String): List<String> = mediaItemDao.search(query).map { it.id }
    override suspend fun getPlaybackUrl(mediaId: String, episodeId: String?): com.kurostream.domain.result.Result<com.kurostream.domain.model.PlaybackUrl> = com.kurostream.domain.result.Result.Error(Exception("Playback URL resolution requires stream source configuration"))
    override suspend fun getNextEpisode(mediaId: String, episodeId: String?): com.kurostream.domain.result.Result<com.kurostream.domain.model.EpisodeInfo> = com.kurostream.domain.result.Result.Error(Exception("Playback URL resolution requires stream source configuration"))
}