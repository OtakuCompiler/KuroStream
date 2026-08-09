// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Arctic Fuse 3 design tokens — centralized design system for spacing,
 * elevation, motion timing, focus states and component sizes.
 *
 * Inspired by jurialmunkey's Arctic Fuse 3 Kodi skin: minimalist row-based
 * layout with a hero spotlight, combined widget rows, smooth focus
 * transitions, and ambient gradient backdrops.
 */
@Immutable
data class ArcticFuse3Tokens(
    // Spacing scale (8pt grid, AF3 uses slightly tighter spacing)
    val space2: Dp = 2.dp,
    val space4: Dp = 4.dp,
    val space6: Dp = 6.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space40: Dp = 40.dp,
    val space48: Dp = 48.dp,
    val space64: Dp = 64.dp,

    // Corner radii (AF3 uses very subtle rounding)
    val radiusSmall: Dp = 4.dp,
    val radiusMedium: Dp = 8.dp,
    val radiusLarge: Dp = 12.dp,
    val radiusXLarge: Dp = 16.dp,
    val radiusPoster: Dp = 10.dp,
    val radiusLandscape: Dp = 10.dp,
    val radiusSpotlight: Dp = 14.dp,

    // Component dimensions
    val posterWidth: Dp = 132.dp,
    val posterHeight: Dp = 198.dp,
    val posterFocusScale: Float = 1.08f,
    val landscapeWidth: Dp = 240.dp,
    val landscapeHeight: Dp = 135.dp,
    val landscapeFocusScale: Float = 1.06f,
    val iconCardSize: Dp = 80.dp,
    val hubSwitcherHeight: Dp = 44.dp,

    // Elevation
    val elevationLow: Dp = 2.dp,
    val elevationMid: Dp = 6.dp,
    val elevationHigh: Dp = 12.dp,
    val elevationSpotlight: Dp = 16.dp,

    // Spotlight
    val spotlightHeight: Dp = 460.dp,
    val spotlightCorner: Dp = 14.dp,
    val spotlightOverlayStart: Float = 0.30f,
    val spotlightOverlayMid: Float = 0.70f,
    val spotlightOverlayEnd: Float = 1.00f,

    // Hub switcher
    val hubInset: Dp = 60.dp,
    val hubFocusBorder: Dp = 2.dp,
    val hubPadding: Dp = 14.dp,

    // Motion (in milliseconds)
    val motionFast: Int = 150,
    val motionMedium: Int = 240,
    val motionSlow: Int = 380,
    val motionHero: Int = 600,

    // Focus glow
    val focusGlowRadius: Dp = 24.dp,
    val focusGlowAlpha: Float = 0.55f,
    val focusBorderWidth: Dp = 2.dp,

    // Backdrop gradient
    val backdropBlurAlpha: Float = 0.85f,
    val backdropVignetteAlpha: Float = 0.55f,

    // Parallax
    val parallaxFactor: Float = 0.08f,

    // Ambient overlay tint (a subtle color overlay on hero backdrop)
    val ambientTintAlpha: Float = 0.06f,

    // Letterbox
    val letterboxSize: Dp = 56.dp,

    // Progress bar
    val progressHeight: Dp = 4.dp,
    val progressHeightFocus: Dp = 6.dp,

    // Star rating
    val starSize: Dp = 12.dp,
    val starSizeFocus: Dp = 14.dp,

    // Quality badge
    val qualityBadgeHeight: Dp = 18.dp,

    // Z-layers
    val zSidebar: Float = 10f,
    val zHub: Float = 11f,
    val zBackdrop: Float = 0f,
    val zContent: Float = 5f,
    val zSpotlight: Float = 6f,
    val zOverlay: Float = 100f,
    val zToast: Float = 200f,
    val zContextMenu: Float = 150f,

    // Component-specific tints
    val widgetTitleAccent: Color = Color(0xFF22D3EE), // cyan-400
    val widgetDivider: Color = Color(0x1FFFFFFF),     // 12% white
    val glassOverlay: Color = Color(0x14FFFFFF),     // 8% white
    val glassOverlayStrong: Color = Color(0x26FFFFFF), // 15% white
    val scrimTop: Color = Color(0x80000000),         // 50% top scrim
    val scrimBottom: Color = Color(0xCC000000),      // 80% bottom scrim
    val scrimLeft: Color = Color(0xA6000000),        // 65% left scrim (sidebar side)
)

val LocalArcticFuse3Tokens = staticCompositionLocalOf { ArcticFuse3Tokens() }

/**
 * AF3 palette extension — extra colors used by the AF3 components
 * (kept separate from [ArcticFusePalette] to avoid breaking changes).
 */
@Immutable
data class ArcticFuse3Extras(
    val starGold: Color = Color(0xFFFFD166),
    val hudRed: Color = Color(0xFFEF4444),
    val hudGreen: Color = Color(0xFF22C55E),
    val hudBlue: Color = Color(0xFF3B82F6),
    val hudOrange: Color = Color(0xFFF97316),
    val hudPurple: Color = Color(0xFFA855F7),
    val scrubberTrack: Color = Color(0x33FFFFFF),
    val scrubberBuffered: Color = Color(0x66FFFFFF),
    val pipBorder: Color = Color(0xFF22D3EE),
)

val LocalArcticFuse3Extras = staticCompositionLocalOf { ArcticFuse3Extras() }
