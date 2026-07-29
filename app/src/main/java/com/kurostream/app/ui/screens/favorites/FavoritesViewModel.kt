package com.kurostream.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.FavoritesRepository
import com.kurostream.app.ui.screens.home.RowState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RowState<MediaItem>>(RowState.Loading)
    val uiState: StateFlow<RowState<MediaItem>> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            try {
                favoritesRepository.removeFavorite(itemId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove favorite $itemId")
            }
        }
    }

    fun retry() {
        loadFavorites()
    }

    private fun loadFavorites() {
        favoritesRepository.getFavorites()
            .map { items ->
                if (items.isEmpty()) RowState.Success(emptyList())
                else RowState.Success(items)
            }
            .catch { e ->
                Timber.e(e, "Failed to load favorites")
                emit(RowState.Error(e.message ?: "Failed to load favorites"))
            }
            .collect { state ->
                _uiState.value = state
            }
    }
}