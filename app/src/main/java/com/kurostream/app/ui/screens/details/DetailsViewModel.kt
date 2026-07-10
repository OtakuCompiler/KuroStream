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
import com.kurostream.app.repository.FavoritesRepository
import com.kurostream.app.repository.MediaRepository
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
            _uiState.value = DetailsUiState.Loading

            val mediaResult = mediaRepository.getMediaById(mediaId)
            val episodesResult = mediaRepository.getEpisodes(mediaId)
            val isFav = favoritesRepository.isFavorite(mediaId).first()

            _uiState.value = when {
                mediaResult.isFailure -> DetailsUiState.Error(
                    mediaResult.exceptionOrNull()?.message ?: "Failed to load"
                )
                else -> DetailsUiState.Success(
                    media = mediaResult.getOrThrow(),
                    episodes = episodesResult.getOrNull() ?: emptyList(),
                    isFavorite = isFav
                )
            }
        }
    }

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(mediaId)
            val current = _uiState.value
            if (current is DetailsUiState.Success) {
                _uiState.value = current.copy(isFavorite = !current.isFavorite)
            }
        }
    }
}
