// This file is part of KuroStream.
//
// Arctic Fuse 3 icon set — custom-drawn geometric line icons matching the
// Nuvio TV style (thin stroke, no fill, round caps/joins). Replaces the
// generic Material icon set used previously. Each icon is drawn on a 24x24
// virtual grid and scaled to whatever `size` is requested.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared stroke used by every nav icon: 1.5dp on a 24dp grid, scaled
 * proportionally when [size] differs from 24dp (e.g. 18dp hero buttons).
 */
private fun DrawScope.afStroke(strokeWidthDp: Dp = 1.5.dp): Stroke {
    val scale = size.width / 24.dp.toPx()
    return Stroke(
        width = strokeWidthDp.toPx() * scale,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
}

/** Maps a 24x24-grid coordinate to actual canvas pixels for the current draw size. */
private fun DrawScope.g(x: Float, y: Float): Offset {
    val scale = size.width / 24f
    return Offset(x * scale, y * scale)
}

@Composable
private fun IconSlot(
    iconSize: Dp,
    modifier: Modifier,
    draw: DrawScope.() -> Unit,
) {
    Canvas(modifier = modifier.size(iconSize)) { draw() }
}

// ---------------------------------------------------------------------
// Home — minimal house outline: triangular roof, rectangular base,
// no door, no chimney.
// ---------------------------------------------------------------------
@Composable
fun IconHome(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val roof = Path().apply {
            moveTo(g(3f, 11f).x, g(3f, 11f).y)
            lineTo(g(12f, 4f).x, g(12f, 4f).y)
            lineTo(g(21f, 11f).x, g(21f, 11f).y)
        }
        drawPath(roof, color = tint, style = stroke)
        val base = Path().apply {
            moveTo(g(5.5f, 10f).x, g(5.5f, 10f).y)
            lineTo(g(5.5f, 20f).x, g(5.5f, 20f).y)
            lineTo(g(18.5f, 20f).x, g(18.5f, 20f).y)
            lineTo(g(18.5f, 10f).x, g(18.5f, 10f).y)
        }
        drawPath(base, color = tint, style = stroke)
    }

// ---------------------------------------------------------------------
// Search — thin circle with a diagonal line extending bottom-right at
// 45°. Line length = radius.
// ---------------------------------------------------------------------
@Composable
fun IconSearch(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val radius = 6f
        val center = Offset(10f, 10f)
        drawCircle(color = tint, radius = radius * (size.width / 24f), center = g(center.x, center.y), style = stroke)
        val handleStart = g(center.x + radius * cos(Math.PI / 4).toFloat(), center.y + radius * sin(Math.PI / 4).toFloat())
        val handleEnd = g(
            center.x + radius * cos(Math.PI / 4).toFloat() + radius * cos(Math.PI / 4).toFloat(),
            center.y + radius * sin(Math.PI / 4).toFloat() + radius * sin(Math.PI / 4).toFloat(),
        )
        drawLine(color = tint, start = handleStart, end = handleEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
    }

// ---------------------------------------------------------------------
// Library (used for the "Movies" hub) — three vertical rounded
// rectangles, middle one slightly taller.
// ---------------------------------------------------------------------
@Composable
fun IconLibrary(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        val barW = 4.5f
        val gap = 1.5f
        val xs = floatArrayOf(3.5f, 3.5f + barW + gap, 3.5f + (barW + gap) * 2)
        val heights = floatArrayOf(14f, 18f, 14f)
        val tops = floatArrayOf(5f, 3f, 5f)
        xs.forEachIndexed { i, x ->
            drawRoundRect(
                color = tint,
                topLeft = g(x, tops[i]),
                size = androidx.compose.ui.geometry.Size(barW * scale, heights[i] * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale, 2f * scale),
                style = stroke,
            )
        }
    }

@Composable
fun IconMovies(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconLibrary(tint, iconSize, modifier)

// ---------------------------------------------------------------------
// Downloads — downward arrow inside a thin square box. Straight shaft,
// triangular head.
// ---------------------------------------------------------------------
@Composable
fun IconDownloads(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        drawRoundRect(
            color = tint,
            topLeft = g(3f, 3f),
            size = androidx.compose.ui.geometry.Size((18f) * (size.width / 24f), (18f) * (size.width / 24f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * (size.width / 24f)),
            style = stroke,
        )
        drawLine(color = tint, start = g(12f, 7f), end = g(12f, 14.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(g(8.5f, 11.5f).x, g(8.5f, 11.5f).y)
            lineTo(g(12f, 15f).x, g(12f, 15f).y)
            lineTo(g(15.5f, 11.5f).x, g(15.5f, 11.5f).y)
        }
        drawPath(head, color = tint, style = stroke)
    }

// ---------------------------------------------------------------------
// Settings — single gear/cog with 8 teeth, hollow center (donut), thin
// ring.
// ---------------------------------------------------------------------
@Composable
fun IconSettings(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        val center = g(12f, 12f)
        val outerR = 8.5f * scale
        val innerR = 5.5f * scale
        val toothLen = 2f * scale
        val teeth = 8
        for (i in 0 until teeth) {
            val angle = (Math.PI * 2 * i / teeth)
            val x1 = center.x + (outerR - toothLen / 2) * cos(angle).toFloat()
            val y1 = center.y + (outerR - toothLen / 2) * sin(angle).toFloat()
            val x2 = center.x + (outerR + toothLen / 2) * cos(angle).toFloat()
            val y2 = center.y + (outerR + toothLen / 2) * sin(angle).toFloat()
            drawLine(color = tint, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        drawCircle(color = tint, radius = outerR - toothLen / 2, center = center, style = stroke)
        drawCircle(color = tint, radius = 2f * scale, center = center, style = stroke) // hollow donut center
    }

// ---------------------------------------------------------------------
// Extensions (used for the "Add-ons" hub) — four squares in a 2x2 grid
// with a 2dp gap.
// ---------------------------------------------------------------------
@Composable
fun IconExtensions(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        val sq = 8f
        val gap = 2f
        val start = 4f
        val positions = listOf(
            start to start,
            start + sq + gap to start,
            start to start + sq + gap,
            start + sq + gap to start + sq + gap,
        )
        positions.forEach { (x, y) ->
            drawRoundRect(
                color = tint,
                topLeft = g(x, y),
                size = androidx.compose.ui.geometry.Size(sq * scale, sq * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * scale),
                style = stroke,
            )
        }
    }

// ---------------------------------------------------------------------
// Live TV (used for "TV Shows") — play triangle inside a thin rounded
// rectangle (screen shape).
// ---------------------------------------------------------------------
@Composable
fun IconTV(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        drawRoundRect(
            color = tint,
            topLeft = g(2.5f, 5f),
            size = androidx.compose.ui.geometry.Size(19f * scale, 14f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * scale),
            style = stroke,
        )
        val play = Path().apply {
            moveTo(g(10f, 9f).x, g(10f, 9f).y)
            lineTo(g(16f, 12f).x, g(16f, 12f).y)
            lineTo(g(10f, 15f).x, g(10f, 15f).y)
            close()
        }
        drawPath(play, color = tint, style = stroke)
    }

/** YouTube hub reuses the same screen shape but wider, to read as a distinct glyph from Live TV. */
@Composable
fun IconYouTube(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        drawRoundRect(
            color = tint,
            topLeft = g(1.5f, 6.5f),
            size = androidx.compose.ui.geometry.Size(21f * scale, 11f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.5f * scale),
            style = stroke,
        )
        val play = Path().apply {
            moveTo(g(10.5f, 9.5f).x, g(10.5f, 9.5f).y)
            lineTo(g(15f, 12f).x, g(15f, 12f).y)
            lineTo(g(10.5f, 14.5f).x, g(10.5f, 14.5f).y)
            close()
        }
        drawPath(play, color = tint, style = stroke)
    }

// ---------------------------------------------------------------------
// Profile — simple circle on top of a curved line (shoulders), a
// minimal person silhouette.
// ---------------------------------------------------------------------
@Composable
fun IconProfile(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        drawCircle(color = tint, radius = 3.2f * scale, center = g(12f, 8f), style = stroke)
        val shoulders = Path().apply {
            moveTo(g(5f, 19.5f).x, g(5f, 19.5f).y)
            quadraticBezierTo(
                g(12f, 12.5f).x, g(12f, 12.5f).y,
                g(19f, 19.5f).x, g(19f, 19.5f).y,
            )
        }
        drawPath(shoulders, color = tint, style = stroke)
    }

// ---------------------------------------------------------------------
// Favourites — thin heart outline (not in the base Nuvio set, drawn in
// the same stroke language so it doesn't look out of place).
// ---------------------------------------------------------------------
@Composable
fun IconFav(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val heart = Path().apply {
            moveTo(g(12f, 19f).x, g(12f, 19f).y)
            cubicTo(
                g(4f, 13f).x, g(4f, 13f).y,
                g(3f, 8.5f).x, g(3f, 8.5f).y,
                g(6.5f, 6.5f).x, g(6.5f, 6.5f).y,
            )
            cubicTo(
                g(9f, 5f).x, g(9f, 5f).y,
                g(11f, 6.5f).x, g(11f, 6.5f).y,
                g(12f, 8f).x, g(12f, 8f).y,
            )
            cubicTo(
                g(13f, 6.5f).x, g(13f, 6.5f).y,
                g(15f, 5f).x, g(15f, 5f).y,
                g(17.5f, 6.5f).x, g(17.5f, 6.5f).y,
            )
            cubicTo(
                g(21f, 8.5f).x, g(21f, 8.5f).y,
                g(20f, 13f).x, g(20f, 13f).y,
                g(12f, 19f).x, g(12f, 19f).y,
            )
            close()
        }
        drawPath(heart, color = tint, style = stroke)
    }

@Composable
fun IconFavOutline(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconFav(tint, iconSize, modifier)

// ---------------------------------------------------------------------
// Remaining utility icons (player controls, buttons, badges) redrawn in
// the same thin-stroke language. Play stays filled per spec §6.1 (solid
// white CTA button uses a filled triangle at 16dp).
// ---------------------------------------------------------------------
@Composable
fun IconPlay(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier, filled: Boolean = true) =
    IconSlot(iconSize, modifier) {
        val scale = size.width / 24f
        val path = Path().apply {
            moveTo(g(8f, 5f).x, g(8f, 5f).y)
            lineTo(g(19f, 12f).x, g(19f, 12f).y)
            lineTo(g(8f, 19f).x, g(8f, 19f).y)
            close()
        }
        if (filled) {
            drawPath(path, color = tint)
        } else {
            drawPath(path, color = tint, style = afStroke())
        }
    }

@Composable
fun IconPause(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val scale = size.width / 24f
        drawRoundRect(color = tint, topLeft = g(6f, 5f), size = androidx.compose.ui.geometry.Size(4f * scale, 14f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * scale))
        drawRoundRect(color = tint, topLeft = g(14f, 5f), size = androidx.compose.ui.geometry.Size(4f * scale, 14f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * scale))
    }

@Composable
fun IconInfo(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val scale = size.width / 24f
        drawCircle(color = tint, radius = 9f * scale, center = g(12f, 12f), style = stroke)
        drawCircle(color = tint, radius = 1f * scale, center = g(12f, 8f))
        drawLine(color = tint, start = g(12f, 11f), end = g(12f, 16.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }

@Composable
fun IconAdd(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        drawLine(color = tint, start = g(12f, 5f), end = g(12f, 19f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = g(5f, 12f), end = g(19f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }

@Composable
fun IconBookmark(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val path = Path().apply {
            moveTo(g(6f, 4f).x, g(6f, 4f).y)
            lineTo(g(18f, 4f).x, g(18f, 4f).y)
            lineTo(g(18f, 20f).x, g(18f, 20f).y)
            lineTo(g(12f, 15.5f).x, g(12f, 15.5f).y)
            lineTo(g(6f, 20f).x, g(6f, 20f).y)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }

@Composable
fun IconCheck(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val path = Path().apply {
            moveTo(g(5f, 12.5f).x, g(5f, 12.5f).y)
            lineTo(g(10f, 17.5f).x, g(10f, 17.5f).y)
            lineTo(g(19f, 7f).x, g(19f, 7f).y)
        }
        drawPath(path, color = tint, style = stroke)
    }

@Composable
fun IconClose(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        drawLine(color = tint, start = g(6f, 6f), end = g(18f, 18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = g(18f, 6f), end = g(6f, 18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }

@Composable
fun IconStar(tint: Color = AFStarGold, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val scale = size.width / 24f
        val cx = 12f
        val cy = 12f
        val outerR = 9f
        val innerR = 3.8f
        val path = Path()
        for (i in 0 until 10) {
            val angle = Math.PI / 2 * 3 + Math.PI * i / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            val pt = g(x, y)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        drawPath(path, color = tint)
    }

@Composable
fun IconSkipBack(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val scale = size.width / 24f
        val path = Path().apply {
            moveTo(g(17f, 5f).x, g(17f, 5f).y)
            lineTo(g(8f, 12f).x, g(8f, 12f).y)
            lineTo(g(17f, 19f).x, g(17f, 19f).y)
            close()
        }
        drawPath(path, color = tint)
        drawRoundRect(color = tint, topLeft = g(5.5f, 5f), size = androidx.compose.ui.geometry.Size(1.6f * scale, 14f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f * scale))
    }

@Composable
fun IconSkipFwd(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val scale = size.width / 24f
        val path = Path().apply {
            moveTo(g(7f, 5f).x, g(7f, 5f).y)
            lineTo(g(16f, 12f).x, g(16f, 12f).y)
            lineTo(g(7f, 19f).x, g(7f, 19f).y)
            close()
        }
        drawPath(path, color = tint)
        drawRoundRect(color = tint, topLeft = g(16.9f, 5f), size = androidx.compose.ui.geometry.Size(1.6f * scale, 14f * scale), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f * scale))
    }

@Composable
fun IconVolume(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        val body = Path().apply {
            moveTo(g(4f, 10f).x, g(4f, 10f).y)
            lineTo(g(8f, 10f).x, g(8f, 10f).y)
            lineTo(g(12.5f, 6f).x, g(12.5f, 6f).y)
            lineTo(g(12.5f, 18f).x, g(12.5f, 18f).y)
            lineTo(g(8f, 14f).x, g(8f, 14f).y)
            lineTo(g(4f, 14f).x, g(4f, 14f).y)
            close()
        }
        drawPath(body, color = tint, style = stroke)
        val wave1 = Path().apply {
            moveTo(g(16f, 9f).x, g(16f, 9f).y)
            quadraticBezierTo(g(18.5f, 12f).x, g(18.5f, 12f).y, g(16f, 15f).x, g(16f, 15f).y)
        }
        drawPath(wave1, color = tint, style = stroke)
        val wave2 = Path().apply {
            moveTo(g(18.2f, 6.5f).x, g(18.2f, 6.5f).y)
            quadraticBezierTo(g(22f, 12f).x, g(22f, 12f).y, g(18.2f, 17.5f).x, g(18.2f, 17.5f).y)
        }
        drawPath(wave2, color = tint, style = stroke)
    }

@Composable
fun IconMenu(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val stroke = afStroke()
        drawLine(color = tint, start = g(4f, 7f), end = g(20f, 7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = g(4f, 12f), end = g(20f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = g(4f, 17f), end = g(20f, 17f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
