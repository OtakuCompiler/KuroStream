package com.kurostream.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.kurostream.ui.theme.KuroPrimary
import com.kurostream.ui.theme.KuroSurface
import com.kurostream.ui.theme.KuroSurfaceVariant

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = KuroSurface,
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )
    val color by animateColorAsState(
        targetValue = if (isFocused) KuroPrimary.copy(alpha = 0.2f) else containerColor,
        animationSpec = tween(durationMillis = 150),
        label = "color"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        color = color,
        shape = shape,
        shadowElevation = if (isFocused) elevation * 2 else elevation,
        tonalElevation = if (isFocused) 8.dp else 2.dp,
        interactionSource = interactionSource
    ) {
        content()
    }
}
