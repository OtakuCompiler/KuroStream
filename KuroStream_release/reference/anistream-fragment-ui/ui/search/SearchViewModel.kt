package com.kurostream.legacyui.anistream.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.search.SearchRepository
import com.kurostream.data.anistream.model.AnimeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kurostream.data.anistream.search.SearchFilter

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val recentSearchDao: RecentSearchDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle(emptyList()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentQuery: String = ""
    private var currentFilters: List<SearchFilter> = listOf(SearchFilter.ALL)

    init {
        loadRecentSearches()
    }

    fun updateQuery(query: String) {
        currentQuery = query
        if (query.isEmpty()) {
            loadRecentSearches()
        } else if (query.length < 2) {
            viewModelScope.launch {
                val suggestions = searchRepository.getSuggestions(query)
                _uiState.value = SearchUiState.Suggestions(suggestions)
            }
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        currentQuery = query

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading

            try {
                val results = searchRepository.search(
                    query = query,
                    filters = currentFilters,
                    enableFuzzy = true,
                    enableTypoCorrection = true
                )

                if (results.isEmpty()) {
                    val fuzzyResults = searchRepository.fuzzySearch(query)
                    if (fuzzyResults.isEmpty()) {
                        _uiState.value = SearchUiState.Empty(
                            "No results found for "$query".\nTry checking your spelling or browse categories."
                        )
                    } else {
                        _uiState.value = SearchUiState.Results(fuzzyResults)
                    }
                } else {
                    _uiState.value = SearchUiState.Results(results)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error("Search failed: ${e.message}")
            }
        }
    }

    fun updateFilters(filters: List<SearchFilter>) {
        currentFilters = filters
        if (currentQuery.isNotEmpty()) {
            performSearch(currentQuery)
        }
    }

    fun saveRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchDao.insert(RecentSearchEntity(query = query, timestamp = System.currentTimeMillis()))
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchDao.deleteByQuery(query)
            loadRecentSearches()
        }
    }

    fun showRecentSearches() {
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            val recent = recentSearchDao.getRecentSearches(10)
            _uiState.value = SearchUiState.Idle(recent.map { it.query })
        }
    }

    fun onItemFocused(item: AnimeItem) {
        // Preload details
    }
}

sealed class SearchUiState {
    data class Idle(val recentSearches: List<String>) : SearchUiState()
    object Loading : SearchUiState()
    data class Suggestions(val suggestions: List<String>) : SearchUiState()
    data class Results(val results: List<AnimeItem>) : SearchUiState()
    data class Empty(val message: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
