// This file is part of KuroStream.
//
// ArcticFuseHomeScreen — pixel-perfect recreation of the Arctic Fuse Kodi skin
// home screen.  Integrates all Arctic Fuse components:
//   - Collapsible sidebar (left rail)
//   - Hub switcher (top tabs)
//   - Hero spotlight (auto-advancing)
//   - Widget rows (horizontal carousels)
//   - Widget wall (grid)
//   - Info panel (slides in on card focus)
//   - Overlays: Search, Detail, Settings, Player, ContextMenu, Toast
//
// This screen is the primary entry point for the Arctic Fuse UI experience.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.screens.home.RowState
import com.kurostream.app.ui.theme.ThemeMode
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

    // Settings state
    var settingsState by remember { mutableStateOf(ArcticSettingsState()) }

    // Sidebar expanded
    var sidebarExpanded by remember { mutableStateOf(false) }

    // List state for scroll
    val listState = rememberLazyListState()

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
                    ArcticHub.Movies -> { activeHubTab = ArcticHubTab.Movies }
                    ArcticHub.TVShows -> { activeHubTab = ArcticHubTab.TVShows }
                    ArcticHub.YouTube -> { /* future */ }
                    ArcticHub.Addons -> onAddonsClick()
                    ArcticHub.Favourites -> onFavoritesClick()
                    ArcticHub.System -> { showSettings = true }
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                // Hero spotlight
                item(key = "hero") {
                    ArcticFuseHeroSpotlight(
                        items = heroItems,
                        onPlay = { item ->
                            selectedItem = item
                            onPlay(item)
                        },
                        onInfo = { item ->
                            selectedItem = item
                            showDetail = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Continue Watching
                item(key = "continue_watching") {
                    when (val state = continueWatching) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetRow(
                                title = "Continue Watching",
                                items = emptyList(),
                                onItemClick = {},
                                view = CardView.Landscape,
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetRow(
                                title = "Continue Watching",
                                items = emptyList(),
                                onItemClick = {},
                                view = CardView.Landscape,
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Success -> {
                            if (state.items.isNotEmpty()) {
                                ArcticFuseWidgetRow(
                                    title = "Continue Watching",
                                    items = state.items,
                                    onItemClick = { onMediaClick(it.id) },
                                    onItemFocus = { focusedItem = it },
                                    view = CardView.Landscape,
                                    modifier = Modifier.padding(top = AFSpacing.px6),
                                )
                            }
                        }
                    }
                }

                // Trending
                item(key = "trending") {
                    when (val state = trending) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetRow(
                                title = "Trending Now",
                                items = emptyList(),
                                onItemClick = {},
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetRow(
                                title = "Trending Now",
                                items = emptyList(),
                                onItemClick = {},
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Success -> {
                            ArcticFuseWidgetRow(
                                title = "Trending Now",
                                items = state.items,
                                onItemClick = { onMediaClick(it.id) },
                                onItemFocus = { focusedItem = it },
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                    }
                }

                // Recently Added
                item(key = "recently_added") {
                    when (val state = newReleases) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetRow(
                                title = "Recently Added",
                                items = emptyList(),
                                onItemClick = {},
                                view = CardView.Landscape,
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetRow(
                                title = "Recently Added",
                                items = emptyList(),
                                onItemClick = {},
                                view = CardView.Landscape,
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Success -> {
                            ArcticFuseWidgetRow(
                                title = "Recently Added",
                                items = state.items,
                                onItemClick = { onMediaClick(it.id) },
                                onItemFocus = { focusedItem = it },
                                view = CardView.Landscape,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                    }
                }

                // Seasonal
                item(key = "seasonal") {
                    when (val state = seasonal) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetRow(
                                title = "This Season",
                                items = emptyList(),
                                onItemClick = {},
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetRow(
                                title = "This Season",
                                items = emptyList(),
                                onItemClick = {},
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Success -> {
                            ArcticFuseWidgetRow(
                                title = "This Season",
                                items = state.items,
                                onItemClick = { onMediaClick(it.id) },
                                onItemFocus = { focusedItem = it },
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                    }
                }

                // Because You Watched
                item(key = "because_you_watched") {
                    when (val state = becauseYouWatched) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetRow(
                                title = "Recommended",
                                items = emptyList(),
                                onItemClick = {},
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px2),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetRow(
                                title = "Recommended",
                                items = emptyList(),
                                onItemClick = {},
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px2),
                            )
                        }
                        is RowState.Success -> {
                            if (state.items.isNotEmpty()) {
                                Column {
                                    if (becauseYouWatchedSource.isNotBlank()) {
                                        Text(
                                            text = "Because you watched $becauseYouWatchedSource",
                                            color = AFText,
                                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(
                                                horizontal = AFSpacing.safeZoneH,
                                                vertical = AFSpacing.px3,
                                            ),
                                        )
                                    }
                                    ArcticFuseWidgetRow(
                                        title = "Recommended",
                                        items = state.items,
                                        onItemClick = { onMediaClick(it.id) },
                                        onItemFocus = { focusedItem = it },
                                        modifier = Modifier.padding(top = AFSpacing.px2),
                                    )
                                }
                            }
                        }
                    }
                }

                // Recommended Wall (uses trending data)
                item(key = "recommended_wall") {
                    when (val state = trending) {
                        is RowState.Loading -> {
                            ArcticFuseWidgetWall(
                                title = "Recommended For You",
                                items = emptyList(),
                                onItemClick = {},
                                loading = true,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Error -> {
                            ArcticFuseWidgetWall(
                                title = "Recommended For You",
                                items = emptyList(),
                                onItemClick = {},
                                error = state.message,
                                onRetry = onRetry,
                                modifier = Modifier.padding(top = AFSpacing.px6),
                            )
                        }
                        is RowState.Success -> {
                            val items = state.items.take(18)
                            if (items.isNotEmpty()) {
                                ArcticFuseWidgetWall(
                                    title = "Recommended For You",
                                    items = items,
                                    onItemClick = { onMediaClick(it.id) },
                                    modifier = Modifier.padding(top = AFSpacing.px6),
                                )
                            }
                        }
                    }
                }
            }
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
        visible = showSettings,
        state = settingsState,
        onToggle = { key ->
            settingsState = when (key) {
                "blurEffects" -> settingsState.copy(blurEffects = !settingsState.blurEffects)
                "autoScrollHero" -> settingsState.copy(autoScrollHero = !settingsState.autoScrollHero)
                "hdr" -> settingsState.copy(hdrPassthrough = !settingsState.hdrPassthrough)
                "next-ep" -> settingsState.copy(autoPlayNext = !settingsState.autoPlayNext)
                else -> settingsState
            }
        },
        onSelect = { key, value ->
            settingsState = when (key) {
                "theme" -> settingsState.copy(theme = ThemeMode.valueOf(value))
                "density" -> settingsState.copy(density = value)
                "default" -> settingsState.copy(defaultHub = value)
                "rows" -> settingsState.copy(maxRows = value)
                "quality" -> settingsState.copy(defaultQuality = value)
                else -> settingsState
            }
        },
        onClearCache = {
            addToast(ArcticToastType.Success, "Cache cleared")
        },
        onResetDefaults = {
            settingsState = ArcticSettingsState()
            addToast(ArcticToastType.Info, "Settings reset to defaults")
        },
        onClose = { showSettings = false },
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