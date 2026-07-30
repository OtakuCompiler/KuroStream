package com.kurostream.app.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.model.MediaItem
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import com.kurostream.app.ui.theme.Skin
import com.kurostream.app.repository.SettingsRepositoryAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@Immutable
sealed interface RowState<out T> {
    @Immutable data object Loading : RowState<Nothing>
    @Immutable data class Success<T>(val items: List<T>) : RowState<T>
    @Immutable data class Error(val message: String) : RowState<Nothing>
}

@Immutable
data class HomeUiState(
    val heroItems: List<MediaItem> = emptyList(),
    val continueWatching: RowState<MediaItem> = RowState.Loading,
    val trending: RowState<MediaItem> = RowState.Loading,
    val newReleases: RowState<MediaItem> = RowState.Loading,
    val seasonal: RowState<MediaItem> = RowState.Loading,
    val becauseYouWatched: RowState<MediaItem> = RowState.Loading,
    val becauseYouWatchedSource: String = "",
    val popular: RowState<MediaItem> = RowState.Loading,
    val recentlyAdded: RowState<MediaItem> = RowState.Loading,
    val recommended: RowState<MediaItem> = RowState.Loading,
    val genres: RowState<MediaItem> = RowState.Loading,
    val myList: RowState<MediaItem> = RowState.Loading,
    val skin: Skin = Skin.ARCTIC_FUSE,
    val reduceMotionEnabled: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val settingsRepository: SettingsRepositoryAdapter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Read persisted skin preference and start data loading
        viewModelScope.launch {
            val savedSkinName = settingsRepository.getSettings().skinName
            val skin = Skin.entries.find { it.name.equals(savedSkinName, ignoreCase = true) }
                ?: Skin.ARCTIC_FUSE
            _uiState.value = _uiState.value.copy(skin = skin)
        }
        loadHomeData()
    }

    /**
     * Retry loading home data. Cancels any in-flight load and starts fresh.
     */
    fun retry() {
        loadHomeData()
    }

    private fun loadHomeData() {
        loadJob?.cancel()
        combine(
            mediaRepository.getMediaItems(),
            watchProgressRepository.getWatchProgress(),
            mediaRepository.isRefreshing,
            mediaRepository.refreshError,
        ) { allItems, progressMap, isRefreshing, refreshError ->
            // ── Empty / loading / error detection ──
            if (allItems.isEmpty()) {
                // DB is empty — determine whether to show Loading or Error
                if (isRefreshing) {
                    // Refresh is in progress — keep items empty; the map below
                    // will preserve the default Loading state
                    return@combine Pair(emptyList<MediaItem>(), progressMap)
                }
                if (refreshError != null) {
                    // Refresh finished with error — throw so .catch handles it
                    throw IllegalStateException(refreshError)
                }
                // Initial state — refresh hasn't started yet or completed with
                // empty data. Treat as loading since refresh triggers immediately.
                return@combine Pair(emptyList<MediaItem>(), progressMap)
            }

            // We have real data — attach watch progress
            val itemsWithProgress = allItems.map { item ->
                item.copy(
                    watchProgress = progressMap[item.id] ?: 0L,
                    isFavorite = false,
                )
            }
            return@combine Pair(itemsWithProgress, progressMap)
        }
            .debounce(100)
            .map { (allItems, progressMap) ->
                // ── Empty / loading detection ──
                if (allItems.isEmpty()) {
                    // No data in DB — keep all rows as Loading (shimmer skeletons)
                    // The error case is handled by .catch below
                    return@map HomeUiState(
                        skin = _uiState.value.skin,
                        reduceMotionEnabled = _uiState.value.reduceMotionEnabled,
                    )
                }

                // ── Real data — populate rows ──
                val watchedItems = allItems.filter { (progressMap[it.id] ?: 0L) > 0L }

                // Genres from all items
                val genres = allItems.flatMap { it.genre }.distinct().take(12).map { genre ->
                    MediaItem(
                        id = "genre_$genre",
                        title = genre,
                        description = "",
                        posterUrl = "",
                        backdropUrl = "",
                        genre = listOf(genre),
                    )
                }

                // Shuffle once and cache results through the snapshot
                val shuffled = allItems.shuffled()
                val sortedByRating = allItems.sortedByDescending { it.rating }

                HomeUiState(
                    heroItems = allItems.take(5),
                    continueWatching = if (watchedItems.isNotEmpty()) RowState.Success(watchedItems) else RowState.Success(emptyList()),
                    trending = RowState.Success(shuffled.take(12)),
                    newReleases = RowState.Success(allItems.takeLast(12).reversed()),
                    seasonal = RowState.Success(allItems.filter { item -> item.genre.any { g -> g.equals("Action", ignoreCase = true) || g.equals("Fantasy", ignoreCase = true) } }.take(10)),
                    becauseYouWatched = RowState.Success(shuffled.drop(12).take(8)),
                    becauseYouWatchedSource = allItems.firstOrNull()?.title ?: "",
                    popular = RowState.Success(sortedByRating.take(12)),
                    recentlyAdded = RowState.Success(allItems.takeLast(12).reversed()),
                    recommended = RowState.Success(shuffled.drop(20).take(12)),
                    genres = RowState.Success(genres),
                    myList = RowState.Success(emptyList()), // Favorites tracked separately
                    skin = _uiState.value.skin,
                    reduceMotionEnabled = _uiState.value.reduceMotionEnabled,
                )
            }
            .catch { e ->
                // Flow error — emit error state so skeletons are replaced with errors
                Timber.e(e, "Home data pipeline error")
                val message = e.localizedMessage ?: "Unknown error"
                emit(HomeUiState(
                    trending = RowState.Error("Failed to load trending: $message"),
                    continueWatching = RowState.Error("Failed to load continue watching"),
                    newReleases = RowState.Error("Failed to load new releases"),
                    seasonal = RowState.Error("Failed to load seasonal"),
                    becauseYouWatched = RowState.Error("Failed to load recommendations"),
                    popular = RowState.Error("Failed to load popular"),
                    recentlyAdded = RowState.Error("Failed to load recently added"),
                    recommended = RowState.Error("Failed to load recommendations"),
                    genres = RowState.Error("Failed to load genres"),
                    skin = _uiState.value.skin,
                    reduceMotionEnabled = _uiState.value.reduceMotionEnabled,
                ))
            }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
            .also { loadJob = it }
    }
}