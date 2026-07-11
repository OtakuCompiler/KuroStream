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

package com.kurostream.launcher.extensions.plex

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
class PlexLibrarySync @Inject constructor(
    private val authManager: PlexAuthManager,
    private val libraryDao: LibraryDao
) {

    fun syncLibraries(): Flow<SyncProgress> = flow {
        emit(SyncProgress.Started)

        val service = authManager.getServerService()
            ?: run { emit(SyncProgress.Error("Not connected to Plex server")); return@flow }

        try {
            val sectionsResponse = service.getLibrarySections()
            if (!sectionsResponse.isSuccessful) {
                emit(SyncProgress.Error("Failed: ${sectionsResponse.code()}"))
                return@flow
            }

            val sections = sectionsResponse.body()?.mediaContainer?.directories ?: emptyList()
            emit(SyncProgress.SectionsFetched(sections.size))

            var totalItems = 0
            var processedItems = 0

            for (section in sections) {
                emit(SyncProgress.SyncingSection(section.title))
                val itemsResponse = service.getLibraryItems(section.key)

                if (itemsResponse.isSuccessful) {
                    val metadata = itemsResponse.body()?.mediaContainer?.metadata ?: emptyList()
                    totalItems += metadata.size
                    val entities = metadata.map { mapToEntity(it, section.key, section.title) }
                    libraryDao.insertAll(entities)
                    processedItems += entities.size
                    emit(SyncProgress.ItemsSynced(processedItems, totalItems))
                }
            }
            emit(SyncProgress.Completed(processedItems))
        } catch (e: Exception) {
            emit(SyncProgress.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    private fun mapToEntity(metadata: PlexMetadata, sectionKey: String, sectionTitle: String): LibraryItemEntity {
        val mediaType = when (metadata.type) {
            "movie" -> MediaType.MOVIE
            "show" -> MediaType.SERIES
            "episode" -> MediaType.EPISODE
            "season" -> MediaType.SEASON
            else -> MediaType.UNKNOWN
        }
        val serverUrl = authManager.getServerUrl()?.trimEnd('/') ?: ""
        val thumbUrl = metadata.thumb?.let { "$serverUrl$it?X-Plex-Token=${authManager.getAccessToken()}" }
        val artUrl = metadata.art?.let { "$serverUrl$it?X-Plex-Token=${authManager.getAccessToken()}" }

        return LibraryItemEntity(
            id = "plex_${metadata.ratingKey}",
            externalId = metadata.ratingKey,
            source = "plex",
            title = metadata.title,
            overview = metadata.summary,
            mediaType = mediaType,
            libraryId = sectionKey,
            libraryName = sectionTitle,
            seriesName = metadata.grandparentTitle,
            seasonNumber = metadata.parentIndex,
            episodeNumber = metadata.index,
            durationMs = metadata.duration ?: 0L,
            progressMs = metadata.viewOffset ?: 0L,
            isWatched = (metadata.viewCount ?: 0) > 0,
            posterPath = thumbUrl,
            backdropPath = artUrl,
            streamUrl = null,
            lastSynced = System.currentTimeMillis()
        )
    }

    sealed class SyncProgress {
        object Started : SyncProgress()
        data class SectionsFetched(val count: Int) : SyncProgress()
        data class SyncingSection(val name: String) : SyncProgress()
        data class ItemsSynced(val processed: Int, val total: Int) : SyncProgress()
        data class Completed(val totalItems: Int) : SyncProgress()
        data class Error(val message: String) : SyncProgress()
    }
}
