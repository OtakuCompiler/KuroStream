package com.kurostream.data.home

import com.kurostream.domain.result.Result
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.domain.home.CustomHomeRow
import com.kurostream.domain.home.CustomHomeRowRepository
import com.kurostream.domain.home.RowPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomHomeRowRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : CustomHomeRowRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _customRows = MutableStateFlow<List<CustomHomeRow>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch { loadRows() }
    }

    private suspend fun loadRows() {
        val stored = settingsDataStore.customHomeRows.first()
        val rows = if (stored.isBlank()) emptyList() else {
            try { json.decodeFromString<List<CustomHomeRow>>(stored) } catch (e: Exception) { emptyList() }
        }
        _customRows.value = rows
    }

    override suspend fun getRows(): List<CustomHomeRow> = _customRows.value

    override suspend fun saveRows(rows: List<CustomHomeRow>) {
        _customRows.value = rows
        val jsonString = json.encodeToString(rows)
        settingsDataStore.setCustomHomeRows(jsonString)
    }

    override suspend fun createRow(row: CustomHomeRow): Result<Unit> = Result.success(Unit)
    override suspend fun updateRow(row: CustomHomeRow): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRow(rowId: String): Result<Unit> = Result.success(Unit)
    override suspend fun reorderRows(rowIds: List<String>): Result<Unit> = Result.success(Unit)
    override suspend fun generatePreview(row: CustomHomeRow): Result<RowPreview> = Result.success(RowPreview(row, emptyList(), 0))
}
