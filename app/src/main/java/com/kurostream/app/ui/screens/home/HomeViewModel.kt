package com.kurostream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.MediaRepository
import com.kurostream.app.repository.SettingsRepository
import com.kurostream.app.repository.WatchProgressRepository
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.app.ui.components.LiveWallpaperType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val becauseYouWatched: RowState<MediaItem> = RowState.Loading,
    val becauseYouWatchedSource: String = "",
    val liveWallpaperEnabled: Boolean = false,
    val liveWallpaperType: LiveWallpaperType = LiveWallpaperType.CHERRY_BLOSSOM,
    val placeholderSections: List<PlaceholderSection> = listOf(
        PlaceholderSection("Top Rated"), PlaceholderSection("Recently Updated"), PlaceholderSection("My List"),
    ),
)

data class PlaceholderSection(val title: String)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var isVisible = false
    private val deferLoading = LowRamDevice.isLowRamDevice()

    init {
        settingsRepository.observeSettings().onEach { s ->
            _uiState.update { it.copy(
                liveWallpaperEnabled = s.liveWallpaperEnabled,
                liveWallpaperType = try { LiveWallpaperType.valueOf(s.liveWallpaperType) } catch (_: Exception) { LiveWallpaperType.CHERRY_BLOSSOM },
            ) }
        }.launchIn(viewModelScope)
        if (!deferLoading) loadHomeData()
    }

    fun onScreenVisible() { if (deferLoading && !isVisible) { isVisible = true; loadHomeData() } }

    fun setLiveWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLiveWallpaperEnabled(enabled) }
        _uiState.update { it.copy(liveWallpaperEnabled = enabled) }
    }

    fun setLiveWallpaperType(type: LiveWallpaperType) {
        viewModelScope.launch { settingsRepository.setLiveWallpaperType(type.name) }
        _uiState.update { it.copy(liveWallpaperType = type) }
    }

    private fun loadHomeData() {
        viewModelScope.launch { mediaRepository.getFeatured().onSuccess { featured -> _uiState.update { state -> state.copy(heroItems = featured.take(5)) } } }
        watchProgressRepository.getContinueWatching().onEach { r ->
            _uiState.update { state -> state.copy(continueWatching = r.fold({ items -> RowState.Success(items) }, { e -> RowState.Error(e.message ?: "") })) }
        }.launchIn(viewModelScope)
        mediaRepository.observeTrending().onEach { r ->
            _uiState.update { state -> state.copy(trending = r.fold({ items -> RowState.Success(items) }, { e -> RowState.Error(e.message ?: "") })) }
        }.launchIn(viewModelScope)
        mediaRepository.observeNewReleases().onEach { r ->
            _uiState.update { state -> state.copy(newReleases = r.fold({ items -> RowState.Success(items) }, { e -> RowState.Error(e.message ?: "") })) }
        }.launchIn(viewModelScope)
        mediaRepository.observeSeasonal().onEach { r ->
            _uiState.update { state ->
                val sea = r.fold({ items -> RowState.Success(items) }, { e -> RowState.Error(e.message ?: "") })
                val bec = r.fold({ items -> RowState.Success(items.shuffled().take(8)) }, { e -> RowState.Error(e.message ?: "") })
                state.copy(seasonal = sea, becauseYouWatched = bec, becauseYouWatchedSource = r.getOrNull()?.firstOrNull()?.title ?: "")
            }
        }.launchIn(viewModelScope)
    }
}
