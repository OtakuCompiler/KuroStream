// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.app.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kurostream.app.ui.components.SidebarNavigation
import com.kurostream.app.ui.arctic.ArcticFuseHomeScreen
import com.kurostream.app.ui.theme.Skin
import com.kurostream.app.ui.theme.TvBackground
import androidx.compose.foundation.background

/**
 * HomeScreen — entry point for the home experience.
 *
 * Uses [ArcticFuseHomeScreen] when the [Skin.ARCTIC_FUSE] skin is active,
 * otherwise renders the standard [SidebarNavigation] + [ModernHomeScreen] layout.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.skin == Skin.ARCTIC_FUSE) {
        ArcticFuseHomeScreen(
            heroItems = uiState.heroItems,
            continueWatching = uiState.continueWatching,
            trending = uiState.trending,
            newReleases = uiState.newReleases,
            seasonal = uiState.seasonal,
            becauseYouWatched = uiState.becauseYouWatched,
            becauseYouWatchedSource = uiState.becauseYouWatchedSource,
            onMediaClick = onMediaClick,
            onPlay = { item -> onMediaClick(item.id) },
            onRetry = { viewModel.retry() },
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            onAddonsClick = onAddonsClick,
            onTorrentsClick = onTorrentsClick,
            onBackupClick = onBackupClick,
            onFavoritesClick = onFavoritesClick,
            onHistoryClick = onHistoryClick,
            onLibraryClick = onLibraryClick,
        )
    } else {
        Row(modifier = Modifier.fillMaxSize().background(TvBackground)) {
            SidebarNavigation(
                selectedItem = "home",
                onItemSelected = { id ->
                    when (id) {
                        "home" -> { /* already on home */ }
                        "search" -> onSearchClick()
                        "settings" -> onSettingsClick()
                        "addons" -> onAddonsClick()
                        "torrents" -> onTorrentsClick()
                        "backup" -> onBackupClick()
                        "favorites" -> onFavoritesClick()
                        "history" -> onHistoryClick()
                        "library" -> onLibraryClick()
                    }
                },
            )

            ModernHomeScreen(
                onMediaClick = onMediaClick,
                onPlayClick = { item -> onMediaClick(item.id) },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onAddonsClick = onAddonsClick,
                onTorrentsClick = onTorrentsClick,
                onBackupClick = onBackupClick,
                viewModel = viewModel,
            )
        }
    }
}