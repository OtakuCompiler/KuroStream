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
import com.kurostream.common.result.Result
import com.kurostream.app.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: kotlinx.coroutines.flow.StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: kotlinx.coroutines.flow.StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun setQuery(query: String) {
        _query.value = query
    }

    fun search() {
        val currentQuery = _query.value.trim()
        if (currentQuery.isBlank()) return

        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            val result = mediaRepository.searchRemote(currentQuery, 1, 20)
            result.fold(
                onSuccess = { mediaItems ->
                    val searchResults = mediaItems.map { item ->
                        SearchResultItem(
                            id = item.id,
                            title = item.title,
                            year = item.releaseDate?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).year } ?: 0,
                            type = item.category.name,
                            posterUrl = item.posterUrl ?: "",
                            score = item.rating ?: 0.0,
                            episodes = null
                        )
                    }
                    _uiState.value = SearchUiState.Success(searchResults, currentQuery)
                },
                onFailure = { error ->
                    _uiState.value = SearchUiState.Error(error.message ?: "Search failed")
                }
            )
        }
    }
}