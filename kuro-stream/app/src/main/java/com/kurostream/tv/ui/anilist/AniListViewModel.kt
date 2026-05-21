package com.kurostream.tv.ui.anilist

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.data.remote.anilist.AniListMediaEntry
import com.kurostream.tv.data.remote.anilist.AniListMediaStatus
import com.kurostream.tv.data.remote.anilist.AniListSyncManager
import com.kurostream.tv.data.remote.anilist.AniListUser
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for AniList Tab Screen.
 */
data class AniListUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val user: AniListUser? = null,
    val selectedTab: Int = 0,
    val currentList: List<AniListMediaEntry> = emptyList(),
    val planningList: List<AniListMediaEntry> = emptyList(),
    val completedList: List<AniListMediaEntry> = emptyList(),
    val droppedList: List<AniListMediaEntry> = emptyList(),
    val pausedList: List<AniListMediaEntry> = emptyList(),
    val error: String? = null,
    val loginIntent: Intent? = null
)

/**
 * ViewModel for AniList Tab Screen.
 */
@HiltViewModel
class AniListViewModel @Inject constructor(
    private val aniListSyncManager: AniListSyncManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AniListUiState())
    val uiState: StateFlow<AniListUiState> = _uiState.asStateFlow()
    
    init {
        observeLoginState()
    }
    
    private fun observeLoginState() {
        viewModelScope.launch(ioDispatcher) {
            aniListSyncManager.isLoggedIn.collect { isLoggedIn ->
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
                
                if (isLoggedIn) {
                    loadUserInfo()
                    loadAllLists()
                }
            }
        }
    }
    
    /**
     * Load initial data.
     */
    fun loadInitialData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            
            val isLoggedIn = aniListSyncManager.isLoggedIn.first()
            if (isLoggedIn) {
                loadUserInfo()
                loadAllLists()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private suspend fun loadUserInfo() {
        aniListSyncManager.currentUser.collect { user ->
            _uiState.update { it.copy(user = user) }
        }
    }
    
    private fun loadAllLists() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Load all lists in parallel
            launch { loadList(AniListMediaStatus.CURRENT) }
            launch { loadList(AniListMediaStatus.PLANNING) }
            launch { loadList(AniListMediaStatus.COMPLETED) }
            launch { loadList(AniListMediaStatus.DROPPED) }
            launch { loadList(AniListMediaStatus.PAUSED) }
        }
    }
    
    private suspend fun loadList(status: AniListMediaStatus) {
        aniListSyncManager.getAnimeList(status)
            .catch { e ->
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load list",
                        isLoading = false
                    )
                }
            }
            .collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        _uiState.update { state ->
                            when (status) {
                                AniListMediaStatus.CURRENT -> state.copy(
                                    currentList = entries,
                                    isLoading = false
                                )
                                AniListMediaStatus.PLANNING -> state.copy(
                                    planningList = entries,
                                    isLoading = false
                                )
                                AniListMediaStatus.COMPLETED -> state.copy(
                                    completedList = entries,
                                    isLoading = false
                                )
                                AniListMediaStatus.DROPPED -> state.copy(
                                    droppedList = entries,
                                    isLoading = false
                                )
                                AniListMediaStatus.PAUSED -> state.copy(
                                    pausedList = entries,
                                    isLoading = false
                                )
                                AniListMediaStatus.REPEATING -> state.copy(isLoading = false)
                            }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(
                                error = e.message ?: "Failed to load list",
                                isLoading = false
                            )
                        }
                    }
                )
            }
    }
    
    /**
     * Select a tab.
     */
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
    
    /**
     * Start AniList login flow.
     */
    fun startLogin() {
        val intent = aniListSyncManager.startOAuthFlow()
        _uiState.update { it.copy(loginIntent = intent) }
    }
    
    /**
     * Clear login intent after handling.
     */
    fun clearLoginIntent() {
        _uiState.update { it.copy(loginIntent = null) }
    }
    
    /**
     * Handle OAuth callback.
     */
    fun handleOAuthCallback(code: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            
            aniListSyncManager.handleOAuthCallback(code).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = user
                        )
                    }
                    loadAllLists()
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Login failed"
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Logout from AniList.
     */
    fun logout() {
        viewModelScope.launch(ioDispatcher) {
            aniListSyncManager.logout()
            _uiState.update { 
                AniListUiState(isLoading = false, isLoggedIn = false)
            }
        }
    }
    
    /**
     * Refresh anime list.
     */
    fun refreshList() {
        loadAllLists()
    }
    
    /**
     * Update anime progress.
     */
    fun updateProgress(mediaId: Int, progress: Int) {
        viewModelScope.launch(ioDispatcher) {
            aniListSyncManager.updateProgress(mediaId, progress).fold(
                onSuccess = {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            currentList = state.currentList.map { entry ->
                                if (entry.mediaId == mediaId) {
                                    entry.copy(progress = progress)
                                } else {
                                    entry
                                }
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to update progress")
                    }
                }
            )
        }
    }
    
    /**
     * Update anime score.
     */
    fun updateScore(mediaId: Int, score: Float) {
        viewModelScope.launch(ioDispatcher) {
            aniListSyncManager.updateScore(mediaId, score).fold(
                onSuccess = {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            currentList = state.currentList.map { entry ->
                                if (entry.mediaId == mediaId) {
                                    entry.copy(score = score)
                                } else {
                                    entry
                                }
                            },
                            completedList = state.completedList.map { entry ->
                                if (entry.mediaId == mediaId) {
                                    entry.copy(score = score)
                                } else {
                                    entry
                                }
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to update score")
                    }
                }
            )
        }
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
