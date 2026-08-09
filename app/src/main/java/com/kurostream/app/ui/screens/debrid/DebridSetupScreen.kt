// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.debrid

import androidx.compose.runtime.Composable
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold

@Composable
fun DebridSetupScreen(
    onBack: () -> Unit = {},
) {
    Af3ScreenScaffold(title = "Debrid Service", onBack = onBack) {
        Af3EmptyState(
            icon = "🔗",
            title = "Debrid setup",
            subtitle = "Connect a premium multi-hoster for unrestricted streaming.",
            actionLabel = "Add service",
            onAction = {},
        )
    }
}
