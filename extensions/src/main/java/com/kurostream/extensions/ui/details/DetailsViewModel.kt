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

package com.kurostream.extensions.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.extensions.domain.model.*
import com.kurostream.extensions.kitsu.KitsuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kitsuRepository: KitsuRepository
) : ViewModel() {

    private val mediaId: String = checkNotNull(savedStateHandle["mediaId"])

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init { loadDetails() }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            try {
                val kitsuId = mediaId.removePrefix("kitsu:")
                val detailsDeferred = async { kitsuRepository.getAnimeDetails(kitsuId) }
                val charactersDeferred = async { kitsuRepository.getAnimeCharacters(kitsuId) }
                val staffDeferred = async { kitsuRepository.getAnimeStaff(kitsuId) }

                val detailsResult = detailsDeferred.await()
                val charactersResult = charactersDeferred.await()
                val staffResult = staffDeferred.await()

                detailsResult.onSuccess { mediaDetail ->
                    val enriched = mediaDetail.copy(
                        characters = charactersResult.getOrNull() ?: mediaDetail.characters,
                        staff = staffResult.getOrNull() ?: mediaDetail.staff
                    )
                    _uiState.value = DetailsUiState.Success(enriched)
                }.onFailure {
                    _uiState.value = DetailsUiState.Error(it.message ?: "Failed to load details")
                }
            } catch (e: Exception) {
                _uiState.value = DetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() { loadDetails() }
}

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val mediaDetail: MediaDetail) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}
