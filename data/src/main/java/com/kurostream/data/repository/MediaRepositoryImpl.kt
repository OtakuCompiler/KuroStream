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
import com.kurostream.core.common.result.Result
import com.kurostream.core.common.result.Resource
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.entity.*
import com.kurostream.data.remote.api.*
import com.kurostream.data.remote.dto.anilist.*
import com.kurostream.domain.model.*
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val downloadItemDao: DownloadItemDao,
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

    override suspend fun searchRemote(query: String, source: String?): Result<List<MediaItem>> {
        val cacheKey = "search_${source}_${query}"
        cacheManager.searchResults.get<List<MediaItem>>(cacheKey)?.let { return Result.success(it) }
        val results = when (source) {
            "anilist" -> searchAniList(query)
            "mal" -> searchMal(query)
            else -> searchAllSources(query)
        }
        cacheManager.searchResults.put(cacheKey, results, SEARCH_CACHE_TTL)
        return Result.success(results)
    }

    private suspend fun searchAniList(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val request = AniListSearchRequest(variables = mapOf("search" to query, "page" to 1, "perPage" to 20))
            val response = anilistApi.searchAnime(request)
            if (response.isSuccessful) response.body()?.data?.Page?.media?.map { it.toDomain() } ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun searchMal(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = malApi.searchAnime(query = query, limit = 20)
            if (response.isSuccessful) response.body()?.data?.map { it.node?.toDomain() }?.filterNotNull() ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun searchAllSources(query: String): List<MediaItem> {
        return (searchAniList(query) + searchMal(query)).distinctBy { it.id }
    }

    override suspend fun getRemoteDetails(mediaId: String, source: String): Result<MediaItem?> {
        val cacheKey = "details_${source}_${mediaId}"
        cacheManager.metadata.get<MediaItem>(cacheKey)?.let { return Result.success(it) }
        val result = when (source) {
            "anilist" -> getAniListDetails(mediaId.toIntOrNull() ?: return Result.error(RuntimeException("Invalid media ID")))
            "mal" -> getMalDetails(mediaId.toIntOrNull() ?: return Result.error(RuntimeException("Invalid media ID")))
            else -> Result.error(RuntimeException("Unknown source: $source"))
        }
        return result.map { mediaItem ->
            cacheManager.metadata.put(cacheKey, mediaItem, METADATA_CACHE_TTL)
            mediaItem
        }
    }

    private suspend fun getAniListDetails(id: Int): Result<MediaItem?> = withContext(Dispatchers.IO) {
        try {
            val request = AniListAnimeDetailsRequest(variables = mapOf("id" to id))
            val response = anilistApi.getAnimeDetails(request)
            if (response.isSuccessful) Result.success(response.body()?.data?.Media?.toDomain())
            else Result.error(RuntimeException("AniList API error: ${response.code()}"))
        } catch (e: Exception) { Result.error(e) }
    }

    private suspend fun getMalDetails(id: Int): Result<MediaItem?> = withContext(Dispatchers.IO) {
        try {
            val response = malApi.getAnimeDetails(animeId = id)
            if (response.isSuccessful) Result.success(response.body()?.data?.toDomain())
            else Result.error(RuntimeException("MAL API error: ${response.code()}"))
        } catch (e: Exception) { Result.error(e) }
    }

    override suspend fun getTrending(source: String?): Result<List<MediaItem>> {
        val cacheKey = "trending_${source ?: "all"}"
        cacheManager.searchResults.get<List<MediaItem>>(cacheKey)?.let { return Result.success(it) }
        val results = when (source) {
            "anilist" -> getAniListTrending()
            "mal" -> getMalRanking()
            else -> getAniListTrending() + getMalRanking()
        }
        cacheManager.searchResults.put(cacheKey, results, SEARCH_CACHE_TTL)
        return Result.success(results)
    }

    private suspend fun getAniListTrending(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = anilistApi.getTrendingAnime(AniListTrendingRequest())
            if (response.isSuccessful) response.body()?.data?.Page?.media?.map { it.toDomain() } ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun getMalRanking(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = malApi.getRanking(limit = 20)
            if (response.isSuccessful) response.body()?.data?.map { it.node?.toDomain() }?.filterNotNull() ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
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

    override fun observeDownloads(profileId: String): Flow<List<DownloadItem>> {
        return downloadItemDao.observeByProfile(profileId).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun getDownload(mediaItemId: String, profileId: String): DownloadItem? {
        return downloadItemDao.getByMediaAndProfile(mediaItemId, profileId)?.toDomain()
    }

    override suspend fun saveDownload(download: DownloadItem) { downloadItemDao.insert(download.toEntity()) }

    override suspend fun updateDownloadProgress(id: String, progress: Float, status: DownloadStatus) {
        downloadItemDao.updateProgress(id, status.name, progress)
    }

    override suspend fun deleteDownload(id: String) {
        // Simplified - needs proper ID-based lookup
    }

    override suspend fun searchSubtitles(query: String, languages: List<String>, episodeInfo: EpisodeInfo?): Result<List<SubtitleResult>> = withContext(Dispatchers.IO) {
        try {
            val response = openSubtitlesApi.searchSubtitles(
                query = query,
                languages = languages.joinToString(","),
                seasonNumber = episodeInfo?.seasonNumber,
                episodeNumber = episodeInfo?.episodeNumber
            )
            if (response.isSuccessful) {
                val results = response.body()?.data?.mapNotNull { item ->
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
                Result.success(results)
            } else Result.error(RuntimeException("OpenSubtitles API error: ${response.code()}"))
        } catch (e: Exception) { Result.error(e) }
    }

    private fun MediaItemEntity.toDomain() = MediaItem(id, sourceId, sourceType, title, description, posterUrl, bannerUrl, MediaCategory.valueOf(category), releaseDate, rating, duration, streamUrl, metadataJson)
    private fun MediaItem.toEntity() = MediaItemEntity(id, sourceId, sourceType, title, description, posterUrl, bannerUrl, category.name, releaseDate, rating, duration, streamUrl, metadataJson)
    private fun AniListMedia.toDomain() = MediaItem("anilist_$id", id.toString(), "anilist", title?.english ?: title?.romaji ?: "Unknown", description, coverImage?.large ?: coverImage?.medium, bannerImage, MediaCategory.ANIME, seasonYear?.let { it * 10000L }, averageScore?.let { it / 10.0 }, duration?.let { it * 60L * 1000L }, null, null)
    private fun MalAnime.toDomain() = MediaItem("mal_$id", id.toString(), "mal", title ?: alternativeTitles?.en ?: "Unknown", synopsis, mainPicture?.large ?: mainPicture?.medium, null, MediaCategory.ANIME, parseMalDate(startDate), mean, averageEpisodeDuration?.let { it * 1000L }, null, null)
    private fun parseMalDate(dateStr: String): Long? = try { val p = dateStr.split("-"); if (p.size >= 3) java.util.Calendar.getInstance().apply { set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) }.timeInMillis else null } catch (e: Exception) { null }
    private fun WatchHistoryEntity.toDomain() = WatchHistory(id, mediaItemId, profileId, position, duration, watchedAt, completionPercent, episodeNumber, seasonNumber)
    private fun WatchHistory.toEntity() = WatchHistoryEntity(id, mediaItemId, profileId, position, duration, watchedAt, completionPercent, episodeNumber, seasonNumber)
    private fun FavoriteEntity.toDomain() = Favorite(id, mediaItemId, profileId, addedAt, category)
    private fun Favorite.toEntity() = FavoriteEntity(id, mediaItemId, profileId, addedAt, category)
    private fun DownloadItemEntity.toDomain() = DownloadItem(id, mediaItemId, profileId, localPath, DownloadStatus.valueOf(status), progress, totalBytes, downloadedBytes, startedAt, completedAt, errorMessage)
    private fun DownloadItem.toEntity() = DownloadItemEntity(id, mediaItemId, profileId, localPath, status.name, progress, totalBytes, downloadedBytes, startedAt, completedAt, errorMessage)
}
