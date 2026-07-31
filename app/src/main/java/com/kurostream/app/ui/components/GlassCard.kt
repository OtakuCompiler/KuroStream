// This file is part of KuroStream.
//
// GlassCardModifier — reusable glass effect for Arctic Fuse cards.
// Adds translucent background, subtle blue border, soft shadow,
// and optional focus glow. TV-optimized: no continuous expensive blur.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kurostream.app.ui.theme.BlueGlowTokens
import com.kurostream.app.ui.theme.CinematicBlueGlow
import com.kurostream.app.ui.theme.GlassTokens
import com.kurostream.app.ui.theme.OledSurfaceElevated

@Composable
fun GlassCard(
    isFocused: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isSelected -> GlassTokens.opacitySelected
            isFocused -> GlassTokens.opacityFocused
            else -> GlassTokens.opacityNormal
        },
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "glassAlpha",
    )
    val borderAlpha = if (isFocused) 0.4f else GlassTokens.borderAlpha
    val borderColor = CinematicBluePrimary.copy(alpha = borderAlpha)
    val glowAlpha = if (isFocused) BlueGlowTokens.glowAlpha else 0f

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = alpha
                shadowElevation = if (isFocused) 12f else 4f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        OledSurfaceElevated.copy(alpha = 0.95f),
                        OledSurfaceElevated.copy(alpha = 0.85f),
                    ),
                ),
            )
            .border(
                width = if (isFocused) 2.dp else GlassTokens.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .drawBehind {
                if (glowAlpha > 0f) {
                    val glow = CinematicBlueGlow.copy(alpha = glowAlpha)
                    drawRoundRect(
                        color = glow,
                        topLeft = androidx.compose.ui.geometry.Offset(-BlueGlowTokens.glowRadius.toPx(), -BlueGlowTokens.glowRadius.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width + BlueGlowTokens.glowRadius.toPx() * 2,
                            size.height + BlueGlowTokens.glowRadius.toPx() * 2,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                    )
                }
            },
        content = content,
    )
}
