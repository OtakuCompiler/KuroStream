package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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
        animationSpec = tween(500),
        label = "bgOpacity",
    )

    LaunchedEffect(imageUrl) {
        if (imageUrl != null && imageUrl.isNotBlank()) {
            dominantColor = extractDominantColor(imageUrl)
        } else {
            dominantColor = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        val color = dominantColor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedOpacity }
                .background(
                    Brush.verticalGradient(
                        colors = if (animateColors && color != null) {
                            listOf(
                                color.copy(alpha = 0f),
                                color.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f),
                            )
                        } else {
                            gradientColors
                        },
                    ),
                ),
        )
    }
}

@Composable
fun HeroBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    overlayOpacity: Float = 0.6f,
) {
    var dominantColor by remember { mutableStateOf<Color?>(null) }

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
        )

        val color = dominantColor
        val gradientBrush = if (color != null) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = 0.1f),
                    color.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = overlayOpacity),
                ),
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = overlayOpacity),
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
        )
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

        val currentPalette = palette
        val backgroundBrush = if (currentPalette != null && currentPalette.isNotEmpty()) {
            val primaryColor = currentPalette[0]
            val secondaryColor = if (currentPalette.size > 1) currentPalette[1] else primaryColor
            Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0f),
                    primaryColor.copy(alpha = 0.2f),
                    secondaryColor.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.8f),
                ),
                center = Offset(0.5f, 0.5f),
                radius = 1.2f,
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.8f),
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
        )
    }
}

private fun extractDominantColor(imageUrl: String): Color? {
    val hash = imageUrl.hashCode()
    val hue = (hash % 360).toFloat()
    return Color.hsv(hue, 0.6f, 0.4f)
}

private fun generatePaletteFromUrl(url: String): List<Color> {
    val hash = url.hashCode()
    val hue1 = (hash % 360).toFloat()
    val hue2 = ((hash / 360) % 360).toFloat()
    return listOf(
        Color.hsv(hue1, 0.7f, 0.4f),
        Color.hsv(hue2, 0.5f, 0.3f),
        Color.hsv((hue1 + 60) % 360, 0.4f, 0.3f),
    )
}
