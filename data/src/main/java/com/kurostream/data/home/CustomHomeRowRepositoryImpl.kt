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
import com.kurostream.domain.home.CustomHomeRow
import com.kurostream.domain.home.CustomHomeRowRepository
import com.kurostream.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override val customRows = _customRows.asStateFlow()
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
        _customRows.value = rows.sortedBy { it.position }
    }

    override suspend fun getCustomRows(): List<CustomHomeRow> = _customRows.value

    override suspend fun createRow(row: CustomHomeRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var rows = _customRows.value.toMutableList()
            val newRow = row.copy(
                position = rows.size,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            rows.add(newRow)
            _customRows.value = rows
            saveRows(rows)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRow(row: CustomHomeRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var rows = _customRows.value.toMutableList()
            val index = rows.indexOfFirst { it.id == row.id }
            if (index >= 0) {
                rows[index] = row.copy(updatedAt = System.currentTimeMillis())
                _customRows.value = rows
                saveRows(rows)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Row not found: ${row.id}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRow(rowId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var rows = _customRows.value.toMutableList()
            rows.removeAll { it.id == rowId }
            rows = rows.mapIndexed { idx, row -> row.copy(position = idx) }
            _customRows.value = rows
            saveRows(rows)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderRows(rowIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rows = _customRows.value.toMutableList()
            val reordered = rowIds.mapNotNull { id ->
                rows.find { it.id == id }
            }.mapIndexed { idx, row ->
                row.copy(position = idx, updatedAt = System.currentTimeMillis())
            }
            val remaining = rows.filter { it.id !in rowIds }
            val allRows = reordered + remaining.mapIndexed { idx, row ->
                row.copy(position = reordered.size + idx)
            }
            _customRows.value = allRows
            saveRows(allRows)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveRows(rows: List<CustomHomeRow>) {
        val jsonString = json.encodeToString(rows)
        settingsDataStore.setCustomHomeRows(jsonString)
    }

    suspend fun generatePreview(row: CustomHomeRow): Result<RowPreview> = withContext(Dispatchers.IO) {
        try {
            val filter = row.filter
            val mediaResult = mediaRepository.searchMedia(
                query = "",
                genres = filter.genres,
                studios = filter.studios,
                yearStart = filter.yearRange?.start,
                yearEnd = filter.yearRange?.end,
                minRating = filter.ratingRange?.min,
                maxRating = filter.ratingRange?.max,
                status = filter.status,
                mediaTypes = filter.mediaTypes,
                limit = row.limit,
            )

            val items = mediaResult.getOrNull()?.take(5)?.map { media ->
                PreviewItem(
                    id = media.id,
                    title = media.title,
                    posterUrl = media.coverImageUrl,
                    rating = media.score,
                    year = media.releaseDate?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).year }
                )
            } ?: emptyList()

            Result.success(RowPreview(
                row = row,
                sampleItems = items,
                totalCount = mediaResult.getOrNull()?.size ?: 0,
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}