package com.kurostream.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// ===== 1. Hero Crossfade with enhanced transitions =====
@Composable
fun HeroCrossfade(
    targetState: Any?,
    modifier: Modifier = Modifier,
    content: @Composable (Any?) -> Unit,
) {
    androidx.compose.animation.Crossfade(
        targetState = targetState,
        animationSpec = tween(durationMillis = 800),
        modifier = modifier,
        label = "heroCrossfade",
        content = content,
    )
}

// ===== 2. Parallax effect for hero banners =====
@Composable
fun ParallaxHeroBackground(
    imageUrl: String?,
    scrollOffset: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = scrollOffset * 0.3f
                    scaleX = 1f + (scrollOffset.absoluteValue / 2000f)
                    scaleY = 1f + (scrollOffset.absoluteValue / 2000f)
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.5f),
                        )
                    )
                )
        )
    }
}

// ===== 3. Glassmorphism effect for cards =====
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 10.dp,
    tintColor: Color = Color.White.copy(alpha = 0.1f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = SolidColor(tintColor),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
            }
            .graphicsLayer {
                alpha = 0.85f
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(
                        blurRadius.toPx(),
                        blurRadius.toPx(),
                        android.graphics.Shader.TileMode.CLAMP,
                    )
            }
    ) {
        content()
    }
}

// ===== 4. Shimmer loading effect =====
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF2A2A3A),
    highlightColor: Color = Color(0xFF3A3A4A),
    cornerRadius: Dp = 8.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            baseColor,
                            highlightColor,
                            baseColor,
                        ),
                        startX = -1f + shimmerTranslate * 1f,
                        endX = shimmerTranslate * 1f,
                    )
                )
        )
    }
}

// ===== 5. Focus scale animation with spring physics =====
@Composable
fun FocusScaleAnimation(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "focusScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "focusElevation",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                ambientShadowColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
                spotShadowColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
            }
    ) {
        content()
    }
}

// ===== 6. Content reveal animation =====
@Composable
fun StaggeredRevealItem(
    index: Int,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50,
            )
        ),
        modifier = modifier,
    ) {
        content()
    }
}

// ===== 7. Gradient border on focus =====
@Composable
fun GradientFocusBorder(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color(0xFF7C4DFF),
        Color(0xFF00E5FF),
    ),
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "borderWidth",
    )

    Box(
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = borderWidth,
                        brush = Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(cornerRadius),
                    )
                } else Modifier
            )
    ) {
        content()
    }
}

// ===== 8. Micro-interaction press feedback =====
@Composable
fun PressAnimation(
    isPressed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "pressScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}

// ===== 9. Smooth page indicator animation =====
@Composable
fun AnimatedPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    selectedWidth: Dp = 32.dp,
    unselectedWidth: Dp = 12.dp,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) selectedWidth else unselectedWidth,
                animationSpec = tween(300),
                label = "indicatorWidth",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        if (isSelected) color
                        else color.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

// ===== 10. D-pad input debounce utility =====
@Composable
fun DebouncedKeyHandler(
    debounceMs: Long = 150L,
    onKeyEvent: (Key) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var lastKeyTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier.onKeyEvent { event ->
            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                val now = System.currentTimeMillis()
                if (now - lastKeyTime > debounceMs) {
                    lastKeyTime = now
                    onKeyEvent(event.key)
                } else {
                    true
                }
            } else {
                false
            }
        }
    ) {
        content()
    }
}
