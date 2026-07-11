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

package com.kurostream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.MediaRepository
import com.kurostream.app.repository.WatchProgressRepository
import com.kurostream.common.memory.LowRamDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RowState<out T> {
    data object Loading : RowState<Nothing>
    data class Success<T>(val items: List<T>) : RowState<T>
    data class Error(val message: String) : RowState<Nothing>
}

data class HomeUiState(
    val heroItems: List<MediaItem> = emptyList(),
    val continueWatching: RowState<MediaItem> = RowState.Loading,
    val trending: RowState<MediaItem> = RowState.Loading,
    val newReleases: RowState<MediaItem> = RowState.Loading,
    val seasonal: RowState<MediaItem> = RowState.Loading,
    val placeholderSections: List<PlaceholderSection> = listOf(
        PlaceholderSection("Top Rated"),
        PlaceholderSection("Recently Updated"),
        PlaceholderSection("My List")
    )
)

data class PlaceholderSection(val title: String)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isVisible = false
    private val deferLoading = LowRamDevice.isLowRamDevice()

    init {
        if (!deferLoading) {
            loadHomeData()
        }
    }

    fun onScreenVisible() {
        if (deferLoading && !isVisible) {
            isVisible = true
            loadHomeData()
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            mediaRepository.getFeatured()
                .onSuccess { featured ->
                    _uiState.update { it.copy(heroItems = featured.take(5)) }
                }
        }

        watchProgressRepository.getContinueWatching()
            .onEach { result ->
                _uiState.update {
                    it.copy(continueWatching = result.fold(
                        onSuccess = { items -> RowState.Success(items) },
                        onFailure = { e -> RowState.Error(e.message ?: "Unknown error") }
                    ))
                }
            }
            .launchIn(viewModelScope)

        mediaRepository.observeTrending()
            .onEach { result ->
                _uiState.update {
                    it.copy(trending = result.fold(
                        onSuccess = { items -> RowState.Success(items) },
                        onFailure = { e -> RowState.Error(e.message ?: "Unknown error") }
                    ))
                }
            }
            .launchIn(viewModelScope)

        mediaRepository.observeNewReleases()
            .onEach { result ->
                _uiState.update {
                    it.copy(newReleases = result.fold(
                        onSuccess = { items -> RowState.Success(items) },
                        onFailure = { e -> RowState.Error(e.message ?: "Unknown error") }
                    ))
                }
            }
            .launchIn(viewModelScope)

        mediaRepository.observeSeasonal()
            .onEach { result ->
                _uiState.update {
                    it.copy(seasonal = result.fold(
                        onSuccess = { items -> RowState.Success(items) },
                        onFailure = { e -> RowState.Error(e.message ?: "Unknown error") }
                    ))
                }
            }
            .launchIn(viewModelScope)
    }
}
