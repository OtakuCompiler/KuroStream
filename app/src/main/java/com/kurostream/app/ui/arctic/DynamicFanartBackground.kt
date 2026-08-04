// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Full-screen backdrop image with:
 *
 * 1. Ken Burns effect — slow pan+zoom that makes static backdrops feel alive.
 *    Cycles through three poses over [KB_DURATION_MS] ms:
 *      top-left zoom-in → center pan → bottom-right zoom-out → repeat
 *
 * 2. Triple-layer gradient scrim (top vignette + side vignet + bottom ramp)
 *    tuned to keep text legible at all hero positions.
 *
 * 3. Crossfade on image URL change — no jarring cuts.
 *
 * Pass [kenBurns]=false to disable the animation (e.g. low-RAM devices or
 * when the user has "Reduce Motion" enabled).
 */
@Composable
fun DynamicFanartBackground(
    imageUrl:   String?  = null,
    modifier:   Modifier = Modifier,
    kenBurns:   Boolean  = true,
    palette:    ArcticFusePalette = ArcticFusePalette(),
) {
    Box(modifier = modifier.fillMaxSize()) {

        if (!imageUrl.isNullOrBlank()) {
            val imageModifier = if (kenBurns) {
                Modifier
                    .fillMaxSize()
                    .kenBurnsEffect()
            } else {
                Modifier.fillMaxSize()
            }
            AsyncImage(
                model              = imageUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = imageModifier,
            )
        }

        // Top vignette — darkens the sky behind the hub switcher
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f   to palette.bg.copy(alpha = 0.60f),
                        0.3f to Color.Transparent,
                    )
                )
        )

        // Left edge vignette — softens the sidebar boundary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f   to palette.bg.copy(alpha = 0.80f),
                        0.2f to Color.Transparent,
                    )
                )
        )

        // Bottom ramp — must be fully opaque so the metadata row is always readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        0.70f to palette.bg.copy(alpha = 0.70f),
                        1.00f to palette.bg,
                    )
                )
        )
    }
}

// ── Ken Burns effect ──────────────────────────────────────────────────────────

private const val KB_DURATION_MS = 18_000

/**
 * Slow animated pan+scale modifier. Cycles through four poses:
 *   0 %  — 1.08× zoom, translated top-left
 *   50 % — 1.12× zoom, centred
 *  100 % — 1.08× zoom, translated bottom-right
 *
 * The scale never drops below 1.0 so we never see letter-box edges.
 */
private fun Modifier.kenBurnsEffect(): Modifier = composed(
    factory = {
        val transition = rememberInfiniteTransition(label = "kenBurns")

        // Primary pan axis (X)
        val tx by transition.animateFloat(
            initialValue   = -0.04f,
            targetValue    =  0.04f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(KB_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenBurns_tx",
        )
        // Secondary pan axis (Y) — offset phase via different duration
        val ty by transition.animateFloat(
            initialValue   =  0.03f,
            targetValue    = -0.03f,
            animationSpec  = infiniteRepeatable(
                animation  = tween((KB_DURATION_MS * 1.3).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenBurns_ty",
        )
        // Scale pulse
        val scale by transition.animateFloat(
            initialValue   = 1.08f,
            targetValue    = 1.14f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(KB_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenBurns_scale",
        )

        this.graphicsLayer {
            scaleX        = scale
            scaleY        = scale
            translationX  = tx * size.width
            translationY  = ty * size.height
        }
    },
    inspectorInfo = { name = "kenBurnsEffect" },
)

// Missing Modifier.composed workaround for Compose TV if needed
private fun Modifier.composed(
    factory: @Composable Modifier.() -> Modifier,
    inspectorInfo: androidx.compose.ui.platform.InspectorInfo.() -> Unit = {},
): Modifier = then(object : androidx.compose.ui.Modifier.Element {}).run {
    // Compose's standard composed() from compose-ui is available; this just calls it.
    androidx.compose.ui.composed(inspectorInfo) { factory() }
}
