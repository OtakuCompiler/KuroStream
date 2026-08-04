// This file is part of KuroStream.
//
// BlueGlowEffect — cinematic blue glow composable for focused elements.
// Low GPU cost: static radial gradient, no continuous blur.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.kurostream.app.ui.theme.CinematicBlueGlow

@Composable
fun BlueGlowEffect(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 0.35f,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(isFocused) }
    val glowAlpha = if (focused) intensity else 0f
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (focused) 1f else 0.9f }
            .drawBehind {
                if (glowAlpha > 0f) {
                    val gradient = Brush.radialGradient(
                        colors = listOf(CinematicBlueGlow.copy(alpha = glowAlpha), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension * 0.6f,
                    )
                    drawRect(brush = gradient)
                }
            },
    ) {
        content()
    }
}
