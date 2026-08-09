// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nuvio-style Material 3 theme. Replaces the AF3 Kodi-skin mock with a
 * native Android look:
 * - Material 3 ColorScheme
 * - System-followed dark/light
 * - Compact, dense typography tuned for 16dp grid
 * - No backdrop overlays, no overscan safe-zones — just native UI
 */
object NuvioColors {
    val Primary = Color(0xFF7C5CFF)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF2A1F66)
    val OnPrimaryContainer = Color(0xFFE5DEFF)

    val Secondary = Color(0xFF54C5F8)
    val OnSecondary = Color(0xFF003049)
    val SecondaryContainer = Color(0xFF003F5F)
    val OnSecondaryContainer = Color(0xFFCDE5FF)

    val Tertiary = Color(0xFFFF8FB1)
    val OnTertiary = Color(0xFF5C1133)

    val DarkBg = Color(0xFF0E0E10)
    val DarkSurface = Color(0xFF18181B)
    val DarkSurfaceVariant = Color(0xFF27272A)
    val DarkOutline = Color(0xFF3F3F46)
    val DarkTextPrimary = Color(0xFFF4F4F5)
    val DarkTextSecondary = Color(0xFFA1A1AA)
    val DarkTextTertiary = Color(0xFF71717A)

    val LightBg = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF4F4F5)
    val LightOutline = Color(0xFFE4E4E7)
    val LightTextPrimary = Color(0xFF18181B)
    val LightTextSecondary = Color(0xFF52525B)
    val LightTextTertiary = Color(0xFFA1A1AA)

    val Error = Color(0xFFEF4444)
    val OnError = Color(0xFFFFFFFF)
}

private val NuvioDarkColors = darkColorScheme(
    primary = NuvioColors.Primary,
    onPrimary = NuvioColors.OnPrimary,
    primaryContainer = NuvioColors.PrimaryContainer,
    onPrimaryContainer = NuvioColors.OnPrimaryContainer,
    secondary = NuvioColors.Secondary,
    onSecondary = NuvioColors.OnSecondary,
    secondaryContainer = NuvioColors.SecondaryContainer,
    onSecondaryContainer = NuvioColors.OnSecondaryContainer,
    tertiary = NuvioColors.Tertiary,
    onTertiary = NuvioColors.OnTertiary,
    background = NuvioColors.DarkBg,
    onBackground = NuvioColors.DarkTextPrimary,
    surface = NuvioColors.DarkSurface,
    onSurface = NuvioColors.DarkTextPrimary,
    surfaceVariant = NuvioColors.DarkSurfaceVariant,
    onSurfaceVariant = NuvioColors.DarkTextSecondary,
    outline = NuvioColors.DarkOutline,
    error = NuvioColors.Error,
    onError = NuvioColors.OnError,
)

private val NuvioLightColors = lightColorScheme(
    primary = NuvioColors.Primary,
    onPrimary = NuvioColors.OnPrimary,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = NuvioColors.PrimaryContainer,
    secondary = NuvioColors.Secondary,
    onSecondary = NuvioColors.OnSecondary,
    secondaryContainer = Color(0xFFCDE5FF),
    onSecondaryContainer = NuvioColors.SecondaryContainer,
    tertiary = NuvioColors.Tertiary,
    onTertiary = NuvioColors.OnTertiary,
    background = NuvioColors.LightBg,
    onBackground = NuvioColors.LightTextPrimary,
    surface = NuvioColors.LightSurface,
    onSurface = NuvioColors.LightTextPrimary,
    surfaceVariant = NuvioColors.LightSurfaceVariant,
    onSurfaceVariant = NuvioColors.LightTextSecondary,
    outline = NuvioColors.LightOutline,
    error = NuvioColors.Error,
    onError = NuvioColors.OnError,
)

private val NuvioTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    displayMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    displaySmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
)

@Composable
fun NuvioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) NuvioDarkColors else NuvioLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = NuvioTypography,
        content = content,
    )
}
