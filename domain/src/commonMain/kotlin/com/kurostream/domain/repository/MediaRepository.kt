package com.kurostream.domain.repository

import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.model.DownloadItem
import com.kurostream.domain.model.DownloadStatus
import com.kurostream.domain.model.EpisodeInfo
import com.kurostream.domain.model.Favorite
import com.kurostream.domain.model.MediaCategory
import com.kurostream.domain.model.SubtitleResult
import com.kurostream.domain.model.PlaybackUrl
import com.kurostream.domain.model.WatchHistory
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getMediaItems(): List<String>
    suspend fun getMediaItem(id: String): String?
    /** Observe all media items as a flow for home screen display */
    fun observeAllMediaItems(): Flow<List<MediaItem>>
    suspend fun search(query: String): List<String>

    fun observeMediaByCategory(category: MediaCategory): Flow<List<MediaItem>>
    suspend fun getMediaById(id: String): MediaItem?
    suspend fun searchLocal(query: String): List<MediaItem>
    suspend fun searchRemote(query: String, source: String? = null): List<MediaItem>
    suspend fun getTrending(source: String? = null): List<MediaItem>
    suspend fun getRemoteDetails(mediaId: String, source: String): MediaItem?
    suspend fun saveMediaItem(item: MediaItem)
    suspend fun saveMediaItems(items: List<MediaItem>)
    suspend fun deleteMediaItem(id: String)

    fun observeDownloads(profileId: String): Flow<List<DownloadItem>>
    suspend fun getDownload(mediaItemId: String, profileId: String): DownloadItem?
    suspend fun saveDownload(download: DownloadItem)
    suspend fun updateDownloadProgress(id: String, progress: Float, status: DownloadStatus)
    suspend fun deleteDownload(id: String)

    fun observeFavorites(profileId: String): Flow<List<Favorite>>
    suspend fun isFavorite(mediaItemId: String, profileId: String): Boolean
    suspend fun addFavorite(favorite: Favorite)
    suspend fun removeFavorite(mediaItemId: String, profileId: String)

    fun observeWatchHistory(profileId: String): Flow<List<WatchHistory>>
    suspend fun getWatchHistory(mediaItemId: String, profileId: String): WatchHistory?
    suspend fun saveWatchHistory(history: WatchHistory)
    suspend fun deleteWatchHistory(mediaItemId: String, profileId: String)

    suspend fun searchSubtitles(query: String, languages: List<String>, episodeInfo: EpisodeInfo? = null): List<SubtitleResult>
    suspend fun getPlaybackUrl(mediaId: String, episodeId: String? = null): com.kurostream.domain.result.Result<PlaybackUrl>
    suspend fun getNextEpisode(mediaId: String, episodeId: String?): com.kurostream.domain.result.Result<EpisodeInfo>
}
