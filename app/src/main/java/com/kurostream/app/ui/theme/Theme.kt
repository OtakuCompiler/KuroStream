package com.kurostream.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = TvPrimary, onPrimary = TvOnPrimary, primaryContainer = TvPrimary.copy(alpha = 0.15f), onPrimaryContainer = TvPrimary,
    secondary = TvSecondary, onSecondary = TvOnSecondary, secondaryContainer = TvSecondary.copy(alpha = 0.15f), onSecondaryContainer = TvSecondary,
    tertiary = TvSecondaryVariant, onTertiary = TvOnPrimary, tertiaryContainer = TvSecondaryVariant.copy(alpha = 0.15f), onTertiaryContainer = TvSecondaryVariant,
    background = TvBackground, onBackground = TvOnBackground, surface = TvSurface, onSurface = TvOnSurface,
    surfaceVariant = TvSurfaceVariant, onSurfaceVariant = TvOnSurfaceVariant, error = TvError, onError = TvOnPrimary,
    errorContainer = TvError.copy(alpha = 0.15f), onErrorContainer = TvError,
    border = TvFocusBorder.copy(alpha = 0.3f), borderVariant = TvFocusBorder.copy(alpha = 0.15f),
)

private val AmoledScheme = generateAmoledBlackScheme()
private val TvShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AnimeStreamTVTheme(content: @Composable () -> Unit) {
    val dp = LocalDynamicPalette.current
    val base = if (LocalAmoledMode.current) AmoledScheme else TvDarkColorScheme
    val scheme = base.copy(
        primary = animateColorAsState(dp.primary, tween(600), "tP").value,
        onPrimary = dp.onPrimary,
        primaryContainer = animateColorAsState(dp.primary, tween(600), "tPC").value.copy(alpha = 0.15f),
        onPrimaryContainer = dp.primary,
        secondary = animateColorAsState(dp.secondary, tween(600), "tS").value,
        onSecondary = dp.onSecondary,
        secondaryContainer = animateColorAsState(dp.secondary, tween(600), "tSC").value.copy(alpha = 0.15f),
        onSecondaryContainer = dp.secondary,
        tertiary = animateColorAsState(dp.tertiary, tween(600), "tT").value,
        onTertiary = dp.onTertiary,
        tertiaryContainer = animateColorAsState(dp.tertiary, tween(600), "tTC").value.copy(alpha = 0.15f),
        onTertiaryContainer = dp.tertiary,
        background = animateColorAsState(dp.background, tween(600), "tB").value,
        onBackground = dp.onBackground,
        surface = animateColorAsState(dp.surface, tween(600), "tSu").value,
        onSurface = dp.onSurface,
        surfaceVariant = dp.surfaceVariant, onSurfaceVariant = dp.onSurfaceVariant,
    )
    MaterialTheme(colorScheme = scheme, shapes = TvShapes, typography = TvTypography, content = content)
}

@Composable
fun focusedCardBorder(): CardBorder = CardDefaults.border(
    focusedBorder = Border(border = BorderStroke(2.dp, TvFocusBorder), shape = TvShapes.medium),
)
