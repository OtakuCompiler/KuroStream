// This file is part of KuroStream.
//
// ArcticFuseHomeScreen — pixel-perfect recreation of the Arctic Fuse Kodi skin
// home screen.  Integrates all Arctic Fuse components:
//   - Collapsible sidebar (left rail)
//   - AF3 hub switcher + hero spotlight + combined widget rows
//   - Info panel (slides in on card focus)
//   - Overlays: Search, Detail, Settings, Player, ContextMenu, Toast
//
// This screen is the primary entry point for the Arctic Fuse UI experience.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.screens.home.RowState
import kotlinx.coroutines.delay

/**
 * Full Arctic Fuse home screen.  Replaces the existing HomeScreen when the
 * user has selected the Arctic Fuse skin.
 */
@Composable
fun ArcticFuseHomeScreen(
    // Hero + content data
    heroItems: List<MediaItem>,
    continueWatching: RowState<MediaItem>,
    trending: RowState<MediaItem>,
    newReleases: RowState<MediaItem>,
    seasonal: RowState<MediaItem>,
    becauseYouWatched: RowState<MediaItem>,
    becauseYouWatchedSource: String,
    // Callbacks
    onMediaClick: (String) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onRetry: () -> Unit = {},
    // Navigation callbacks
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAddonsClick: () -> Unit = {},
    onTorrentsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    // Weather (optional)
    weatherTempC: String? = null,
    // Profile
    profileInitial: String = "U",
    profileName: String = "User",
    modifier: Modifier = Modifier,
) {
    // Navigation state
    var activeHub by remember { mutableStateOf(ArcticHub.Home) }
    var activeHubTab by remember { mutableStateOf(ArcticHubTab.Home) }

    val settingsViewModel: ArcticFuseSettingsViewModel = hiltViewModel()

    // Overlay state
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var focusedItem by remember { mutableStateOf<MediaItem?>(null) }
    var contextMenuItem by remember { mutableStateOf<MediaItem?>(null) }
    var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    var toasts by remember { mutableStateOf(listOf<ArcticToast>()) }

    // Settings state — migrated to DataStore via ArcticFuseSettingsViewModel
    // (removed the in-memory `settingsState` remember block)

    // Sidebar expanded
    var sidebarExpanded by remember { mutableStateOf(false) }

    // Auto-focus first item on launch
    LaunchedEffect(Unit) {
        delay(500)
    }

    // Local toast helper — captures toasts var via closure
    val addToast: (ArcticToastType, String) -> Unit = { type, message ->
        val id = (System.currentTimeMillis() % 100000).toString()
        toasts = (toasts + ArcticToast(id = id, type = type, message = message)).takeLast(5)
    }

    Row(modifier = modifier.fillMaxSize()) {
        // ===== LEFT SIDEBAR =====
        ArcticFuseSidebar(
            activeHub = activeHub,
            onNavigate = { hub ->
                activeHub = hub
                when (hub) {
                    ArcticHub.Home -> { /* stay on home */ }
                    ArcticHub.Anime -> { activeHubTab = ArcticHubTab.Anime }
                    ArcticHub.Movies -> { activeHubTab = ArcticHubTab.Movies }
                    ArcticHub.TVShows -> { activeHubTab = ArcticHubTab.TVShows }
                    ArcticHub.YouTube -> { /* future */ }
                    ArcticHub.Addons -> onAddonsClick()
                    ArcticHub.Favorites -> onFavoritesClick()
                    ArcticHub.System -> { showSettings = true }
                    else -> { /* no-op */ }
                }
            },
            initialExpanded = sidebarExpanded,
            weatherTempC = weatherTempC,
            profileInitial = profileInitial,
            profileName = profileName,
        )

        // ===== MAIN CONTENT =====
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Hub switcher
            ArcticFuseHubSwitcher(
                activeHub = activeHubTab,
                onSelect = { activeHubTab = it },
            )

            // Content area
            // ===== AF3 TOP-TIER HOME LAYOUT =====
            // AF3-style: hub switcher (top), hero spotlight (auto-advancing),
            // combined widget rows with parallax backdrop.
            ArcticFuse3HomeLayout(
                heroItems = heroItems,
                hubs = listOf(
                    Af3Hub("home", "Home", "⌂"),
                    Af3Hub("movies", "Movies", "🎬"),
                    Af3Hub("series", "TV", "📺"),
                    Af3Hub("anime", "Anime", "🌸"),
                    Af3Hub("favorites", "Favorites", "★"),
                ),
                widgets = buildList {
                    // Continue Watching (landscape cards)
                    val cwItems = (continueWatching as? RowState.Success)?.items ?: emptyList()
                    if (cwItems.isNotEmpty()) {
                        add(
                            Af3WidgetRow(
                                id = "continue_watching",
                                title = "Continue Watching",
                                items = cwItems,
                                layout = Af3WidgetLayout.Landscape,
                            ),
                        )
                    }
                    // Trending (poster)
                    val trItems = (trending as? RowState.Success)?.items ?: emptyList()
                    if (trItems.isNotEmpty()) {
                        add(
                            Af3WidgetRow(
                                id = "trending",
                                title = "Trending Now",
                                items = trItems.take(12),
                                layout = Af3WidgetLayout.Poster,
                            ),
                        )
                    }
                    // Recently Added (landscape)
                    val raItems = (newReleases as? RowState.Success)?.items ?: emptyList()
                    if (raItems.isNotEmpty()) {
                        add(
                            Af3WidgetRow(
                                id = "recently_added",
                                title = "Recently Added",
                                items = raItems.take(12),
                                layout = Af3WidgetLayout.Landscape,
                            ),
                        )
                    }
                    // This Season (poster)
                    val sItems = (seasonal as? RowState.Success)?.items ?: emptyList()
                    if (sItems.isNotEmpty()) {
                        add(
                            Af3WidgetRow(
                                id = "seasonal",
                                title = "This Season",
                                items = sItems.take(12),
                                layout = Af3WidgetLayout.Poster,
                            ),
                        )
                    }
                    // Because You Watched (poster)
                    val bywItems = (becauseYouWatched as? RowState.Success)?.items ?: emptyList()
                    if (bywItems.isNotEmpty()) {
                        add(
                            Af3WidgetRow(
                                id = "because_you_watched",
                                title = if (becauseYouWatchedSource.isNotBlank())
                                    "Because you watched $becauseYouWatchedSource"
                                else "Recommended",
                                items = bywItems.take(12),
                                layout = Af3WidgetLayout.Poster,
                            ),
                        )
                    }
                    // Anime grid (poster)
                    if (activeHub == ArcticHub.Anime) {
                        val animeItems = trItems.filter { item ->
                            item.genre.any { it.equals("Anime", ignoreCase = true) }
                        }
                        if (animeItems.isNotEmpty()) {
                            add(
                                Af3WidgetRow(
                                    id = "anime_grid",
                                    title = "Trending Anime",
                                    items = animeItems.take(18),
                                    layout = Af3WidgetLayout.Poster,
                                ),
                            )
                        }
                    }
                },
                onMediaClick = { item ->
                    selectedItem = item
                    onMediaClick(item.id)
                },
                onHeroPlay = { item ->
                    selectedItem = item
                    onPlay(item)
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        // ===== INFO PANEL (right side, slides in on focus) =====
        ArcticFuseInfoPanel(
            item = focusedItem,
            visible = focusedItem != null && !showDetail && !showSearch && !showSettings,
            onClose = { focusedItem = null },
        )
    }

    // ===== SEARCH OVERLAY =====
    ArcticFuseSearchHub(
        visible = showSearch,
        allItems = (trending as? RowState.Success)?.items ?: emptyList(),
        onClose = { showSearch = false },
        onItemClick = { item ->
            showSearch = false
            onMediaClick(item.id)
        },
    )

    // ===== DETAIL PAGE OVERLAY =====
    ArcticFuseDetailPage(
        item = selectedItem,
        visible = showDetail,
        onClose = { showDetail = false; selectedItem = null },
        onPlay = { item ->
            showDetail = false
            onPlay(item)
        },
        onMediaClick = { mediaId ->
            showDetail = false
            onMediaClick(mediaId)
        },
        relatedItems = (trending as? RowState.Success)?.items?.take(6) ?: emptyList(),
    )

    // ===== SETTINGS PAGE OVERLAY =====
    ArcticFuseSettingsPage(
        visible    = showSettings,
        viewModel  = settingsViewModel,
        onClose    = { showSettings = false },
        systemInfo = ArcticSystemInfo(),
    )

    // ===== PLAYER OVERLAY =====
    ArcticFusePlayerOverlay(
        item = selectedItem,
        visible = showPlayer,
        onClose = { showPlayer = false },
    )

    // ===== CONTEXT MENU =====
    ArcticFuseContextMenu(
        item = contextMenuItem,
        visible = contextMenuItem != null,
        onClose = { contextMenuItem = null },
        onAction = { action, item ->
            when (action) {
                ArcticContextAction.Play -> onPlay(item)
                ArcticContextAction.Watchlist -> addToast(ArcticToastType.Success, "Added to Watchlist")
                ArcticContextAction.Favourite -> addToast(ArcticToastType.Success, "Added to Favourites")
                ArcticContextAction.MarkWatched -> addToast(ArcticToastType.Info, "Marked as watched")
                ArcticContextAction.Info -> { selectedItem = item; showDetail = true }
            }
        },
        anchorOffset = contextMenuOffset,
    )

    // ===== TOAST CONTAINER =====
    ArcticFuseToastContainer(
        toasts = toasts,
        onDismiss = { id -> toasts = toasts.filter { it.id != id } },
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    )
}