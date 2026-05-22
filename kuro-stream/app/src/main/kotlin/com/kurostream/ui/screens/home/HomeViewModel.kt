package com.kurostream.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.plugin.PluginManager
import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.WatchHistoryEntry
import com.kurostream.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredContent: List<ContentItem> = emptyList(),
    val continueWatching: List<WatchHistoryEntry> = emptyList(),
    val trending: List<ContentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPlugins: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
        loadContent()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.getRecentHistory().collect { history ->
                _uiState.update { it.copy(continueWatching = history) }
            }
        }
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                pluginManager.initialize()
                val plugins = pluginManager.activePlugins.first()
                if (plugins.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, hasPlugins = false)
                    }
                    return@launch
                }

                val trending = pluginManager.searchContent("popular")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasPlugins = true,
                        trending = trending,
                        featuredContent = trending.take(5)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun refresh() = loadContent()
}
