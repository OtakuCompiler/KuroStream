// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Af3FocusCard — performance-optimized focus wrapper.
 *
 * Replaces the per-card Material3 focus highlight with a custom
 * graphicsLayer-driven transition (scale + elevation + glow border).
 * Avoids the recomposition overhead of Material3's Card composable
 * while keeping a polished AF3-style focus state.
 *
 * Performance characteristics:
 * - Uses `graphicsLayer` (skipped during draw when scale == 1)
 * - Animation runs on the animation thread, not the main composer
 * - Single remember'd interaction source per card
 */
@Composable
fun Af3FocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.06f,
    baseElevationDp: Int = 2,
    focusedElevationDp: Int = 12,
    cornerRadiusDp: Int = 10,
    content: @Composable () -> Unit,
) {
    val palette = LocalArcticFusePalette.current
    val tokens = LocalArcticFuse3Tokens.current

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f),
        label = "focus_scale",
    )
    val elevationDp by animateFloatAsState(
        targetValue = if (focused) focusedElevationDp.toFloat() else baseElevationDp.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f),
        label = "focus_elevation",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevationDp.dp, RoundedCornerShape(cornerRadiusDp.dp))
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(palette.surface)
            .border(
                width = if (focused) tokens.focusBorderWidth else 0.dp,
                color = if (focused) palette.borderFocus else Color.Transparent,
                shape = RoundedCornerShape(cornerRadiusDp.dp),
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}
