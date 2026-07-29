package com.kurostream.app.repository

import com.kurostream.app.model.MediaItem
import com.kurostream.app.model.Episode
import com.kurostream.app.model.PlaybackUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object TvRepositories {
    interface MediaRepository {
        fun getMediaItems(): Flow<List<MediaItem>>
        fun getMediaItem(id: String): Flow<MediaItem?>
        suspend fun search(query: String): List<MediaItem>
        suspend fun getPlaybackUrl(mediaId: String, episodeId: String? = null): Result<PlaybackUrl>
        suspend fun getNextEpisode(mediaId: String, episodeId: String?): Result<Episode>

        val isRefreshing: StateFlow<Boolean>
        val refreshError: StateFlow<String?>
    }

    interface FavoritesRepository {
        fun getFavorites(): Flow<List<MediaItem>>
        suspend fun addFavorite(item: MediaItem)
        suspend fun removeFavorite(itemId: String)
    }

    interface WatchProgressRepository {
        fun getWatchProgress(): Flow<Map<String, Long>>
        suspend fun updateProgress(itemId: String, progress: Long)
        suspend fun saveProgress(mediaId: String, episodeId: String?, positionMs: Long, durationMs: Long)
    }
}
