// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Immutable
data class DetailsUiState(
    val item: MediaItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val provider: String = "",
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val mediaRepository: TvRepositories.MediaRepository,
    private val watchProgressRepository: TvRepositories.WatchProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun load(mediaId: String) {
        observeJob?.cancel()
        _state.value = DetailsUiState(isLoading = true)
        observeJob = mediaRepository.getMediaItem(mediaId)
            .onEach { item ->
                if (item != null) {
                    _state.value = DetailsUiState(
                        item = item,
                        isLoading = false,
                        provider = item.source.ifBlank { "local" },
                    )
                } else {
                    _state.value = DetailsUiState(
                        item = null,
                        isLoading = false,
                        error = "Media not found in the local library.",
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "DetailsViewModel.load($mediaId)")
                _state.value = DetailsUiState(
                    item = null,
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load",
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            kotlinx.coroutines.delay(2_500)
            if (_state.value.isLoading) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Taking longer than expected — try again.",
                )
            }
        }
    }
}
