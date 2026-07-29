package com.kurostream.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import com.kurostream.app.ui.screens.home.RowState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RowState<MediaItem>>(RowState.Loading)
    val uiState: StateFlow<RowState<MediaItem>> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun retry() {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = RowState.Loading
            try {
                val allItems = mediaRepository.getMediaItems().first()
                val progressMap = watchProgressRepository.getWatchProgress().first()

                val watchedItems = allItems
                    .filter { (progressMap[it.id] ?: 0L) > 0L }
                    .sortedByDescending { progressMap[it.id] }

                _uiState.value = if (watchedItems.isEmpty()) {
                    RowState.Success(emptyList())
                } else {
                    RowState.Success(watchedItems)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load history")
                _uiState.value = RowState.Error(e.message ?: "Failed to load history")
            }
        }
    }
}