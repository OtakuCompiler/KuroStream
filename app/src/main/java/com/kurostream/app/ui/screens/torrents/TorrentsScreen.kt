// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.torrents

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun TorrentsScreen(
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Torrents", onBack = onBackClick) {
        Af3EmptyState(
            icon = "🌀",
            title = "No active torrents",
            subtitle = "Start a stream and your downloads will appear here.",
        )
    }
}
