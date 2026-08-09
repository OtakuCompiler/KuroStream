// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.history

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun HistoryScreen(
    onMediaClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "History", onBack = onBackClick) {
        Af3EmptyState(
            icon = "⏱",
            title = "No watch history",
            subtitle = "Titles you watch will appear here.",
        )
    }
}
