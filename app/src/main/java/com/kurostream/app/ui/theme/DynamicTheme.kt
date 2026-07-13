package com.kurostream.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicColorPalette(
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color, val surfaceVariant: Color, val onSurfaceVariant: Color,
)

class PosterColorExtractor {
    suspend fun extractColors(bitmap: Bitmap): DynamicColorPalette = withContext(Dispatchers.Default) {
        val p = Palette.from(bitmap).generate()
        val d = p.dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF7C4DFF)
        val v = p.vibrantSwatch?.rgb?.let { Color(it) } ?: d
        val m = p.mutedSwatch?.rgb?.let { Color(it) } ?: Color(0xFF1E1E2E)
        DynamicColorPalette(
            primary = d, onPrimary = if (ColorUtils.calculateLuminance(d.toArgb()) > 0.5) Color.Black else Color.White,
            primaryContainer = d.copy(alpha = 0.15f), onPrimaryContainer = d,
            secondary = v, onSecondary = if (ColorUtils.calculateLuminance(v.toArgb()) > 0.5) Color.Black else Color.White,
            secondaryContainer = v.copy(alpha = 0.15f), onSecondaryContainer = v,
            tertiary = m, onTertiary = if (ColorUtils.calculateLuminance(m.toArgb()) > 0.5) Color.Black else Color.White,
            tertiaryContainer = m.copy(alpha = 0.15f), onTertiaryContainer = m,
            background = TvBackground, onBackground = TvOnBackground, surface = TvSurface,
            onSurface = TvOnSurface, surfaceVariant = TvSurfaceVariant, onSurfaceVariant = TvOnSurfaceVariant,
        )
    }
}

@Composable
fun rememberDynamicTheme(
    imageUrl: String?,
    defaultPalette: DynamicColorPalette = TvDarkColorScheme.toDynamicPalette(),
    imageLoader: ImageLoader? = null,
): DynamicColorPalette {
    var palette by remember { mutableStateOf<DynamicColorPalette?>(null) }
    if (imageUrl != null) {
        val ctx = LocalContext.current
        val loader = imageLoader ?: remember { ImageLoader.Builder(ctx).build() }
        LaunchedEffect(imageUrl) {
            val r = loader.execute(ImageRequest.Builder(ctx).data(imageUrl).allowHardware(false).build())
            if (r is SuccessResult) palette = PosterColorExtractor().extractColors(r.drawable.bitmap)
        }
    }
    val t = palette ?: defaultPalette
    return t.copy(
        primary = animateColorAsState(t.primary, tween(600), "aP").value,
        secondary = animateColorAsState(t.secondary, tween(600), "aS").value,
        tertiary = animateColorAsState(t.tertiary, tween(600), "aT").value,
        background = animateColorAsState(t.background, tween(600), "aB").value,
        surface = animateColorAsState(t.surface, tween(600), "aSu").value,
    )
}

fun androidx.tv.material3.ColorScheme.toDynamicPalette() = DynamicColorPalette(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary, background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface, surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
)

val LocalDynamicPalette = staticCompositionLocalOf { TvDarkColorScheme.toDynamicPalette() }
val LocalAmoledMode = staticCompositionLocalOf { false }

@Composable
fun DynamicThemeProvider(palette: DynamicColorPalette, isAmoled: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDynamicPalette provides palette, LocalAmoledMode provides isAmoled, content = content)
}

fun generateAmoledBlackScheme() = androidx.tv.material3.darkColorScheme(
    primary = Color(0xFF7C4DFF), onPrimary = Color.White, primaryContainer = Color(0xFF651FFF).copy(alpha = 0.12f), onPrimaryContainer = Color(0xFF7C4DFF),
    secondary = Color(0xFF00E5FF), onSecondary = Color.Black, secondaryContainer = Color(0xFF00B8D4).copy(alpha = 0.12f), onSecondaryContainer = Color(0xFF00E5FF),
    tertiary = Color(0xFF00E5FF), onTertiary = Color.Black, tertiaryContainer = Color(0xFF00B8D4).copy(alpha = 0.12f), onTertiaryContainer = Color(0xFF00E5FF),
    background = Color.Black, onBackground = Color(0xFFE0E0E0), surface = Color.Black, onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF0A0A0A), onSurfaceVariant = Color(0xFFB0B0C0),
    error = Color(0xFFCF6679), onError = Color.White, errorContainer = Color(0xFFCF6679).copy(alpha = 0.12f), onErrorContainer = Color(0xFFCF6679),
    outline = Color(0xFF7C4DFF).copy(alpha = 0.3f), outlineVariant = Color(0xFF7C4DFF).copy(alpha = 0.12f),
)

fun generateScheduledTheme(isNight: Boolean, useAmoled: Boolean = false) = when {
    useAmoled -> generateAmoledBlackScheme()
    else -> TvDarkColorScheme
}
