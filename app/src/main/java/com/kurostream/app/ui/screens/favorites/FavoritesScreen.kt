// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.favorites

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun FavoritesScreen(
    onMediaClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Favorites", onBack = onBackClick) {
        Af3EmptyState(
            icon = "★",
            title = "No favorites yet",
            subtitle = "Tap the star on any title to add it here.",
        )
    }
}
