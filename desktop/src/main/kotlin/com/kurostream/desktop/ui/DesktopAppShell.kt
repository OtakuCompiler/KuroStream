package com.kurostream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import com.kurostream.desktop.playback.DesktopPlayerFactory
import com.kurostream.desktop.ui.screens.DesktopHomeScreen
import com.kurostream.desktop.ui.screens.DesktopSearchScreen
import com.kurostream.desktop.ui.screens.DesktopDetailsScreen
import com.kurostream.desktop.ui.screens.DesktopPlayerScreen
import com.kurostream.desktop.ui.screens.DesktopSettingsScreen
import com.kurostream.desktop.ui.screens.DesktopLibraryScreen

private enum class Tab(val label: String) {
    Home("Home"),
    Search("Search"),
    Library("Library"),
    Settings("Settings"),
}

/**
 * Top-level Compose scaffold for the desktop app. Mirrors the Android
 * `TvNavHost` so the navigation feels identical across platforms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAppShell(
    state: DesktopAppState,
    playerFactory: DesktopPlayerFactory,
    onExit: () -> Unit,
) {
    var currentTab by remember { mutableStateOf(Tab.Home) }
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    var nowPlayingId by remember { mutableStateOf<String?>(null) }
    var initialSearchQuery by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Persistent top nav so keyboard users can tab between screens.
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab.values().forEach { tab ->
                NavigationBarItem(
                    selected = currentTab == tab,
                    onClick = { currentTab = tab; selectedMediaId = null },
                    label = { Text(tab.label) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (currentTab) {
                Tab.Home -> DesktopHomeScreen(
                    state = state,
                    onSelectItem = { selectedMediaId = it },
                    onPlay = { nowPlayingId = it; selectedMediaId = null },
                    onSearch = { currentTab = Tab.Search; initialSearchQuery = it },
                )
                Tab.Search -> DesktopSearchScreen(
                    state = state,
                    initialQuery = initialSearchQuery,
                    onSelectItem = { selectedMediaId = it },
                    onPlay = { nowPlayingId = it; selectedMediaId = null },
                )
                Tab.Library -> DesktopLibraryScreen(
                    state = state,
                    onSelectItem = { selectedMediaId = it },
                    onPlay = { nowPlayingId = it; selectedMediaId = null },
                )
                Tab.Settings -> DesktopSettingsScreen(
                    state = state,
                    onExit = onExit,
                )
            }

            val mediaId = selectedMediaId
            if (mediaId != null) {
                DesktopDetailsScreen(
                    state = state,
                    mediaId = mediaId,
                    onPlay = { nowPlayingId = mediaId; selectedMediaId = null },
                    onClose = { selectedMediaId = null },
                )
            }

            val playId = nowPlayingId
            if (playId != null) {
                DesktopPlayerScreen(
                    state = state,
                    mediaId = playId,
                    playerFactory = playerFactory,
                    onClose = { nowPlayingId = null },
                )
            }
        }
    }
}
