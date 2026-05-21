package com.kurostream.tv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.Episode
import com.kurostream.tv.domain.model.WatchStatus
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
import javax.inject.Inject

/**
 * UI State for Anime Detail Screen.
 */
data class AnimeDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val anime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val relatedAnime: List<Anime> = emptyList(),
    val isInMyList: Boolean = false,
    val isFavorite: Boolean = false,
    val selectedProviders: Set<String> = emptySet()
)

/**
 * ViewModel for Anime Detail Screen.
 */
@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = _uiState.asStateFlow()

    private var currentAnimeId: String? = null

    fun loadAnime(animeId: String) {
        if (animeId == currentAnimeId && _uiState.value.anime != null) {
            return // Already loaded
        }
        
        currentAnimeId = animeId
        
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Load anime details, episodes, and related in parallel
            launch { loadAnimeDetails(animeId) }
            launch { loadEpisodes(animeId) }
            launch { loadRelatedAnime(animeId) }
            launch { checkListStatus(animeId) }
            launch { checkFavoriteStatus(animeId) }
        }
    }

    private suspend fun loadAnimeDetails(animeId: String) {
        animeRepository.getAnimeById(animeId)
            .catch { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load anime"
                    )
                }
            }
            .collect { result ->
                result.onSuccess { anime ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            anime = anime
                        )
                    }
                }
                result.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load anime"
                        )
                    }
                }
            }
    }

    private suspend fun loadEpisodes(animeId: String) {
        animeRepository.getEpisodes(animeId)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { episodes ->
                    _uiState.update { it.copy(episodes = episodes) }
                }
            }
    }

    private suspend fun loadRelatedAnime(animeId: String) {
        animeRepository.getRecommendations(animeId, limit = 10)
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { related ->
                    _uiState.update { it.copy(relatedAnime = related) }
                }
            }
    }

    private suspend fun checkListStatus(animeId: String) {
        animeRepository.getMyList()
            .catch { e -> e.printStackTrace() }
            .collect { result ->
                result.onSuccess { entries ->
                    val isInList = entries.any { it.anime.id == animeId }
                    _uiState.update { it.copy(isInMyList = isInList) }
                }
            }
    }

    private suspend fun checkFavoriteStatus(animeId: String) {
        animeRepository.isFavorite(animeId)
            .catch { e -> e.printStackTrace() }
            .collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
    }

    fun toggleMyList() {
        val animeId = currentAnimeId ?: return
        val isCurrentlyInList = _uiState.value.isInMyList
        
        viewModelScope.launch(ioDispatcher) {
            if (isCurrentlyInList) {
                animeRepository.removeFromMyList(animeId)
                    .onSuccess {
                        _uiState.update { it.copy(isInMyList = false) }
                    }
            } else {
                animeRepository.addToMyList(animeId, WatchStatus.PLAN_TO_WATCH)
                    .onSuccess {
                        _uiState.update { it.copy(isInMyList = true) }
                    }
            }
        }
    }

    fun toggleFavorite() {
        val animeId = currentAnimeId ?: return
        val isCurrentlyFavorite = _uiState.value.isFavorite
        
        viewModelScope.launch(ioDispatcher) {
            if (isCurrentlyFavorite) {
                animeRepository.removeFromFavorites(animeId)
                    .onSuccess {
                        _uiState.update { it.copy(isFavorite = false) }
                    }
            } else {
                animeRepository.addToFavorites(animeId)
                    .onSuccess {
                        _uiState.update { it.copy(isFavorite = true) }
                    }
            }
        }
    }

    fun updateWatchStatus(status: WatchStatus) {
        val animeId = currentAnimeId ?: return
        
        viewModelScope.launch(ioDispatcher) {
            animeRepository.updateWatchStatus(animeId, status)
        }
    }
}
