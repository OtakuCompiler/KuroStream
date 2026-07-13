// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
)

class PosterColorExtractor {
    suspend fun extractColors(bitmap: Bitmap): DynamicColorPalette = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        
        val dominantColor = palette.dominantSwatch?.rgb ?: Color(0xFF7C4DFF).toArgb()
        val vibrantColor = palette.vibrantSwatch?.rgb ?: ColorUtils.setAlphaComponent(dominantColor, 200)
        val mutedColor = palette.mutedSwatch?.rgb ?: ColorUtils.compositeColors(
            Color(0xFF1E1E2E).toArgb(),
            dominantColor
        )
        
        val primary = Color(dominantColor)
        val secondary = Color(vibrantColor)
        val tertiary = Color(mutedColor)
        
        DynamicColorPalette(
            primary = primary,
            onPrimary = calculateOnColor(primary),
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = calculateOnColor(secondary),
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            onTertiary = calculateOnColor(tertiary),
            background = TvBackground,
            onBackground = TvOnBackground,
            surface = TvSurface,
            onSurface = TvOnSurface,
            surfaceVariant = TvSurfaceVariant,
            onSurfaceVariant = TvOnSurfaceVariant,
        )
    }
    
    private fun calculateOnColor(background: Color): Color {
        val luminance = ColorUtils.calculateLuminance(background.toArgb())
        return if (luminance > 0.5) Color(0xFF000000) else Color(0xFFFFFFFF)
    }
    
    suspend fun extractColorsFromUrl(imageUrl: String): DynamicColorPalette? {
        return try {
            val context = LocalContext.current.applicationContext
            val imageLoader = ImageLoader.Builder(context).build()
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.bitmap
                extractColors(bitmap)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberDynamicTheme(
    imageUrl: String?,
    defaultPalette: DynamicColorPalette = TvDarkColorScheme.toDynamicPalette()
): DynamicColorPalette {
    var colorPalette by remember { mutableStateOf<DynamicColorPalette?>(null) }
    
    if (imageUrl != null) {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader.Builder(context).build() }
        
        androidx.compose.runtime.LaunchedEffect(imageUrl) {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.bitmap
                colorPalette = PosterColorExtractor().extractColors(bitmap)
            }
        }
    }
    
    val targetPalette = colorPalette ?: defaultPalette
    
    val animatedPrimary by animateColorAsState(
        targetValue = targetPalette.primary,
        animationSpec = tween(600),
        label = "animatedPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetPalette.secondary,
        animationSpec = tween(600),
        label = "animatedSecondary"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = targetPalette.tertiary,
        animationSpec = tween(600),
        label = "animatedTertiary"
    )
    
    return targetPalette.copy(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary
    )
}

private fun androidx.tv.material3.ColorScheme.toDynamicPalette(): DynamicColorPalette {
    return DynamicColorPalette(
        primary = this.primary,
        onPrimary = this.onPrimary,
        primaryContainer = this.primaryContainer,
        onPrimaryContainer = this.onPrimaryContainer,
        secondary = this.secondary,
        onSecondary = this.onSecondary,
        secondaryContainer = this.secondaryContainer,
        onSecondaryContainer = this.onSecondaryContainer,
        tertiary = this.tertiary,
        onTertiary = this.onTertiary,
        background = this.background,
        onBackground = this.onBackground,
        surface = this.surface,
        onSurface = this.onSurface,
        surfaceVariant = this.surfaceVariant,
        onSurfaceVariant = this.onSurfaceVariant,
    )
}

fun generateAmoledBlackScheme(): androidx.tv.material3.ColorScheme {
    return androidx.tv.material3.darkColorScheme(
        primary = Color(0xFF7C4DFF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF651FFF).copy(alpha = 0.12f),
        onPrimaryContainer = Color(0xFF7C4DFF),
        secondary = Color(0xFF00E5FF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF00B8D4).copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF00E5FF),
        tertiary = Color(0xFF00E5FF),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF00B8D4).copy(alpha = 0.12f),
        onTertiaryContainer = Color(0xFF00E5FF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF0A0A0A),
        onSurfaceVariant = Color(0xFFB0B0C0),
        error = Color(0xFFCF6679),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFCF6679).copy(alpha = 0.12f),
        onErrorContainer = Color(0xFFCF6679),
        outline = Color(0xFF7C4DFF).copy(alpha = 0.3f),
        outlineVariant = Color(0xFF7C4DFF).copy(alpha = 0.12f),
    )
}

fun generateScheduledTheme(
    isNight: Boolean,
    useAmoled: Boolean = false
): androidx.tv.material3.ColorScheme {
    return if (useAmoled) {
        generateAmoledBlackScheme()
    } else if (isNight) {
        TvDarkColorScheme
    } else {
        TvDarkColorScheme
    }
}