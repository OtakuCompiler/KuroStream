package com.kurostream.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.domain.model.AnimeSeason
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * UI State for Home Screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val featuredAnime: List<Anime> = emptyList(),
    val continueWatching: List<AnimeListEntry> = emptyList(),
    val trendingAnime: List<Anime> = emptyList(),
    val popularAnime: List<Anime> = emptyList(),
    val seasonalAnime: List<Anime> = emptyList(),
    val recentlyUpdated: List<Anime> = emptyList(),
    val recommendations: List<Anime> = emptyList()
)

/**
 * ViewModel for Home Screen.
 * 
 * Manages data loading and state for the home screen content.
 * Uses parallel loading for better performance.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeContent()
    }

    /**
     * Load all home screen content in parallel.
     */
    fun loadHomeContent() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load all sections in parallel
                launch { loadFeaturedAnime() }
                launch { loadContinueWatching() }
                launch { loadTrendingAnime() }
                launch { loadPopularAnime() }
                launch { loadSeasonalAnime() }
                launch { loadRecentlyUpdated() }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load content"
                    ) 
                }
            }
        }
    }

    /**
     * Refresh all content.
     */
    fun refresh() {
        loadHomeContent()
    }

    private suspend fun loadFeaturedAnime() {
        animeRepository.getTrendingAnime(page = 1, perPage = 5)
            .catch { e ->
                // Log error but don't fail the whole screen
                e.printStackTrace()
            }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { 
                        it.copy(
                            featuredAnime = anime,
                            isLoading = false
                        ) 
                    }
                }
                result.onFailure { e ->
                    // Feature can fail silently - other sections will still show
                    e.printStackTrace()
                }
            }
    }

    private suspend fun loadContinueWatching() {
        animeRepository.getContinueWatching(limit = 10)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { entries ->
                    _uiState.update { it.copy(continueWatching = entries) }
                }
            }
    }

    private suspend fun loadTrendingAnime() {
        animeRepository.getTrendingAnime(page = 1, perPage = 20)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { 
                        it.copy(
                            trendingAnime = anime,
                            isLoading = false
                        ) 
                    }
                }
            }
    }

    private suspend fun loadPopularAnime() {
        animeRepository.getPopularAnime(page = 1, perPage = 20)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { it.copy(popularAnime = anime) }
                }
            }
    }

    private suspend fun loadSeasonalAnime() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        val season = when (month) {
            in 0..2 -> AnimeSeason.WINTER
            in 3..5 -> AnimeSeason.SPRING
            in 6..8 -> AnimeSeason.SUMMER
            else -> AnimeSeason.FALL
        }
        
        animeRepository.getSeasonalAnime(year = year, season = season, page = 1, perPage = 20)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { it.copy(seasonalAnime = anime) }
                }
            }
    }

    private suspend fun loadRecentlyUpdated() {
        animeRepository.getRecentlyUpdated(page = 1, perPage = 20)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { it.copy(recentlyUpdated = anime) }
                }
            }
    }
}
