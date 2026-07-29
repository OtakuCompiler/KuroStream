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

package com.kurostream.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Downloads screen.
 */
sealed interface DownloadsUiState {
    data object Loading : DownloadsUiState
    data class Success(val downloads: List<DownloadItem>) : DownloadsUiState
    data class Error(val message: String) : DownloadsUiState
}

/**
 * ViewModel for the Downloads screen.
 * Fetches media items and maps them into [DownloadItem] objects for display.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadsUiState>(DownloadsUiState.Loading)
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    /**
     * Load media items and watch progress, then emit a [DownloadsUiState].
     */
    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.update { DownloadsUiState.Loading }
            try {
                // Take a single snapshot from both reactive sources
                val mediaItems = mediaRepository.getMediaItems().first()
                val progressMap = watchProgressRepository.getWatchProgress().first()

                val downloadItems = mediaItems.map { item ->
                    val progressFraction = progressMap[item.id]?.let { pos ->
                        if (item.duration > 0) (pos.toFloat() / (item.duration * 60_000f)).coerceIn(0f, 1f)
                        else 0f
                    } ?: 0f

                    DownloadItem(
                        id = item.id,
                        title = item.title,
                        description = buildString {
                            if (item.year > 0) append(item.year)
                            if (item.genre.isNotEmpty()) {
                                if (isNotEmpty()) append(" • ")
                                append(item.genre.first())
                            }
                            if (isEmpty()) append("Media")
                        },
                        progress = progressFraction,
                        status = if (progressFraction >= 1f) DownloadStatus.COMPLETED
                                else DownloadStatus.DOWNLOADING,
                        size = if (item.duration > 0) "${item.duration} min" else "--",
                    )
                }
                _uiState.update { DownloadsUiState.Success(downloadItems) }
            } catch (e: Exception) {
                _uiState.update {
                    DownloadsUiState.Error(
                        e.message ?: "Failed to load downloads"
                    )
                }
            }
        }
    }
}
