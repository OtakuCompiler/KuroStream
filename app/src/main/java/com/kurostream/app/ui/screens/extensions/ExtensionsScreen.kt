// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.extensions

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun ExtensionsScreen(
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Extensions", onBack = onBackClick) {
        Af3EmptyState(
            icon = "🧩",
            title = "No extensions enabled",
            subtitle = "Enable extensions to add metadata providers and subtitle sources.",
        )
    }
}

@Composable
fun ExtensionConfigScreen(
    extensionId: String,
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Extension: $extensionId", onBack = onBackClick) {
        Af3EmptyState(
            icon = "⚙",
            title = "Configure extension",
            subtitle = "Settings for this extension will appear here.",
        )
    }
}
