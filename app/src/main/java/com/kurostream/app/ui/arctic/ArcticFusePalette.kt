// This file is part of KuroStream.
//
// ArcticFusePalette — CompositionLocal that exposes Arctic Fuse tokens to any
// composable, independent of the Material theme so we can render authentic
// Arctic Fuse styling without retrofitting tv-material3 colors.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Immutable snapshot of the Arctic Fuse palette so child composables can
 * explicitly read the design tokens they need.
 */
data class ArcticFusePalette(
    val bg: androidx.compose.ui.graphics.Color = AFBg,
    val bgAlt: androidx.compose.ui.graphics.Color = AFBgAlt,
    val surface: androidx.compose.ui.graphics.Color = AFSurface,
    val surfaceVariant: androidx.compose.ui.graphics.Color = AFSurfaceVariant,
    val surfaceHighlight: androidx.compose.ui.graphics.Color = AFSurfaceHighlight,
    val overlay: androidx.compose.ui.graphics.Color = AFOverlay,
    val cyan: androidx.compose.ui.graphics.Color = AFCyan,
    val teal: androidx.compose.ui.graphics.Color = AFTeal,
    val starGold: androidx.compose.ui.graphics.Color = AFStarGold,
    val danger: androidx.compose.ui.graphics.Color = AFDanger,
    val text: androidx.compose.ui.graphics.Color = AFText,
    val textSec: androidx.compose.ui.graphics.Color = AFTextSec,
    val textDim: androidx.compose.ui.graphics.Color = AFTextDim,
    val textMuted: androidx.compose.ui.graphics.Color = AFTextMuted,
    val border: androidx.compose.ui.graphics.Color = AFBorder,
    val borderStrong: androidx.compose.ui.graphics.Color = AFBorderStrong,
)

val LocalArcticFusePalette = staticCompositionLocalOf { ArcticFusePalette() }

@Composable
fun ArcticFuseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalArcticFusePalette provides ArcticFusePalette()) {
        content()
    }
}
