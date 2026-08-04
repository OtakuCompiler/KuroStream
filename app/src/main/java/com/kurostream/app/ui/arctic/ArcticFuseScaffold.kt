// This file is part of KuroStream.
//
// ArcticFuseScaffold — top-level layout matching Arctic Fuse 3 skin:
//   - Full-screen fanart background with 15% black overlay
//   - Top horizontal hub switcher (Movies, TV, Anime, Settings, etc.)
//   - Vertical widget rows below
//   - Spotlight hero at top (collapsible)
//   - Glassmorphism cards (not solid surfaces)
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArcticFuseScaffold(
    currentHub: ArcticHubTab,
    onHubChange: (ArcticHubTab) -> Unit,
    hubs: List<ArcticHubTab> = ArcticHubTab.values().toList(),
    spotlightContent: @Composable () -> Unit,
    widgetRows: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        DynamicFanartBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            ArcticFuseHubSwitcher(
                activeHub = currentHub,
                onSelect = onHubChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            )

            spotlightContent()

            widgetRows()
        }
    }
}
