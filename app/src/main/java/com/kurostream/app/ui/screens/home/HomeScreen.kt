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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.Af3Backdrop
import com.kurostream.app.ui.components.Af3CardLayout
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3HeroSpotlight
import com.kurostream.app.ui.components.Af3Hub
import com.kurostream.app.ui.components.Af3HubSwitcher
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3WidgetRow
import com.kurostream.app.ui.theme.Af3AspectRatio
import com.kurostream.app.ui.theme.Af3Theme
import com.kurostream.app.ui.theme.rememberAf3AspectRatio
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
    val aspect = rememberAf3AspectRatio()

    LaunchedEffect(uiState.isInitialLoading) {
        if (!uiState.isInitialLoading) {
            delay(300)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Af3Backdrop(backdropUrl = uiState.heroItems.firstOrNull()?.backdropUrl)

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== TOP BAR (KuroStream logo + hub switcher + settings) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = space.safeH, end = space.safeH, top = space.s12, bottom = space.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "KuroStream",
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.width(space.s24))
                Box(modifier = Modifier.weight(1f)) {
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
                    )
                }
                Spacer(Modifier.width(space.s12))
                Af3PillButton("Settings", primary = false, onClick = onSettingsClick)
            }

            // ===== MAIN CONTENT — never returns early, always renders visible =====
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isInitialLoading -> {
                        Af3EmptyState(
                            icon = "⏳",
                            title = "Loading your library…",
                            subtitle = "Fetching the latest trending titles.",
                        )
                    }
                    else -> {
                        HomeContent(
                            uiState = uiState,
                            aspect = aspect,
                            firstFocus = firstFocus,
                            listState = listState,
                            onMediaClick = onMediaClick,
                            onPlayClick = onPlayClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    aspect: Af3AspectRatio,
    firstFocus: FocusRequester,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMediaClick: (String) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space

    // Compute aspect-aware sizes
    val heroHeight = (aspect.heightFraction * 720).dp.coerceIn(380.dp, 700.dp)
    val heroH = aspect.heroHeight.coerceAtLeast(380.dp)

    val anyItems = uiState.heroItems.isNotEmpty() ||
        rowHasItems(uiState.continueWatching) ||
        rowHasItems(uiState.trending) ||
        rowHasItems(uiState.newReleases) ||
        rowHasItems(uiState.popular) ||
        rowHasItems(uiState.seasonal) ||
        rowHasItems(uiState.becauseYouWatched) ||
        rowHasItems(uiState.genres)

    if (!anyItems) {
        Box(modifier = Modifier.fillMaxSize().padding(space.safeH)) {
            Af3EmptyState(
                icon = "📺",
                title = "Welcome to KuroStream",
                subtitle = "Add a media source in Settings → Add-ons to start streaming.",
                actionLabel = "Open Add-ons",
                onAction = null,
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(firstFocus),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(space.s20),
    ) {
        // ===== HERO SPOTLIGHT =====
        if (uiState.heroItems.isNotEmpty()) {
            item("hero") {
                Af3HeroSpotlight(
                    items = uiState.heroItems,
                    onPlay = { onPlayClick(it) },
                    onInfo = { onMediaClick(it.id) },
                    modifier = Modifier
                        .padding(horizontal = space.safeH)
                        .height(heroH),
                )
            }
        }

        // ===== ROW: Continue Watching =====
        renderRow(uiState.continueWatching, "continue_watching", "Continue Watching", Af3CardLayout.Landscape, onMediaClick)

        // ===== ROW: Trending =====
        renderRow(uiState.trending, "trending", "Trending Now", Af3CardLayout.Poster, onMediaClick)

        // ===== ROW: New Releases =====
        renderRow(uiState.newReleases, "new_releases", "New Releases", Af3CardLayout.Landscape, onMediaClick)

        // ===== ROW: Popular =====
        renderRow(uiState.popular, "popular", "Popular", Af3CardLayout.Poster, onMediaClick)

        // ===== ROW: This Season =====
        renderRow(uiState.seasonal, "seasonal", "This Season", Af3CardLayout.Poster, onMediaClick)

        // ===== ROW: Recommended =====
        renderRow(uiState.becauseYouWatched, "recommended", "Recommended for you", Af3CardLayout.Poster, onMediaClick)

        // ===== ROW: Genres =====
        renderRow(uiState.genres, "genres", "Browse by Genre", Af3CardLayout.Icon, onMediaClick)
    }
}

private fun rowHasItems(state: RowState<MediaItem>): Boolean =
    (state as? RowState.Success)?.items?.isNotEmpty() == true

private fun androidx.compose.foundation.lazy.LazyListScope.renderRow(
    state: RowState<MediaItem>,
    key: String,
    title: String,
    layout: Af3CardLayout,
    onMediaClick: (String) -> Unit,
) {
    val items = (state as? RowState.Success)?.items
    if (!items.isNullOrEmpty()) {
        item(key) {
            Af3WidgetRow(
                title = title,
                items = items,
                layout = layout,
                onItemClick = { onMediaClick(it.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
