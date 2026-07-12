package com.kurostream.players.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glassmorphism card with backdrop blur and gradient.
 */
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .blur(8.dp)
    ) {
        content()
    }
}

/**
 * Gradient border with animated sweep effect.
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Red),
    borderWidth: Float = 4f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .drawWithContent {
                // Draw gradient border
                drawRoundRect(
                    brush = Brush.sweepGradient(colors = colors),
                    style = Stroke(width = borderWidth.dp.toPx()),
                )
                drawContent()
            }
    ) {
        content()
    }
}

/**
 * Shimmer loading effect for perceived performance.
 */
@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Gray.copy(alpha = alpha),
                        Color.Gray.copy(alpha = alpha * 0.5f),
                        Color.Gray.copy(alpha = alpha)
                    )
                )
            )
    )
}

/**
 * Animated page indicator with size transition.
 */
@Composable
fun AnimatedPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val size by animateDpAsState(
                targetValue = if (isActive) 12.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Parallax scrolling header.
 */
@Composable
fun ParallaxHeader(
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    parallaxFactor: Float = 0.5f
) {
    val parallaxOffset by animateFloatAsState(
        targetValue = scrollOffset * parallaxFactor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = modifier
            .offset { androidx.compose.ui.unit.IntOffset(0, parallaxOffset.toInt()) }
    ) {
        // Header content
    }
}

/**
 * Staggered reveal animation for list items.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 50, // Stagger by 50ms per item
            easing = FastOutSlowInEasing
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .scale(scale)
    ) {
        content()
    }
}

/**
 * Press feedback with scale and brightness.
 */
@Composable
fun PressFeedback(
    pressed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val brightness by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = tween(100)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .drawWithContent {
                drawContent()
                drawRect(
                    color = Color.Black.copy(alpha = 1f - brightness),
                    size = size
                )
            }
    ) {
        content()
    }
}

/**
 * Debounced search input with smooth transitions.
 */
@Composable
fun DebouncedSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    debounceMs: Long = 300
) {
    var internalValue by remember { mutableStateOf(value) }
    val debouncedValue by remember {
        derivedStateOf {
            // Debounce logic would go here
            internalValue
        }
    }

    // Search input implementation
}
