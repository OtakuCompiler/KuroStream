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

package com.kurostream.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.Episode
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.FavoritesRepository
import com.kurostream.app.repository.TvRepositories.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(
        val media: MediaItem,
        val episodes: List<Episode>,
        val isFavorite: Boolean
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(mediaId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = DetailsUiState.Loading

                val mediaItem = mediaRepository.getMediaItem(mediaId).first()
                if (mediaItem == null) {
                    _uiState.value = DetailsUiState.Error("Media not found")
                    return@launch
                }

                val favorites = favoritesRepository.getFavorites().first()
                val isFavorite = favorites.any { it.id == mediaId }

                _uiState.value = DetailsUiState.Success(
                    media = mediaItem,
                    episodes = mediaItem.episodes,
                    isFavorite = isFavorite
                )
            } catch (e: Exception) {
                _uiState.value = DetailsUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            try {
                val current = _uiState.value
                if (current is DetailsUiState.Success) {
                    val newFav = !current.isFavorite
                    if (newFav) {
                        favoritesRepository.addFavorite(current.media)
                    } else {
                        favoritesRepository.removeFavorite(mediaId)
                    }
                    _uiState.value = current.copy(isFavorite = newFav)
                }
            } catch (_: Exception) {
                /* ignore toggle error */
            }
        }
    }
}
