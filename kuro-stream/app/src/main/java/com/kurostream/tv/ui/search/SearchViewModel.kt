package com.kurostream.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Anime> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 500L
        private const val MIN_QUERY_LENGTH = 2
    }

    init {
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            _queryFlow
                .debounce(DEBOUNCE_MS)
                .filter { it.length >= MIN_QUERY_LENGTH }
                .flatMapLatest { query ->
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    repository.searchAnime(query)
                }
                .catch { e ->
                    Timber.tag(TAG).e(e, "Search error")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Search failed. Please try again.")
                    }
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = { anime ->
                            _uiState.update {
                                it.copy(
                                    results = anime,
                                    isLoading = false,
                                    hasSearched = true,
                                    error = null
                                )
                            }
                        },
                        onFailure = { e ->
                            Timber.tag(TAG).e(e, "Search result failure")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Search failed",
                                    hasSearched = true
                                )
                            }
                        }
                    )
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(results = emptyList(), hasSearched = false, error = null) }
        }
        _queryFlow.value = query
    }

    fun clearSearch() {
        _uiState.update { SearchUiState() }
        _queryFlow.value = ""
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
