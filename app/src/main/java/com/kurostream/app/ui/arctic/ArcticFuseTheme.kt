// This file is part of KuroStream.
//
// Arctic Fuse 3 — color palette, spacing, typography, motion tokens.
// Dark-first indigo/violet design system per the Arctic Fuse 3 spec.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ===== Arctic Fuse 3 Palette =====
// Background tones (spec §3.1)
val AFBgDeep = Color(0xFF0A0A0F)        // --bg-primary — deepest background
val AFBg = Color(0xFF0A0A0F)            // --bg-primary
val AFBgAlt = Color(0xFF12121A)         // --bg-secondary
val AFBgSidebar = Color(0xFF0D0D14)     // --bg-sidebar
val AFOverlay = Color(0xF20A0A0F)       // modal overlay scrim (~0.95 alpha of bg-primary)

// Surface tones — AFSurface = --bg-card, AFSurfaceHighlight = --bg-card-hover
val AFSurface = Color(0xFF16161F)       // --bg-card
val AFSurfaceVariant = Color(0xFF1A1A2E)// track/skeleton base
val AFSurfaceHighlight = Color(0xFF1E1E2D) // --bg-card-hover

// Accents (spec §3.1 — indigo/violet, replacing the old cyan/teal Arctic Fuse look)
val AFCyan = Color(0xFF6366F1)          // --accent-primary (indigo) — primary accent
val AFTeal = Color(0xFF8B5CF6)          // --accent-secondary (violet) — secondary accent
val AFAccentPrimary = AFCyan
val AFAccentSecondary = AFTeal
val AFGold = Color(0xFFFBBF24)          // amber — ratings
val AFDanger = Color(0xFFEF4444)        // --accent-danger
val AFWarning = Color(0xFFFBBF24)

// Text (spec §3.1)
val AFText = Color(0xFFFFFFFF)          // --text-primary
val AFTextSec = Color(0xFF9CA3AF)       // --text-secondary
val AFTextDim = Color(0xFF6B7280)       // --text-muted
val AFTextMuted = Color(0xFF6B7280)     // --text-muted (alias)

// Borders
val AFBorder = Color(0xFF1F1F2E)        // --border-subtle
val AFBorderStrong = Color(0xFF2A2A3D)

// Star-rating amber (single source of truth, spec §6.1 metadata row)
val AFStarGold = Color(0xFFFBBF24)

// ===== Spacing tokens (4dp base grid, mirrors Tailwind scale) =====
object AFSpacing {
    val px1 = 4.dp
    val px2 = 8.dp
    val px3 = 12.dp
    val px4 = 16.dp
    val px5 = 20.dp
    val px6 = 24.dp
    val px8 = 32.dp
    val px10 = 40.dp
    val px12 = 48.dp
    val px16 = 64.dp

    // Safe zone padding around main content
    val safeZoneH = 48.dp
    val safeZoneV = 24.dp
}

// ===== Radii (spec §3.4) =====
object AFRadius {
    val sm = 8.dp    // buttons, badges
    val md = 12.dp   // cards, inputs
    val lg = 16.dp   // modals, large cards
    val xl = 20.dp   // hero images, featured cards
    val pill = 9999.dp
}

// ===== Card sizing mirrors Arctic Fuse dims =====
object AFCardSize {
    // poster (portrait, 2:3 aspect)
    val posterWidth = 160.dp
    val posterHeight = 240.dp

    // landscape (16:9)
    val landscapeWidth = 280.dp
    val landscapeHeight = 158.dp

    // episode (landscape with progress)
    val episodeWidth = 280.dp
    val episodeHeight = 158.dp
}

// ===== Hero sizing (spec §6.1 — TV/Desktop: 70vh, min 500dp, max 700dp) =====
object AFHero {
    val height = 560.dp
    val minHeight = 500.dp
    val maxHeight = 700.dp
    val tabletHeightFraction = 0.5f
    val mobileHeightFraction = 0.45f
}

// ===== Typography =====
// Arctic Fuse uses Inter-like sans-serif. We map to Material3 type scale.
object AFTypo {
    val display = 36.sp
    val title = 24.sp
    val heading = 20.sp
    val section = 18.sp
    val body = 14.sp
    val meta = 12.sp
    val micro = 10.sp
    val tag = 12.sp
    val navLabel = 14.sp
    val clockTime = 16.sp
    val clockDate = 11.sp
    val sectionTitleSpacing = 2.sp
}

// ===== Sidebar sizing (spec §4.2 — 72dp desktop rail) =====
object AFSidebar {
    val collapsedWidth = 72.dp
    val expandedWidth = 200.dp
    val headerHeight = 64.dp
    val weatherHeight = 56.dp
    val navItemHeight = 48.dp
    val profileHeight = 56.dp
}

// ===== Hub switcher =====
object AFHub {
    val height = 56.dp
    val tabLetterSpacing = 2.sp
    val indicatorHeight = 2.dp
}

// ===== Motion durations =====
object AFMotion {
    const val fast = 150
    const val normal = 200
    const val slow = 300
    const val pageEnter = 200
    const val panelEnter = 200
    const val toast = 3000
    const val playerShowHide = 3000
}

// ===== Toast stack cap =====
const val AFMaxToasts = 3
const val AFMaxRecentSearches = 5
