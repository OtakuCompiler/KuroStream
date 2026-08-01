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

package com.kurostream.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Immutable

@Immutable
data class SearchResultItem(
    val id: String,
    val title: String,
    val description: String = "",
    val type: String = "",
    val posterUrl: String = "",
    val score: Double = 0.0,
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    @Immutable
    data class Success(val items: List<SearchResultItem>, val query: String) : SearchUiState
    @Immutable
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Debounced search: automatically search 300ms after user stops typing
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) {
                        executeSearch(q.trim())
                    }
                }
        }
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun search() {
        val currentQuery = _query.value.trim()
        if (currentQuery.isNotBlank()) {
            executeSearch(currentQuery)
        }
    }

    private fun executeSearch(query: String) {
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            try {
                val results = mediaRepository.search(query)
                val searchResults = results.map { item ->
                    SearchResultItem(
                        id = item.id,
                        title = item.title,
                        year = item.year,
                        posterUrl = item.posterUrl,
                        score = item.rating.toDouble()
                    )
                }
                _uiState.value = SearchUiState.Success(searchResults, query)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }
}
