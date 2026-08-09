// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.Af3Backdrop
import com.kurostream.app.ui.components.Af3CardLayout
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3HeroSpotlight
import com.kurostream.app.ui.components.Af3Hub
import com.kurostream.app.ui.components.Af3HubSwitcher
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3WidgetRow
import com.kurostream.app.ui.theme.Af3Theme
import kotlinx.coroutines.delay

private val DefaultHubs = listOf(
    Af3Hub("home", "Home", "⌂"),
    Af3Hub("movies", "Movies", "🎬"),
    Af3Hub("series", "Series", "📺"),
    Af3Hub("anime", "Anime", "🌸"),
    Af3Hub("favorites", "Favorites", "★"),
)

@Composable
fun HomeScreen(
    onMediaClick: (String) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLibraryClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var activeHub by remember { mutableIntStateOf(0) }

    // Auto-focus first widget after layout settles
    LaunchedEffect(uiState.isInitialLoading) {
        if (!uiState.isInitialLoading) {
            delay(400)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Af3Backdrop(backdropUrl = uiState.heroItems.firstOrNull()?.backdropUrl)

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== TOP BAR =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.safeH, vertical = space.s12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "KuroStream",
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.width(space.s24))
                Af3HubSwitcher(
                    hubs = DefaultHubs,
                    activeIndex = activeHub,
                    onHubSelected = { idx ->
                        activeHub = idx
                        when (DefaultHubs[idx].id) {
                            "search" -> onSearchClick()
                            "favorites" -> onFavoritesClick()
                            else -> Unit
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                Af3PillButton("Settings", primary = false, onClick = onSettingsClick)
            }

            if (uiState.isInitialLoading) {
                Af3EmptyState(
                    icon = "⏳",
                    title = "Loading your library…",
                    subtitle = "Fetching the latest trending titles.",
                )
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(firstFocus),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(space.s16),
            ) {
                // ===== HERO SPOTLIGHT =====
                item("hero") {
                    if (uiState.heroItems.isNotEmpty()) {
                        Af3HeroSpotlight(
                            items = uiState.heroItems,
                            onPlay = { onPlayClick(it) },
                            onInfo = { onMediaClick(it.id) },
                            modifier = Modifier.padding(horizontal = space.safeH),
                        )
                    }
                }

                // ===== CONTINUE WATCHING =====
                rowOrEmpty(uiState.continueWatching, "continue_watching", "Continue Watching", Af3CardLayout.Landscape, onMediaClick, uiState, viewModel)

                // ===== TRENDING =====
                rowOrEmpty(uiState.trending, "trending", "Trending Now", Af3CardLayout.Poster, onMediaClick, uiState, viewModel)

                // ===== NEW RELEASES =====
                rowOrEmpty(uiState.newReleases, "new_releases", "New Releases", Af3CardLayout.Landscape, onMediaClick, uiState, viewModel)

                // ===== POPULAR =====
                rowOrEmpty(uiState.popular, "popular", "Popular", Af3CardLayout.Poster, onMediaClick, uiState, viewModel)

                // ===== SEASONAL =====
                rowOrEmpty(uiState.seasonal, "seasonal", "This Season", Af3CardLayout.Poster, onMediaClick, uiState, viewModel)

                // ===== RECOMMENDED =====
                rowOrEmpty(uiState.becauseYouWatched, "recommended", "Recommended for you", Af3CardLayout.Poster, onMediaClick, uiState, viewModel)

                // ===== GENRES =====
                rowOrEmpty(uiState.genres, "genres", "Browse by Genre", Af3CardLayout.Icon, onMediaClick, uiState, viewModel)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rowOrEmpty(
    state: RowState<MediaItem>,
    key: String,
    title: String,
    layout: Af3CardLayout,
    onMediaClick: (String) -> Unit,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
) {
    when (state) {
        is RowState.Success -> if (state.items.isNotEmpty()) {
            item(key) {
                Af3WidgetRow(
                    title = title,
                    items = state.items,
                    layout = layout,
                    onItemClick = { onMediaClick(it.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        is RowState.Error -> Unit
        RowState.Loading -> Unit
    }
}
