package com.kurostream.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
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
        // Simple average color extraction (avoids Palette dependency)
        val avgColor = averageColor(bitmap)
        val accentColor = dominantColor(bitmap) ?: avgColor
        val bgColor = Color(0xFF1E1E2E)
        DynamicColorPalette(
            primary = accentColor,
            onPrimary = if (ColorUtils.calculateLuminance(accentColor.toArgb()) > 0.5) Color.Black else Color.White,
            primaryContainer = accentColor.copy(alpha = 0.15f),
            onPrimaryContainer = accentColor,
            secondary = avgColor,
            onSecondary = if (ColorUtils.calculateLuminance(avgColor.toArgb()) > 0.5) Color.Black else Color.White,
            secondaryContainer = avgColor.copy(alpha = 0.15f),
            onSecondaryContainer = avgColor,
            tertiary = bgColor,
            onTertiary = if (ColorUtils.calculateLuminance(bgColor.toArgb()) > 0.5) Color.Black else Color.White,
            background = TvBackground,
            onBackground = TvOnBackground,
            surface = TvSurface,
            onSurface = TvOnSurface,
            surfaceVariant = TvSurfaceVariant,
            onSurfaceVariant = TvOnSurfaceVariant,
        )
    }

    private fun averageColor(bitmap: Bitmap): Color {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        val count = minOf(bitmap.width * bitmap.height, 2500)
        val step = maxOf(1, (bitmap.width * bitmap.height) / count)
        var pixels = 0
        for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 50)) {
            for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 50)) {
                val pixel = bitmap.getPixel(x, y)
                totalR += (pixel shr 16) and 0xFF
                totalG += (pixel shr 8) and 0xFF
                totalB += pixel and 0xFF
                pixels++
            }
        }
        return if (pixels > 0) {
            Color(
                red = (totalR / pixels).toInt(),
                green = (totalG / pixels).toInt(),
                blue = (totalB / pixels).toInt()
            )
        } else {
            Color(0xFF7C4DFF)
        }
    }

    private fun dominantColor(bitmap: Bitmap): Color? {
        val colorCounts = mutableMapOf<Int, Int>()
        var maxCount = 0
        var dominant = 0
        for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 20)) {
            for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 20)) {
                val pixel = bitmap.getPixel(x, y) and 0x00FFFFFF // ignore alpha
                val count = (colorCounts[pixel] ?: 0) + 1
                colorCounts[pixel] = count
                if (count > maxCount) {
                    maxCount = count
                    dominant = pixel
                }
            }
        }
        return if (maxCount > 0) Color(dominant or 0xFF000000.toInt()) else null
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
            if (r is SuccessResult) {
                val drawable = r.drawable
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    palette = PosterColorExtractor().extractColors(drawable.bitmap)
                }
            }
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
