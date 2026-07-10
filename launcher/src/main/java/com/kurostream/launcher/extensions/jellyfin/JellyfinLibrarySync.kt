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

package com.kurostream.launcher.extensions.jellyfin

import com.kurostream.launcher.data.local.LibraryDao
import com.kurostream.launcher.data.local.entity.LibraryItemEntity
import com.kurostream.launcher.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinLibrarySync @Inject constructor(
    private val authManager: JellyfinAuthManager,
    private val libraryDao: LibraryDao
) {

    fun syncLibraries(): Flow<SyncProgress> = flow {
        emit(SyncProgress.Started)

        val service = authManager.getAuthenticatedService()
            ?: run {
                emit(SyncProgress.Error("Not authenticated with Jellyfin"))
                return@flow
            }

        val userId = authManager.getUserId()
            ?: run {
                emit(SyncProgress.Error("User ID not found"))
                return@flow
            }

        try {
            // Sync views (libraries)
            val viewsResponse = service.getViews(userId)
            if (!viewsResponse.isSuccessful) {
                emit(SyncProgress.Error("Failed to fetch views: ${viewsResponse.code()}"))
                return@flow
            }

            val views = viewsResponse.body()?.Items ?: emptyList()
            emit(SyncProgress.ViewsFetched(views.size))

            var totalItems = 0
            var processedItems = 0

            for (view in views) {
                emit(SyncProgress.SyncingView(view.Name))

                val itemsResponse = service.getItems(
                    userId = userId,
                    parentId = view.Id,
                    recursive = true,
                    fields = "Overview,Path,RunTimeTicks,MediaSources,UserData,SeriesName,SeasonName,IndexNumber,ParentIndexNumber"
                )

                if (itemsResponse.isSuccessful) {
                    val items = itemsResponse.body()?.Items ?: emptyList()
                    totalItems += items.size

                    val entities = items.map { item ->
                        mapToEntity(item, view.Id, view.Name)
                    }

                    libraryDao.insertAll(entities)
                    processedItems += entities.size
                    emit(SyncProgress.ItemsSynced(processedItems, totalItems))
                }
            }

            emit(SyncProgress.Completed(processedItems))
        } catch (e: Exception) {
            emit(SyncProgress.Error(e.message ?: "Unknown error during sync"))
        }
    }.flowOn(Dispatchers.IO)

    private fun mapToEntity(
        item: JellyfinLibraryItem,
        libraryId: String,
        libraryName: String
    ): LibraryItemEntity {
        val mediaType = when (item.Type) {
            "Movie" -> MediaType.MOVIE
            "Series" -> MediaType.SERIES
            "Episode" -> MediaType.EPISODE
            "Season" -> MediaType.SEASON
            else -> MediaType.UNKNOWN
        }

        val durationMs = item.RunTimeTicks?.let { it / 10000 } ?: 0L
        val progressMs = item.UserData?.PlaybackPositionTicks?.let { it / 10000 } ?: 0L
        val isWatched = item.UserData?.Played ?: false

        return LibraryItemEntity(
            id = "jellyfin_${item.Id}",
            externalId = item.Id,
            source = "jellyfin",
            title = item.Name,
            overview = item.Overview,
            mediaType = mediaType,
            libraryId = libraryId,
            libraryName = libraryName,
            seriesName = item.SeriesName,
            seasonNumber = item.ParentIndexNumber,
            episodeNumber = item.IndexNumber,
            durationMs = durationMs,
            progressMs = progressMs,
            isWatched = isWatched,
            posterPath = item.ImageTags?.get("Primary"),
            backdropPath = item.BackdropImageTags?.firstOrNull(),
            streamUrl = null, // Resolved at playback time
            lastSynced = System.currentTimeMillis()
        )
    }

    sealed class SyncProgress {
        object Started : SyncProgress()
        data class ViewsFetched(val count: Int) : SyncProgress()
        data class SyncingView(val name: String) : SyncProgress()
        data class ItemsSynced(val processed: Int, val total: Int) : SyncProgress()
        data class Completed(val totalItems: Int) : SyncProgress()
        data class Error(val message: String) : SyncProgress()
    }
}
