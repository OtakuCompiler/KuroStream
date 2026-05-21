package com.kurostream.tv.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.domain.model.WatchStatus
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for My List Screen.
 */
data class MyListUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val allEntries: List<AnimeListEntry> = emptyList(),
    val filteredList: List<AnimeListEntry> = emptyList(),
    val selectedStatus: WatchStatus? = null,
    val statusCounts: Map<WatchStatus, Int> = emptyMap()
)

/**
 * ViewModel for My List Screen.
 */
@HiltViewModel
class MyListViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState.asStateFlow()

    init {
        loadMyList()
    }

    fun selectStatus(status: WatchStatus?) {
        _uiState.update { state ->
            val filtered = if (status == null) {
                state.allEntries
            } else {
                state.allEntries.filter { it.progress.status == status }
            }
            state.copy(
                selectedStatus = status,
                filteredList = filtered
            )
        }
    }

    fun refresh() {
        loadMyList()
    }

    private fun loadMyList() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            animeRepository.getMyList()
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load list"
                        )
                    }
                }
                .collect { result ->
                    result.onSuccess { entries ->
                        val statusCounts = WatchStatus.entries.associateWith { status ->
                            entries.count { it.progress.status == status }
                        }
                        
                        val selectedStatus = _uiState.value.selectedStatus
                        val filtered = if (selectedStatus == null) {
                            entries
                        } else {
                            entries.filter { it.progress.status == selectedStatus }
                        }
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                allEntries = entries,
                                filteredList = filtered,
                                statusCounts = statusCounts
                            )
                        }
                    }
                    result.onFailure { e ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load list"
                            )
                        }
                    }
                }
        }
    }

    fun updateWatchStatus(animeId: String, status: WatchStatus) {
        viewModelScope.launch(ioDispatcher) {
            animeRepository.updateWatchStatus(animeId, status)
                .onSuccess {
                    // Refresh list after update
                    loadMyList()
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to update status")
                    }
                }
        }
    }

    fun removeFromList(animeId: String) {
        viewModelScope.launch(ioDispatcher) {
            animeRepository.removeFromMyList(animeId)
                .onSuccess {
                    loadMyList()
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to remove anime")
                    }
                }
        }
    }
}
