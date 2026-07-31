// This file is part of KuroStream.
//
// PremiumThemeTokens — cinematic blue + OLED enhancement tokens.
// Extends Arctic Fuse palette with:
//   - AMOLED black layers
//   - Cinematic blue glow
//   - Glass transparency levels
//   - Device-adaptive effect intensity
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ===== OLED / AMOLED =====
val OledPureBlack = Color(0xFF000000)
val OledNearBlack = Color(0xFF050505)
val OledSurfaceElevated = Color(0xFF080808)
val OledSurfaceMid = Color(0xFF0C0C0C)
val OledBorder = Color(0xFF1A1A1A)

// ===== Cinematic Blue =====
val CinematicBluePrimary = Color(0xFF00A8FF)
val CinematicBlueSecondary = Color(0xFF0066FF)
val CinematicBlueGlow = Color(0x4000A8FF)
val CinematicBlueSubtle = Color(0x2000A8FF)

// ===== Glass Effect =====
object GlassTokens {
    val opacityNormal = 0.85f
    val opacityFocused = 0.92f
    val opacitySelected = 0.95f
    val blurRadius = 12.dp
    val borderWidth = 1.dp
    val borderAlpha = 0.15f
    val shadowAlpha = 0.25f
}

// ===== Blue Glow Animation =====
object BlueGlowTokens {
    val focusedBorderWidth = 2.dp
    val glowRadius = 8.dp
    val glowAlpha = 0.35f
    val animationDuration = 200
}

// ===== Device Adaptive =====
object AdaptiveProfile {
    val tvBlurEnabled = false
    val tvMaxGlowAlpha = 0.2f
    val mobileBlurEnabled = true
    val mobileMaxGlowAlpha = 0.4f
    val desktopMaxGlowAlpha = 0.5f
}
