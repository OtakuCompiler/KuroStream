// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kurostream.app.ui.theme.CustomTheme
import com.kurostream.app.ui.theme.ThemeMode

/**
 * Immutable snapshot of the active Arctic Fuse palette.
 *
 * Consumers call [LocalArcticFusePalette].current to read tokens.
 * The palette is reconstructed from [ThemeMode] + optional [CustomTheme]
 * inside [ArcticFuseTheme] wrapper composable.
 */
@Immutable
data class ArcticFusePalette(
    // Backgrounds
    val bg:               Color = AFBg,
    val bgAlt:            Color = AFBgAlt,
    val bgDeep:           Color = AFBgDeep,
    val sidebar:          Color = AFBgSidebar,
    val surface:          Color = AFSurface,
    val surfaceVariant:   Color = AFSurfaceVariant,
    val surfaceHighlight: Color = AFSurfaceHighlight,
    val surfaceActive:    Color = AFSurfaceActive,
    val overlay:          Color = AFOverlay,
    // Accents
    val cyan:             Color = AFAccentPrimary,
    val teal:             Color = AFAccentSecondary,
    val accent:           Color = AFAccentPrimary,
    val accentSec:        Color = AFAccentSecondary,
    val gold:             Color = AFGold,
    val danger:           Color = AFDanger,
    val success:          Color = AFSuccess,
    val warning:          Color = AFWarning,
    // Text
    val text:             Color = AFText,
    val textSec:          Color = AFTextSec,
    val textDim:          Color = AFTextDim,
    val textMuted:        Color = AFTextMuted,
    // Borders
    val border:           Color = AFBorder,
    val borderStrong:     Color = AFBorderStrong,
    val borderFocus:      Color = AFBorderFocus,
    // Special
    val starGold:         Color = AFStarGold,
    // Flags
    val isLight:          Boolean = false,
    val isOled:           Boolean = false,
) {
    companion object {
        fun forMode(mode: ThemeMode, custom: CustomTheme? = null): ArcticFusePalette = when (mode) {
            ThemeMode.LIGHT -> lightPalette()
            ThemeMode.AMOLED_BLACK -> oledPalette()
            ThemeMode.OLED_CINEMA  -> oledPalette()
            ThemeMode.CUSTOM       -> custom?.let { customPalette(it) } ?: ArcticFusePalette()
            ThemeMode.DARK         -> ArcticFusePalette()
        }

        private fun lightPalette() = ArcticFusePalette(
            bg               = Color(0xFFF0F2FF),
            bgAlt            = Color(0xFFE8EAFF),
            bgDeep           = Color(0xFFFFFFFF),
            sidebar          = Color(0xFFEEF0FF),
            surface          = Color(0xFFFFFFFF),
            surfaceVariant   = Color(0xFFE8EAFF),
            surfaceHighlight = Color(0xFFDDE0FF),
            surfaceActive    = Color(0xFFCDD0FF),
            overlay          = Color(0xCC000000),
            cyan             = Color(0xFF4F46E5),
            teal             = Color(0xFF7C3AED),
            accent           = Color(0xFF4F46E5),
            accentSec        = Color(0xFF7C3AED),
            gold             = Color(0xFFD97706),
            danger           = Color(0xFFDC2626),
            success          = Color(0xFF16A34A),
            warning          = Color(0xFFD97706),
            text             = Color(0xFF111827),
            textSec          = Color(0xFF4B5563),
            textDim          = Color(0xFF6B7280),
            textMuted        = Color(0xFF9CA3AF),
            border           = Color(0xFFE5E7EB),
            borderStrong     = Color(0xFFD1D5DB),
            borderFocus      = Color(0xFF4F46E5),
            starGold         = Color(0xFFD97706),
            isLight          = true,
            isOled           = false,
        )

        private fun oledPalette() = ArcticFusePalette(
            bg               = Color(0xFF000000),
            bgAlt            = Color(0xFF080808),
            bgDeep           = Color(0xFF000000),
            sidebar          = Color(0xFF020202),
            surface          = Color(0xFF0D0D0D),
            surfaceVariant   = Color(0xFF111111),
            surfaceHighlight = Color(0xFF161616),
            surfaceActive    = Color(0xFF1A1A1A),
            overlay          = Color(0xF0000000),
            isOled           = true,
        )

        private fun customPalette(c: CustomTheme) = ArcticFusePalette(
            bg               = c.bgColor(),
            bgAlt            = c.surfaceColor(),
            bgDeep           = lerp(c.bgColor(), Color.Black, 0.4f),
            sidebar          = lerp(c.bgColor(), Color.Black, 0.2f),
            surface          = c.surfaceColor(),
            surfaceVariant   = c.surfaceVarColor(),
            surfaceHighlight = lerp(c.surfaceColor(), c.primaryColor(), 0.08f),
            surfaceActive    = lerp(c.surfaceColor(), c.primaryColor(), 0.14f),
            cyan             = c.primaryColor(),
            teal             = c.secondaryColor(),
            accent           = c.primaryColor(),
            accentSec        = c.secondaryColor(),
            danger           = c.dangerColor(),
            starGold         = c.starColor(),
            text             = c.textColor(),
            textSec          = c.textSecColor(),
            textDim          = lerp(c.textColor(), c.bgColor(), 0.5f),
            textMuted        = lerp(c.textColor(), c.bgColor(), 0.7f),
            border           = c.borderColor(),
            borderStrong     = lerp(c.borderColor(), c.primaryColor(), 0.3f),
            borderFocus      = c.primaryColor(),
        )

        /** Linear interpolation between two [Color]s. */
        private fun lerp(a: Color, b: Color, t: Float): Color = Color(
            red   = a.red   + (b.red   - a.red)   * t,
            green = a.green + (b.green - a.green) * t,
            blue  = a.blue  + (b.blue  - a.blue)  * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t,
        )
    }
}

val LocalArcticFusePalette = staticCompositionLocalOf { ArcticFusePalette() }

/**
 * Provides an [ArcticFusePalette] built from [themeMode] and optional [customTheme]
 * to the composition tree.
 *
 * Usage:
 * ```
 * ArcticFuseTheme(themeMode = ThemeMode.DARK) {
 *     // any composable can call: val p = LocalArcticFusePalette.current
 * }
 * ```
 */
@Composable
fun ArcticFuseTheme(
    themeMode:   ThemeMode  = ThemeMode.DARK,
    customTheme: CustomTheme? = null,
    content: @Composable () -> Unit,
) {
    val palette = ArcticFusePalette.forMode(themeMode, customTheme)
    CompositionLocalProvider(LocalArcticFusePalette provides palette) {
        content()
    }
}
