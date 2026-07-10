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
import com.kurostream.common.result.Result
import com.kurostream.data.home.CustomHomeRowRepository
import com.kurostream.domain.home.CustomHomeRow
import com.kurostream.domain.home.RowPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
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
            val rows = repository.getCustomRows()
            _customRows.value = rows
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
            val result = repository.createRow(row.copy(id = java.util.UUID.randomUUID().toString()))
            result.onSuccess { loadRows() }
            result.onFailure { e -> /* Handle error */ }
        }
    }

    fun updateRow(row: CustomHomeRow) {
        viewModelScope.launch {
            val result = repository.updateRow(row)
            result.onSuccess { loadRows() }
            result.onFailure { e -> /* Handle error */ }
        }
    }

    fun deleteRow(rowId: String) {
        viewModelScope.launch {
            val result = repository.deleteRow(rowId)
            result.onSuccess { loadRows() }
            result.onFailure { e -> /* Handle error */ }
        }
    }

    fun reorderRows(rowIds: List<String>) {
        viewModelScope.launch {
            val result = repository.reorderRows(rowIds)
            result.onSuccess { loadRows() }
            result.onFailure { e -> /* Handle error */ }
        }
    }

    fun toggleRowVisibility(rowId: String, visible: Boolean) {
        viewModelScope.launch {
            val rows = _customRows.value.map { row ->
                if (row.id == rowId) row.copy(isVisible = visible) else row
            }
            _customRows.value = rows
            // Persist
        }
    }

    fun previewRow(row: CustomHomeRow) {
        viewModelScope.launch {
            _previewResult.value = Result.loading()
            val result = repository.generatePreview(row)
            _previewResult.value = result
        }
    }
}