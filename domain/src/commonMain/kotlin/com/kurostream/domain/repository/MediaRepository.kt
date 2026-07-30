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

package com.kurostream.domain.repository

import com.kurostream.domain.entity.MediaItem
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
