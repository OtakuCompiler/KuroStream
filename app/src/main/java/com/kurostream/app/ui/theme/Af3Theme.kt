// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

/**
 * AF3 aspect ratio presets matching jurialmunkey's Arctic Fuse 3.
 * - 16:9   default TV (1920×1080, 3840×2160)
 * - 16:10  tablets
 * - 4:3    older TVs
 * - 3:2    iPad
 * - 17:9/18:9/19.5:9/21:9  ultrawide
 *
 * Each preset defines:
 * - heightFraction: hero height as fraction of screen height
 * - heroHeight: explicit hero Dp
 * - posterScale: multiplier on poster card width/height
 * - safeH: horizontal safe zone for TV overscan
 * - rowGap: vertical spacing between widget rows
 */
@Immutable
data class Af3AspectRatio(
    val name: String,
    val heightFraction: Float,
    val heroHeight: Dp,
    val posterScale: Float,
    val landscapeScale: Float,
    val safeH: Dp,
    val rowGap: Dp,
    val widgetTitleSize: Float,
) {
    companion object {
        // 16:9 — default TV
        val Widescreen = Af3AspectRatio(
            name = "16:9",
            heightFraction = 0.55f,
            heroHeight = 460.dp,
            posterScale = 1f,
            landscapeScale = 1f,
            safeH = 48.dp,
            rowGap = 20.dp,
            widgetTitleSize = 16f,
        )
        // 4:3 — older TVs (1024×768, 1440×1080)
        val Standard = Af3AspectRatio(
            name = "4:3",
            heightFraction = 0.50f,
            heroHeight = 420.dp,
            posterScale = 0.92f,
            landscapeScale = 0.95f,
            safeH = 24.dp,
            rowGap = 16.dp,
            widgetTitleSize = 15f,
        )
        // 16:10 — tablets, laptops
        val Widescreen16x10 = Af3AspectRatio(
            name = "16:10",
            heightFraction = 0.53f,
            heroHeight = 440.dp,
            posterScale = 0.96f,
            landscapeScale = 0.98f,
            safeH = 32.dp,
            rowGap = 18.dp,
            widgetTitleSize = 15f,
        )
        // 21:9 — ultrawide
        val Ultrawide = Af3AspectRatio(
            name = "21:9",
            heightFraction = 0.62f,
            heroHeight = 520.dp,
            posterScale = 1.05f,
            landscapeScale = 1.08f,
            safeH = 80.dp,
            rowGap = 24.dp,
            widgetTitleSize = 18f,
        )
        // 3:2 — iPad-like
        val ThreeByTwo = Af3AspectRatio(
            name = "3:2",
            heightFraction = 0.50f,
            heroHeight = 420.dp,
            posterScale = 0.94f,
            landscapeScale = 0.97f,
            safeH = 28.dp,
            rowGap = 18.dp,
            widgetTitleSize = 15f,
        )
        // 17:9 — modern phones
        val Phone17x9 = Af3AspectRatio(
            name = "17:9",
            heightFraction = 0.45f,
            heroHeight = 380.dp,
            posterScale = 0.95f,
            landscapeScale = 0.95f,
            safeH = 16.dp,
            rowGap = 16.dp,
            widgetTitleSize = 14f,
        )
    }
}

val LocalAf3AspectRatio = staticCompositionLocalOf { Af3AspectRatio.Widescreen }

/**
 * Form-factor detection. AF3 has separate phone vs TV layouts.
 * - Phone: screenWidthDp < 600dp OR touch screen without TV leanback
 * - Tablet: 600dp ≤ screenWidthDp < 840dp
 * - TV: ≥ 840dp OR android.ui.mode.tv
 */
enum class Af3FormFactor { Phone, Tablet, Tv }

@Composable
fun rememberAf3FormFactor(): Af3FormFactor {
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp
    return remember(w) {
        when {
            w >= 840 -> Af3FormFactor.Tv
            w >= 600 -> Af3FormFactor.Tablet
            else -> Af3FormFactor.Phone
        }
    }
}

/**
 * Detect the current screen aspect ratio and return the matching AF3 preset.
 * Snap-to-grid: ratios within 0.05 of a preset bucket are matched.
 */
@Composable
fun rememberAf3AspectRatio(): Af3AspectRatio {
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp.toFloat().coerceAtLeast(1f)
    val h = cfg.screenHeightDp.toFloat().coerceAtLeast(1f)
    val ratio = w / h
    return remember(ratio) {
        when {
            ratio < 1.50f -> Af3AspectRatio.Standard         // 4:3 ~ 1.33
            ratio < 1.65f -> Af3AspectRatio.ThreeByTwo      // 3:2 = 1.50
            ratio < 1.78f -> Af3AspectRatio.Widescreen16x10 // 16:10 ~ 1.60
            ratio < 1.90f -> Af3AspectRatio.Widescreen      // 16:9 ~ 1.78
            ratio < 2.00f -> Af3AspectRatio.Phone17x9       // 17:9 ~ 1.89
            else -> Af3AspectRatio.Ultrawide                // 21:9 ~ 2.33
        }
    }
}

@Immutable
data class Af3Tokens(
    val palette: Af3Palette,
    val space: Af3Spacing = Af3Spacing(),
    val motion: Af3Motion = Af3Motion(),
    val size: Af3Size = Af3Size(),
    val aspect: Af3AspectRatio = Af3AspectRatio.Widescreen,
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
    val aspect: Af3AspectRatio
        @Composable @ReadOnlyComposable get() = LocalAf3Tokens.current.aspect
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
    val aspect = rememberAf3AspectRatio()
    val tokens = Af3Tokens(palette = palette, aspect = aspect)
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
