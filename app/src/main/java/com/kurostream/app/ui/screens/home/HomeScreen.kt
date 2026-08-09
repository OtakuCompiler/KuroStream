// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.BottomNavItem
import com.kurostream.app.ui.components.CardType
import com.kurostream.app.ui.components.NuvioBottomNav
import com.kurostream.app.ui.components.NuvioCatalogSection
import com.kurostream.app.ui.components.NuvioHeroCard
import com.kurostream.app.ui.components.NuvioTopAppBar
import com.kurostream.app.ui.theme.NuvioTheme
import com.kurostream.app.ui.theme.rememberAf3FormFactor
import com.kurostream.app.ui.theme.Af3FormFactor
import timber.log.Timber

private val DefaultBottomNav = listOf(
    BottomNavItem("home",     "Home",     "⌂",  "●"),
    BottomNavItem("search",   "Search",   "🔍", "🔎"),
    BottomNavItem("library",  "Library",  "☰",  "≡"),
    BottomNavItem("settings", "Settings", "⚙",  "✦"),
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
    val formFactor = rememberAf3FormFactor()
    val isPhone = formFactor == Af3FormFactor.Phone
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        Timber.tag("HomeScreen").i("Composed (formFactor=$formFactor)")
    }

    NuvioTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NuvioTopAppBar(
                    title = "Home",
                    onSearchClick = onSearchClick,
                    onSettingsClick = onSettingsClick,
                )
            },
            bottomBar = {
                if (isPhone) {
                    NuvioBottomNav(
                        items = DefaultBottomNav,
                        currentRoute = "home",
                        onItemClick = { item ->
                            when (item.route) {
                                "home" -> Unit
                                "search" -> onSearchClick()
                                "library" -> onLibraryClick()
                                "settings" -> onSettingsClick()
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    uiState.isInitialLoading && (uiState.heroItems.isEmpty() &&
                        uiState.trending !is RowState.Success &&
                        uiState.popular !is RowState.Success) -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    else -> HomeContentNuvio(
                        uiState = uiState,
                        listState = listState,
                        onMediaClick = onMediaClick,
                        onPlayClick = onPlayClick,
                        isPhone = isPhone,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContentNuvio(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMediaClick: (String) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    isPhone: Boolean,
) {
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
        verticalArrangement = Arrangement.spacedBy(if (isPhone) 4.dp else 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp),
    ) {
        // Hero
        if (uiState.heroItems.isNotEmpty()) {
            item("hero") {
                NuvioHeroCard(
                    item = uiState.heroItems.first(),
                    onPlay = { onPlayClick(uiState.heroItems.first()) },
                    onInfo = { onMediaClick(uiState.heroItems.first().id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Continue Watching
        item("continue_watching") {
            NuvioCatalogSection(
                title = "Continue Watching",
                items = (uiState.continueWatching as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Landscape,
                emptyMessage = "Nothing in progress yet.",
            )
        }

        // Trending
        item("trending") {
            NuvioCatalogSection(
                title = "Trending Now",
                items = (uiState.trending as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Poster,
                emptyMessage = "No trending titles.",
            )
        }

        // New Releases
        item("new_releases") {
            NuvioCatalogSection(
                title = "New Releases",
                items = (uiState.newReleases as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Landscape,
                emptyMessage = "No new releases.",
            )
        }

        // Popular
        item("popular") {
            NuvioCatalogSection(
                title = "Popular",
                items = (uiState.popular as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Poster,
                emptyMessage = "No popular titles.",
            )
        }

        // Seasonal
        item("seasonal") {
            NuvioCatalogSection(
                title = "This Season",
                items = (uiState.seasonal as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Poster,
                emptyMessage = "No seasonal picks.",
            )
        }

        // Recommended
        item("recommended") {
            NuvioCatalogSection(
                title = if (uiState.becauseYouWatchedSource.isNotBlank())
                    "Because you watched ${uiState.becauseYouWatchedSource}"
                else "Recommended for you",
                items = (uiState.becauseYouWatched as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Poster,
                emptyMessage = "Watch something to get recommendations.",
            )
        }

        // Genres
        item("genres") {
            NuvioCatalogSection(
                title = "Browse by Genre",
                items = (uiState.genres as? RowState.Success)?.items.orEmpty(),
                onItemClick = { onMediaClick(it.id) },
                cardType = CardType.Genre,
                emptyMessage = "No genres.",
            )
        }
    }
}
