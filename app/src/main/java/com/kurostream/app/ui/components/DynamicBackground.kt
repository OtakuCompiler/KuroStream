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

package com.kurostream.app.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.PorterDuffMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.Transformation

@Composable
fun DynamicBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black),
    animateColors: Boolean = true,
) {
    var dominantColor by remember { mutableStateOf<Color?>(null) }
    val animatedOpacity by animateFloatAsState(
        targetValue = if (dominantColor != null) 1f else 0f,
        animationSpec = tween(500)
    )

    // Extract dominant color from image
    LaunchedEffect(imageUrl) {
        if (imageUrl != null && imageUrl.isNotBlank()) {
            // In a real implementation, this would use a palette library
            // For now, we'll use a placeholder
            dominantColor = extractDominantColor(imageUrl)
        } else {
            dominantColor = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = { },
            error = { },
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedOpacity }
                .background(
                    Brush.verticalGradient(
                        colors = if (animateColors && dominantColor != null) {
                            listOf(
                                dominantColor!!.copy(alpha = 0f),
                                dominantColor!!.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f),
                            )
                        } else {
                            gradientColors
                        }
                    )
                )
        )
    }
}

private fun extractDominantColor(imageUrl: String): Color? {
    // Placeholder - in production would use Palette API or Coil's palette generation
    // Return a color based on URL hash for demo
    val hash = imageUrl.hashCode()
    val hue = (hash % 360).toFloat()
    return Color.HSVToColor(hue, 0.6f, 0.4f)
}

@Composable
fun HeroBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    overlayOpacity: Float = 0.6f,
) {
    val dominantColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null && imageUrl.isNotBlank()) {
            dominantColor = extractDominantColor(imageUrl)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
        )

        if (dominantColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                dominantColor!!.copy(alpha = 0.1f),
                                dominantColor!!.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = overlayOpacity),
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = overlayOpacity),
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun DynamicBackgroundWithPalette(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onPaletteReady: (List<Color>) -> Unit = {},
) {
    var palette by remember { mutableStateOf<List<Color>?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null && imageUrl.isNotBlank()) {
            // In production: use Coil's PaletteTransformation or Android Palette API
            val colors = generatePaletteFromUrl(imageUrl)
            palette = colors
            onPaletteReady(colors)
        } else {
            palette = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (palette != null && palette!!.isNotEmpty()) {
            val primaryColor = palette!![0]
            val secondaryColor = if (palette!!.size > 1) palette!![1] else primaryColor
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0f),
                                primaryColor.copy(alpha = 0.2f),
                                secondaryColor.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.8f),
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            radius = 1.2f,
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f),
                            )
                        )
                    )
            )
        }
    }
}

private fun generatePaletteFromUrl(url: String): List<Color> {
    // Placeholder - in production would use Palette API
    val hash = url.hashCode()
    val hue1 = (hash % 360).toFloat()
    val hue2 = ((hash / 360) % 360).toFloat()
    
    return listOf(
        Color.HSVToColor(hue1, 0.7f, 0.4f),
        Color.HSVToColor(hue2, 0.5f, 0.3f),
        Color.HSVToColor((hue1 + 60) % 360, 0.4f, 0.3f),
    )
}