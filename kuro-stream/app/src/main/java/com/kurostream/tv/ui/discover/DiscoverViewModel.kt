package com.kurostream.tv.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeSeason
import com.kurostream.tv.domain.model.AnimeType
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * UI State for Discover Screen.
 */
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val animeList: List<Anime> = emptyList(),
    val selectedCategory: DiscoverCategory = DiscoverCategory.ALL,
    val isFilterPanelVisible: Boolean = false,
    val selectedGenres: Set<String> = emptySet(),
    val selectedTypes: Set<AnimeType> = emptySet(),
    val selectedSeason: AnimeSeason? = null,
    val selectedYear: Int? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)

/**
 * ViewModel for Discover Screen.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadAnime()
    }

    fun selectCategory(category: DiscoverCategory) {
        if (category == _uiState.value.selectedCategory) return
        
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun toggleFilterPanel() {
        _uiState.update { it.copy(isFilterPanelVisible = !it.isFilterPanelVisible) }
    }

    fun toggleGenre(genre: String) {
        _uiState.update { state ->
            val newGenres = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(
                selectedGenres = newGenres,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun toggleType(type: AnimeType) {
        _uiState.update { state ->
            val newTypes = if (state.selectedTypes.contains(type)) {
                state.selectedTypes - type
            } else {
                state.selectedTypes + type
            }
            state.copy(
                selectedTypes = newTypes,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun selectSeason(season: AnimeSeason?) {
        _uiState.update { 
            it.copy(
                selectedSeason = season,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun selectYear(year: Int?) {
        _uiState.update { 
            it.copy(
                selectedYear = year,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun clearFilters() {
        _uiState.update { 
            it.copy(
                selectedGenres = emptySet(),
                selectedTypes = emptySet(),
                selectedSeason = null,
                selectedYear = null,
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
        loadAnime()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMorePages) return
        
        _uiState.update { it.copy(isLoadingMore = true) }
        loadAnime(page = state.currentPage + 1, append = true)
    }

    fun refresh() {
        _uiState.update { 
            it.copy(
                animeList = emptyList(),
                currentPage = 1,
                hasMorePages = true,
                error = null
            )
        }
        loadAnime()
    }

    private fun loadAnime(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch(ioDispatcher) {
            if (!append) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            
            val flow = when (_uiState.value.selectedCategory) {
                DiscoverCategory.ALL -> animeRepository.getPopularAnime(page = page)
                DiscoverCategory.TRENDING -> animeRepository.getTrendingAnime(page = page)
                DiscoverCategory.POPULAR -> animeRepository.getPopularAnime(page = page)
                DiscoverCategory.SEASONAL -> {
                    val calendar = Calendar.getInstance()
                    val year = _uiState.value.selectedYear ?: calendar.get(Calendar.YEAR)
                    val season = _uiState.value.selectedSeason ?: getCurrentSeason()
                    animeRepository.getSeasonalAnime(year = year, season = season, page = page)
                }
                DiscoverCategory.TOP_RATED -> animeRepository.getPopularAnime(page = page)
                DiscoverCategory.UPCOMING -> animeRepository.getRecentlyUpdated(page = page)
            }
            
            flow.catch { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load anime"
                    )
                }
            }.collect { result ->
                result.onSuccess { newAnime ->
                    // Apply client-side filtering
                    val filteredAnime = applyFilters(newAnime)
                    
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            animeList = if (append) {
                                state.animeList + filteredAnime
                            } else {
                                filteredAnime
                            },
                            currentPage = page,
                            hasMorePages = newAnime.size >= 20
                        )
                    }
                }
                result.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message ?: "Failed to load anime"
                        )
                    }
                }
            }
        }
    }

    private fun applyFilters(animeList: List<Anime>): List<Anime> {
        val state = _uiState.value
        
        return animeList.filter { anime ->
            // Genre filter
            val genreMatch = state.selectedGenres.isEmpty() || 
                anime.genres.any { it in state.selectedGenres }
            
            // Type filter
            val typeMatch = state.selectedTypes.isEmpty() || 
                anime.type in state.selectedTypes
            
            genreMatch && typeMatch
        }
    }

    private fun getCurrentSeason(): AnimeSeason {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            in 0..2 -> AnimeSeason.WINTER
            in 3..5 -> AnimeSeason.SPRING
            in 6..8 -> AnimeSeason.SUMMER
            else -> AnimeSeason.FALL
        }
    }
}
