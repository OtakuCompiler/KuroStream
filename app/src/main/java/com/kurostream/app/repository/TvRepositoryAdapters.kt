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

import com.kurostream.domain.model.Favorite
import com.kurostream.domain.model.MediaCategory
import com.kurostream.domain.model.WatchHistory
import com.kurostream.domain.repository.MediaRepository as CanonicalMediaRepository
import com.kurostream.domain.repository.ProfileRepository
import com.kurostream.app.model.Episode
import com.kurostream.app.model.PlaybackUrl
import com.kurostream.app.model.MediaItem as TvMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementations of the tv UI's expected repositories, backed by the canonical
 * (Room-backed) [CanonicalMediaRepository] / [ProfileRepository] from :domain + :data.
 *
 * Two methods are honest stand-ins rather than real features, because the canonical domain
 * model has no per-series episode concept (see MediaItemMapper.kt doc comment):
 *  - [MediaRepositoryAdapter.getEpisodes] returns a single synthetic Episode wrapping the
 *    MediaItem itself.
 *  - [MediaRepositoryAdapter.getNextEpisode] and [MediaRepositoryAdapter.observeSeasonal]
 *    have no underlying data source yet and return empty/failure results.
 * Extend :domain's model with a real Episode/Series concept to do these properly.
 */
@Singleton
class MediaRepositoryAdapter @Inject constructor(
    private val mediaRepository: CanonicalMediaRepository,
    private val profileRepository: ProfileRepository,
) : MediaRepository {

    private suspend fun activeProfileId(): String =
        profileRepository.getActiveProfile()?.id ?: "default"

    override suspend fun getFeatured(): Result<List<TvMediaItem>> = runCatching {
        mediaRepository.getTrending().take(10).map { it.toTvMediaItem() }
    }

    override fun observeTrending(): Flow<Result<List<TvMediaItem>>> =
        mediaRepository.observeMediaByCategory(MediaCategory.ANIME)
            .map { items -> Result.success(items.map { it.toTvMediaItem() }) }

    override fun observeNewReleases(): Flow<Result<List<TvMediaItem>>> =
        mediaRepository.observeMediaByCategory(MediaCategory.GENERAL)
            .map { items ->
                Result.success(items.sortedByDescending { it.releaseDate }.map { it.toTvMediaItem() })
            }

    override fun observeSeasonal(): Flow<Result<List<TvMediaItem>>> =
        mediaRepository.observeMediaByCategory(MediaCategory.ANIME)
            .map { Result.success(emptyList<TvMediaItem>()) } // TODO: no "season" concept in canonical model yet

    override suspend fun getMediaById(mediaId: String): Result<TvMediaItem> = runCatching {
        val item = mediaRepository.getMediaById(mediaId) ?: error("Media not found: $mediaId")
        val history = mediaRepository.getWatchHistory(mediaId, activeProfileId())
        item.toTvMediaItem(history)
    }

    override suspend fun getEpisodes(mediaId: String): Result<List<Episode>> = runCatching {
        val item = mediaRepository.getMediaById(mediaId) ?: error("Media not found: $mediaId")
        // Stand-in: canonical MediaItem is a single watchable unit, not a series with episodes.
        listOf(
            Episode(
                id = item.id,
                number = 1,
                title = item.title,
                thumbnail = item.posterUrl,
                durationMinutes = item.duration?.let { (it / 60_000L).toInt() }
            )
        )
    }

    override suspend fun getPlaybackUrl(mediaId: String, episodeId: String?): Result<PlaybackUrl> = runCatching {
        val item = mediaRepository.getMediaById(mediaId) ?: error("Media not found: $mediaId")
        val url = item.streamUrl ?: error("No stream URL available for $mediaId")
        PlaybackUrl(url = url, title = item.title)
    }

    override suspend fun getNextEpisode(mediaId: String, episodeId: String?): Result<Episode> =
        Result.failure(UnsupportedOperationException("Next-episode lookup requires a real series/episode model"))
}

@Singleton
class WatchProgressRepositoryAdapter @Inject constructor(
    private val mediaRepository: CanonicalMediaRepository,
    private val profileRepository: ProfileRepository,
) : WatchProgressRepository {

    override fun getContinueWatching(): Flow<Result<List<TvMediaItem>>> = flow {
        val profileId = profileRepository.getActiveProfile()?.id ?: "default"
        emitAll(
            mediaRepository.observeWatchHistory(profileId).map { history ->
                runCatching {
                    history.mapNotNull { h ->
                        mediaRepository.getMediaById(h.mediaItemId)?.toTvMediaItem(h)
                    }
                }
            }
        )
    }

    override suspend fun saveProgress(mediaId: String, episodeId: String?, positionMs: Long, durationMs: Long) {
        val profileId = profileRepository.getActiveProfile()?.id ?: "default"
        mediaRepository.saveWatchHistory(
            WatchHistory(
                id = "${profileId}_$mediaId",
                mediaItemId = mediaId,
                profileId = profileId,
                position = positionMs,
                duration = durationMs,
                episodeNumber = episodeId?.toIntOrNull()
            )
        )
    }
}

@Singleton
class FavoritesRepositoryAdapter @Inject constructor(
    private val mediaRepository: CanonicalMediaRepository,
    private val profileRepository: ProfileRepository,
) : FavoritesRepository {

    override fun isFavorite(mediaId: String): Flow<Boolean> = flow {
        val profileId = profileRepository.getActiveProfile()?.id ?: "default"
        emitAll(mediaRepository.observeFavorites(profileId).map { favs -> favs.any { it.mediaItemId == mediaId } })
    }

    override suspend fun toggleFavorite(mediaId: String) {
        val profileId = profileRepository.getActiveProfile()?.id ?: "default"
        if (mediaRepository.isFavorite(mediaId, profileId)) {
            mediaRepository.removeFavorite(mediaId, profileId)
        } else {
            mediaRepository.addFavorite(Favorite(id = "${profileId}_$mediaId", mediaItemId = mediaId, profileId = profileId))
        }
    }
}
