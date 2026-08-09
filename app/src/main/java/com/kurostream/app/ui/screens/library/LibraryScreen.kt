// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.library

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun LibraryScreen(
    onMediaClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Library", onBack = onBackClick) {
        Af3EmptyState(
            icon = "📚",
            title = "Your library is empty",
            subtitle = "Add titles from the details page to build your collection.",
        )
    }
}
