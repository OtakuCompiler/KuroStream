// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.home

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val isInitialLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: TvRepositories.MediaRepository,
    private val watchProgressRepository: TvRepositories.WatchProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var shuffledCache: List<MediaItem> = emptyList()

    init {
        loadHomeData()
        viewModelScope.launch { mediaRepository.refreshTrending() }
        viewModelScope.launch {
            kotlinx.coroutines.delay(3_000)
            if (_uiState.value.isInitialLoading) {
                Timber.w("Home: forcing isInitialLoading=false after 3s timeout")
                _uiState.value = _uiState.value.copy(isInitialLoading = false)
            }
        }
    }

    fun retry() {
        loadHomeData()
        viewModelScope.launch { mediaRepository.refreshTrending() }
    }

    private fun loadHomeData() {
        loadJob?.cancel()
        loadJob = combine(
            mediaRepository.getMediaItems(),
            watchProgressRepository.getWatchProgress(),
        ) { items, progress -> items to progress }
            .debounce(50)
            .map { (allItems, progressMap) ->
                if (allItems.isEmpty()) {
                    return@map _uiState.value.copy(
                        heroItems = emptyList(),
                        continueWatching = RowState.Success(emptyList()),
                        trending = RowState.Success(emptyList()),
                        newReleases = RowState.Success(emptyList()),
                        seasonal = RowState.Success(emptyList()),
                        becauseYouWatched = RowState.Success(emptyList()),
                        popular = RowState.Success(emptyList()),
                        recentlyAdded = RowState.Success(emptyList()),
                        recommended = RowState.Success(emptyList()),
                        genres = RowState.Success(emptyList()),
                        myList = RowState.Success(emptyList()),
                        isInitialLoading = false,
                    )
                }

                val itemsWithProgress = allItems.map { item ->
                    item.copy(watchProgress = progressMap[item.id] ?: 0L)
                }
                if (shuffledCache.isEmpty() || shuffledCache.size != allItems.size) {
                    shuffledCache = itemsWithProgress.shuffled()
                }
                val watched = itemsWithProgress.filter { (progressMap[it.id] ?: 0L) > 0L }
                val genres = itemsWithProgress.flatMap { it.genre }.distinct().take(12).map { g ->
                    MediaItem(id = "g_$g", title = g, genre = listOf(g))
                }

                HomeUiState(
                    heroItems = itemsWithProgress.take(5),
                    continueWatching = RowState.Success(watched),
                    trending = RowState.Success(shuffledCache.take(12)),
                    newReleases = RowState.Success(itemsWithProgress.takeLast(12).reversed()),
                    seasonal = RowState.Success(
                        itemsWithProgress.filter { item ->
                            item.genre.any { g -> g.equals("Action", true) || g.equals("Fantasy", true) }
                        }.take(10),
                    ),
                    becauseYouWatched = RowState.Success(shuffledCache.drop(12).take(8)),
                    becauseYouWatchedSource = itemsWithProgress.firstOrNull()?.title.orEmpty(),
                    popular = RowState.Success(itemsWithProgress.sortedByDescending { it.rating }.take(12)),
                    recentlyAdded = RowState.Success(itemsWithProgress.takeLast(12).reversed()),
                    recommended = RowState.Success(shuffledCache.drop(20).take(12)),
                    genres = RowState.Success(genres),
                    myList = RowState.Success(emptyList()),
                    isInitialLoading = false,
                )
            }
            .catch { e ->
                Timber.e(e, "Home pipeline error")
                emit(
                    _uiState.value.copy(
                        continueWatching = RowState.Error(e.localizedMessage ?: "Failed"),
                        trending = RowState.Error(e.localizedMessage ?: "Failed"),
                        isInitialLoading = false,
                    ),
                )
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }
}
