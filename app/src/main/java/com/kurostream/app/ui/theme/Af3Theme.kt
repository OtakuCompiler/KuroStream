// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography

// =============================================================================
// AF3 design tokens — single source of truth for the entire app.
// =============================================================================

@Immutable
data class Af3Palette(
    val bg: Color,
    val bgAlt: Color,
    val bgDeep: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceHighlight: Color,
    val surfaceActive: Color,
    val overlay: Color,
    val accent: Color,
    val accentSec: Color,
    val gold: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val text: Color,
    val textSec: Color,
    val textDim: Color,
    val border: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val isLight: Boolean = false,
)

object Af3Palettes {
    val Dark = Af3Palette(
        bg = Color(0xFF0A0A0F),
        bgAlt = Color(0xFF12121A),
        bgDeep = Color(0xFF07070E),
        surface = Color(0xFF16161F),
        surfaceVariant = Color(0xFF1A1A2E),
        surfaceHighlight = Color(0xFF1E1E2D),
        surfaceActive = Color(0xFF23233A),
        overlay = Color(0xF20A0A0F),
        accent = Color(0xFF6366F1),
        accentSec = Color(0xFF8B5CF6),
        gold = Color(0xFFFBBF24),
        danger = Color(0xFFEF4444),
        success = Color(0xFF22C55E),
        warning = Color(0xFFF59E0B),
        text = Color(0xFFFFFFFF),
        textSec = Color(0xFF9CA3AF),
        textDim = Color(0xFF6B7280),
        border = Color(0xFF1F1F2E),
        borderStrong = Color(0xFF2A2A3D),
        borderFocus = Color(0xFF6366F1),
    )
    val Light = Af3Palette(
        bg = Color(0xFFF0F2FF),
        bgAlt = Color(0xFFE8EAFF),
        bgDeep = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8EAFF),
        surfaceHighlight = Color(0xFFDDE0FF),
        surfaceActive = Color(0xFFCDD0FF),
        overlay = Color(0xCC000000),
        accent = Color(0xFF4F46E5),
        accentSec = Color(0xFF7C3AED),
        gold = Color(0xFFD97706),
        danger = Color(0xFFDC2626),
        success = Color(0xFF16A34A),
        warning = Color(0xFFD97706),
        text = Color(0xFF111827),
        textSec = Color(0xFF4B5563),
        textDim = Color(0xFF6B7280),
        border = Color(0xFFE5E7EB),
        borderStrong = Color(0xFFD1D5DB),
        borderFocus = Color(0xFF4F46E5),
        isLight = true,
    )
}

@Immutable
data class Af3Spacing(
    val s2: Dp = 2.dp,
    val s4: Dp = 4.dp,
    val s6: Dp = 6.dp,
    val s8: Dp = 8.dp,
    val s12: Dp = 12.dp,
    val s16: Dp = 16.dp,
    val s20: Dp = 20.dp,
    val s24: Dp = 24.dp,
    val s32: Dp = 32.dp,
    val s48: Dp = 48.dp,
    val s64: Dp = 64.dp,
    val safeH: Dp = 48.dp,
    val safeV: Dp = 24.dp,
)

@Immutable
data class Af3Motion(
    val fastMs: Int = 150,
    val medMs: Int = 240,
    val slowMs: Int = 380,
    val heroMs: Int = 600,
    val heroAutoMs: Long = 7_000L,
    val toastMs: Long = 3_000L,
)

@Immutable
data class Af3Size(
    val posterW: Dp = 132.dp,
    val posterH: Dp = 198.dp,
    val posterFocusScale: Float = 1.08f,
    val landscapeW: Dp = 240.dp,
    val landscapeH: Dp = 135.dp,
    val landscapeFocusScale: Float = 1.06f,
    val iconW: Dp = 160.dp,
    val iconH: Dp = 80.dp,
    val heroH: Dp = 460.dp,
    val hubH: Dp = 44.dp,
    val cardRadius: Dp = 10.dp,
    val heroRadius: Dp = 14.dp,
    val hubRadius: Dp = 50.dp,
)

@Immutable
data class Af3Tokens(
    val palette: Af3Palette,
    val space: Af3Spacing = Af3Spacing(),
    val motion: Af3Motion = Af3Motion(),
    val size: Af3Size = Af3Size(),
)

val LocalAf3Tokens = staticCompositionLocalOf { Af3Tokens(Af3Palettes.Dark) }

object Af3Theme {
    val palette: Af3Palette
        @Composable @ReadOnlyComposable get() = LocalAf3Tokens.current.palette
    val space: Af3Spacing
        @Composable @ReadOnlyComposable get() = LocalAf3Tokens.current.space
    val motion: Af3Motion
        @Composable @ReadOnlyComposable get() = LocalAf3Tokens.current.motion
    val size: Af3Size
        @Composable @ReadOnlyComposable get() = LocalAf3Tokens.current.size
}

// =============================================================================
// AF3 typography
// =============================================================================

private val af3Typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp),
)

private val af3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

enum class Af3ThemeMode { System, Dark, Light }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Af3Theme(
    mode: Af3ThemeMode = Af3ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val isDark = when (mode) {
        Af3ThemeMode.System -> isSystemInDarkTheme()
        Af3ThemeMode.Dark -> true
        Af3ThemeMode.Light -> false
    }
    val palette = if (isDark) Af3Palettes.Dark else Af3Palettes.Light
    val tokens = Af3Tokens(palette = palette)
    val scheme = androidx.tv.material3.darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.bgDeep,
        primaryContainer = palette.accent.copy(alpha = 0.18f),
        onPrimaryContainer = palette.accent,
        secondary = palette.accentSec,
        onSecondary = palette.bgDeep,
        background = palette.bg,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.textSec,
        border = palette.border,
        error = palette.danger,
    )
    CompositionLocalProvider(LocalAf3Tokens provides tokens) {
        MaterialTheme(colorScheme = scheme, shapes = af3Shapes, typography = af3Typography) {
            content()
        }
    }
}
