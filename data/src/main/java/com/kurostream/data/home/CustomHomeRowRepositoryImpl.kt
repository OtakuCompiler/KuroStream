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

package com.kurostream.data.home

import com.kurostream.core.common.result.Result
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.home.CustomHomeRow
import com.kurostream.domain.home.CustomHomeRowRepository
import com.kurostream.domain.home.PreviewItem
import com.kurostream.domain.home.RowPreview
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomHomeRowRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val mediaRepository: MediaRepository,
) : CustomHomeRowRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _customRows = MutableStateFlow<List<CustomHomeRow>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            loadRows()
        }
    }

    private suspend fun loadRows() {
        val stored = settingsDataStore.customHomeRows.first()
        val rows = if (stored.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<CustomHomeRow>>(stored)
            } catch (e: Exception) {
                emptyList()
            }
        }
        _customRows.value = rows
    }

    override suspend fun getRows(): List<CustomHomeRow> = _customRows.value

    override suspend fun saveRows(rows: List<CustomHomeRow>) {
        _customRows.value = rows
        val jsonString = json.encodeToString(rows)
        settingsDataStore.setCustomHomeRows(jsonString)
    }

    suspend fun generatePreview(row: CustomHomeRow): Result<RowPreview> = withContext(Dispatchers.IO) {
        try {
            val filter = row.filter
            val mediaList = mediaRepository.searchRemote(
                query = "",
                source = null,
            )

            val yearRange = filter.yearRange
            val filtered = mediaList.filter { media ->
                (filter.genres.isEmpty() || filter.genres.any { it in media.genres }) &&
                    (filter.studios.isEmpty() || filter.studios.any { it in media.studios }) &&
                    (yearRange == null || media.seasonYear?.let { it in yearRange.start..yearRange.end } ?: true) &&
                    (filter.mediaTypes.isEmpty() || filter.mediaTypes.any { it.name == media.type.name })
            }

            val items = filtered.take(5).map { media ->
                PreviewItem(
                    id = media.id,
                    title = media.title,
                    posterUrl = media.coverImageUrl,
                    rating = media.score,
                    year = media.seasonYear,
                )
            }

            Result.success(RowPreview(
                row = row,
                sampleItems = items,
                totalCount = filtered.size,
            ))
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}
