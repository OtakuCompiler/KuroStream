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

import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusableCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    borderColor: Color = Color(0xFF7C4DFF),
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val (focusState, _) = remember { mutableStateOf(FocusState.Unfocused) }
    val scale by animateDpAsState(
        targetValue = if (focusState.isFocused) 1.04.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val borderOpacity by animateFloatAsState(
        targetValue = if (focusState.isFocused) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val glowRadius by animateDpAsState(
        targetValue = if (focusState.isFocused) 16.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val elevation by animateFloatAsState(
        targetValue = if (focusState.isFocused) 16f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value / 1.dp
                scaleY = scale.value / 1.dp
                shadowElevation = elevation
            }
            .clip(RoundedCornerShape(cornerRadius))
            .onFocusChanged { state ->
                focusState = state
            }
            .padding(4.dp) // Padding for glow effect
    ) {
        // Glow effect
        if (focusState.isFocused && glowRadius.value > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Blur effect for glow
                    }
                    .background(
                        borderColor.copy(alpha = 0.2f),
                        RoundedCornerShape(cornerRadius + glowRadius)
                    )
                    .padding(-glowRadius)
            )
        }

        // Main card content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Transparent)
        ) {
            content()
        }

        // Focus border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = borderWidth,
                    color = borderColor.copy(alpha = borderOpacity),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

@Composable
fun MicroInteractionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pressScale by animateDpAsState(
        targetValue = if (isPressed) 0.96.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh)
    )
    val pressElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )
    var isPressed by remember { mutableStateOf(false) }
    var rippleAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            rippleAlpha = 0.1f
            // Ripple animation would go here
        } else {
            rippleAlpha = 0f
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value / 1.dp
                scaleY = pressScale.value / 1.dp
                shadowElevation = pressElevation
            }
            .fillMaxSize()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .onClick(onClick)
            .onHover { isPressed = it }
            .combinedClickable(
                onClick = { isPressed = true; onClick(); isPressed = false },
                onLongClick = {},
            )
    ) {
        content()
    }
}

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF2A2A3A),
    highlightColor: Color = Color(0xFF3A3A4A),
    cornerRadius: Dp = 8.dp,
) {
    val shimmerProgress by remember {
        mutableStateOf(0f)
    }
    
    LaunchedEffect(Unit) {
        // In a real implementation, this would use Animatable or rememberInfiniteTransition
    }

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
            .background(baseColor)
    ) {
        // Shimmer effect overlay
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
                        startX = -1f + shimmerProgress * 2f,
                        endX = shimmerProgress * 2f,
                    )
                )
        )
    }
}

@Composable
fun RippleEffect(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.1f,
    onRippleEnd: () -> Unit = {},
) {
    // Simplified ripple - in production would use rememberRipple or custom implementation
    Box(modifier = modifier) { }
}

@Composable
fun StaggeredItemAnimation(
    index: Int,
    totalItems: Int,
    baseDelay: Int = 50,
    content: @Composable () -> Unit,
) {
    // Staggered entrance animation
    LaunchedEffect(index) {
        // Delay based on index
    }
    content()
}

@Composable
fun ParallaxBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    parallaxFactor: Float = 0.5f,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Parallax effect based on scroll position
                    translationY = 0f // Would be connected to scroll state
                }
        )
    }
}