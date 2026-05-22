package com.kurostream.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KuroPrimary = Color(0xFF6C63FF)
val KuroSecondary = Color(0xFF03DAC6)
val KuroBackground = Color(0xFF0A0A0F)
val KuroSurface = Color(0xFF14141E)
val KuroSurfaceVariant = Color(0xFF1E1E2E)
val KuroOnSurface = Color(0xFFEEEEFF)
val KuroOnSurfaceVariant = Color(0xFFB0B0C8)
val KuroAccent = Color(0xFFFF6B6B)
val KuroError = Color(0xFFCF6679)

private val DarkColorScheme = darkColorScheme(
    primary = KuroPrimary,
    secondary = KuroSecondary,
    background = KuroBackground,
    surface = KuroSurface,
    surfaceVariant = KuroSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = KuroOnSurface,
    onSurface = KuroOnSurface,
    onSurfaceVariant = KuroOnSurfaceVariant,
    error = KuroError
)

@Composable
fun KuroStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = KuroTypography,
        content = content
    )
}
