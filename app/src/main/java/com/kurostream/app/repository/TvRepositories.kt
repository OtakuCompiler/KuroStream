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

package com.kurostream.app.repository

import com.kurostream.app.model.Episode
import com.kurostream.app.model.MediaItem
import com.kurostream.app.model.PlaybackUrl
import kotlinx.coroutines.flow.Flow

/**
 * Merge note: the tv_app_phases_31_40 archive's ViewModels were written against a
 * `com.kurostream.domain.repository.MediaRepository` with THIS shape — which collides
 * by name with, but is incompatible with, the canonical `domain.repository.MediaRepository`
 * introduced in the phases 11-20 merge (profileId-keyed CRUD over `domain.model.MediaItem`).
 *
 * Rather than let two incompatible interfaces fight over the same name, this one lives in
 * `com.kurostream.app.repository` and the ViewModels' imports were repointed here. See
 * [MediaRepositoryAdapter] for how this delegates to the real, data-backed repository.
 */
interface MediaRepository {
    suspend fun getFeatured(): Result<List<MediaItem>>
    fun observeTrending(): Flow<Result<List<MediaItem>>>
    fun observeNewReleases(): Flow<Result<List<MediaItem>>>
    fun observeSeasonal(): Flow<Result<List<MediaItem>>>
    suspend fun getMediaById(mediaId: String): Result<MediaItem>
    suspend fun getEpisodes(mediaId: String): Result<List<Episode>>
    suspend fun getPlaybackUrl(mediaId: String, episodeId: String?): Result<PlaybackUrl>
    suspend fun getNextEpisode(mediaId: String, episodeId: String?): Result<Episode>
}

interface WatchProgressRepository {
    fun getContinueWatching(): Flow<Result<List<MediaItem>>>
    suspend fun saveProgress(mediaId: String, episodeId: String?, positionMs: Long, durationMs: Long)
}

interface FavoritesRepository {
    fun isFavorite(mediaId: String): Flow<Boolean>
    suspend fun toggleFavorite(mediaId: String)
}
