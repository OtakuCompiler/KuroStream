// This file is part of KuroStream.
//
// PremiumThemeProvider — Composable wrapper that selects the correct
// color scheme based on ThemeMode and applies optional glass/cinematic
// blue effects.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PremiumThemeProvider(
    themeMode: ThemeMode,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (themeMode) {
        ThemeMode.LIGHT       -> PremiumThemeTokens.lightScheme
        ThemeMode.DARK        -> PremiumThemeTokens.darkScheme
        ThemeMode.AUTO        -> if (systemDark) PremiumThemeTokens.darkScheme else PremiumThemeTokens.lightScheme
        ThemeMode.AMOLED_BLACK -> PremiumThemeTokens.amoledBlackScheme
        ThemeMode.OLED_CINEMA -> PremiumThemeTokens.oledCinemaScheme
        ThemeMode.CUSTOM      -> PremiumThemeTokens.darkScheme // custom accent handled by CustomThemeEngine
    }
    androidx.tv.material3.MaterialTheme(colorScheme = scheme, content = content)
}

enum class BlueGlowIntensity { LOW, MEDIUM, HIGH }

object PremiumThemeTokens {
    val lightScheme = lightColorScheme(
        primary = Color(0xFF0066FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF0066FF).copy(alpha = 0.12f),
        onPrimaryContainer = Color(0xFF0066FF),
        secondary = Color(0xFF00A8FF),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF00A8FF).copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF00A8FF),
        background = Color(0xFFF5F5F7),
        onBackground = Color(0xFF1C1C1E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF3A3A3C),
    )

    val darkScheme = darkColorScheme(
        primary = Color(0xFF00A8FF),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0066FF).copy(alpha = 0.15f),
        onPrimaryContainer = Color(0xFF00A8FF),
        secondary = Color(0xFF8B5CF6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF8B5CF6).copy(alpha = 0.15f),
        onSecondaryContainer = Color(0xFF8B5CF6),
        background = Color(0xFF0A0A0F),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF14141F),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF1E1E2E),
        onSurfaceVariant = Color(0xFFB0B0C0),
    )

    val amoledBlackScheme = darkColorScheme(
        primary = Color(0xFF00A8FF),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0066FF).copy(alpha = 0.12f),
        onPrimaryContainer = Color(0xFF00A8FF),
        secondary = Color(0xFF8B5CF6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF8B5CF6).copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF8B5CF6),
        background = OledPureBlack,
        onBackground = Color(0xFFE0E0E0),
        surface = OledPureBlack,
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = OledNearBlack,
        onSurfaceVariant = Color(0xFFB0B0C0),
    )

    val oledCinemaScheme = darkColorScheme(
        primary = Color(0xFF00A8FF),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0066FF).copy(alpha = 0.10f),
        onPrimaryContainer = Color(0xFF00A8FF),
        secondary = Color(0xFF00E5FF),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF00B8D4).copy(alpha = 0.10f),
        onSecondaryContainer = Color(0xFF00E5FF),
        background = OledPureBlack,
        onBackground = Color(0xFFE8E8E8),
        surface = OledPureBlack,
        onSurface = Color(0xFFE8E8E8),
        surfaceVariant = OledSurfaceElevated,
        onSurfaceVariant = Color(0xFFC0C0C0),
    )
}
