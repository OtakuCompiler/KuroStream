package com.kurostream.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.MediaRepository
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
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RowState<MediaItem>>(RowState.Loading)
    val uiState: StateFlow<RowState<MediaItem>> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

    fun retry() {
        loadLibrary()
    }

    private fun loadLibrary() {
        mediaRepository.getMediaItems()
            .map { items ->
                when {
                    items.isEmpty() -> RowState.Success(emptyList())
                    else -> RowState.Success(items)
                }
            }
            .catch { e ->
                Timber.e(e, "Failed to load library")
                emit(RowState.Error(e.message ?: "Failed to load library"))
            }
            .collect { state ->
                _uiState.value = state
            }
    }
}