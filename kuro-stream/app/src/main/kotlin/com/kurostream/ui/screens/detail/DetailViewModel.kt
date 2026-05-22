package com.kurostream.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.plugin.PluginManager
import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.StreamSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val content: ContentItem? = null,
    val streams: List<StreamSource> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingStreams: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val pluginManager: PluginManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadContent(contentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val placeholder = ContentItem(
                    id = contentId,
                    title = "Loading...",
                    type = "movie"
                )
                _uiState.update { it.copy(content = placeholder, isLoading = false, isLoadingStreams = true) }
                fetchStreams(contentId, "movie")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun fetchStreams(contentId: String, type: String) {
        try {
            val streams = pluginManager.getStreamsForContent(contentId, type)
            _uiState.update { it.copy(streams = streams, isLoadingStreams = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingStreams = false) }
        }
    }
}
