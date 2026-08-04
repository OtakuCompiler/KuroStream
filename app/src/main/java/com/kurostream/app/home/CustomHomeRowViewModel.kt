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

package com.kurostream.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.domain.result.Result
import com.kurostream.domain.home.CustomHomeRowRepository
import com.kurostream.domain.home.CustomHomeRow
import com.kurostream.domain.home.RowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class CustomHomeRowViewModel @Inject constructor(
    private val repository: CustomHomeRowRepository,
) : ViewModel() {

    private val _customRows = MutableStateFlow<List<CustomHomeRow>>(emptyList())
    val customRows = _customRows.asStateFlow()

    private val _showCreateDialog = MutableStateFlow<CustomHomeRow?>(null)
    val showCreateDialog = _showCreateDialog.asStateFlow()

    private val _showEditDialog = MutableStateFlow<CustomHomeRow?>(null)
    val showEditDialog = _showEditDialog.asStateFlow()

    private val _previewResult = MutableStateFlow<Result<RowPreview>?>(null)
    val previewResult = _previewResult.asStateFlow()

    init {
        loadRows()
    }

  private fun loadRows() {
    viewModelScope.launch {
      try {
        val rows = repository.getRows()
        _customRows.value = rows
      } catch (e: Exception) {
        _customRows.value = emptyList()
      }
    }
  }

    fun showCreateRowDialog() {
        _showCreateDialog.value = CustomHomeRow(
            id = "",
            title = "",
            filter = com.kurostream.domain.home.RowFilter(),
        )
    }

    fun showEditRowDialog(row: CustomHomeRow) {
        _showEditDialog.value = row
    }

    fun dismissDialogs() {
        _showCreateDialog.value = null
        _showEditDialog.value = null
    }

  fun createRow(row: CustomHomeRow) {
    viewModelScope.launch {
      try {
        val newRow = row.copy(id = java.util.UUID.randomUUID().toString())
        val result = repository.createRow(newRow)
        result.onSuccess { loadRows() }
        result.onError { e -> timber.log.Timber.w(e, "Failed to create custom row '${row.title}'") }
      } catch (e: Exception) {
        timber.log.Timber.e(e, "createRow threw unexpectedly")
      }
    }
  }

  fun updateRow(row: CustomHomeRow) {
    viewModelScope.launch {
      try {
        val result = repository.updateRow(row)
        result.onSuccess { loadRows() }
        result.onError { e -> timber.log.Timber.w(e, "Failed to update custom row '${row.id}'") }
      } catch (e: Exception) {
        timber.log.Timber.e(e, "updateRow threw unexpectedly")
      }
    }
  }

  fun deleteRow(rowId: String) {
    viewModelScope.launch {
      try {
        val result = repository.deleteRow(rowId)
        result.onSuccess { loadRows() }
        result.onError { e -> timber.log.Timber.w(e, "Failed to delete custom row '$rowId'") }
      } catch (e: Exception) {
        timber.log.Timber.e(e, "deleteRow threw unexpectedly for $rowId")
      }
    }
  }

  fun reorderRows(rowIds: List<String>) {
    viewModelScope.launch {
      try {
        val result = repository.reorderRows(rowIds)
        result.onSuccess { loadRows() }
        result.onError { e -> timber.log.Timber.w(e, "Failed to reorder rows") }
      } catch (e: Exception) {
        timber.log.Timber.e(e, "reorderRows threw unexpectedly")
      }
    }
  }

  fun toggleRowVisibility(rowId: String, visible: Boolean) {
    viewModelScope.launch {
      try {
        // Optimistic local update
        _customRows.value = _customRows.value.map { row ->
          if (row.id == rowId) row.copy(isVisible = visible) else row
        }
        // Persist via repo
        val updated = _customRows.value.find { it.id == rowId } ?: return@launch
        repository.updateRow(updated)
          .onError { e -> timber.log.Timber.w(e, "Failed to persist visibility toggle for $rowId") }
      } catch (e: Exception) {
        timber.log.Timber.e(e, "toggleRowVisibility threw unexpectedly")
      }
    }
  }

  fun previewRow(row: CustomHomeRow) {
    viewModelScope.launch {
      try {
        _previewResult.value = Result.loading()
        val result = repository.generatePreview(row)
        _previewResult.value = result
      } catch (e: Exception) {
        _previewResult.value = Result.error(e)
      }
    }
  }
}