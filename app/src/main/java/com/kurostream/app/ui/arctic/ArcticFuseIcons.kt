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

// ── Icon infrastructure ───────────────────────────────────────────────────────

private fun DrawScope.afStroke(strokeWidthDp: Dp = 1.5.dp): Stroke {
    val scale = size.width / 24.dp.toPx()
    return Stroke(width = strokeWidthDp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
}

private fun DrawScope.g(x: Float, y: Float): Offset {
    val scale = size.width / 24f
    return Offset(x * scale, y * scale)
}

@Composable
private fun IconSlot(iconSize: Dp, modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier = modifier.size(iconSize)) { draw() }
}

// ── Navigation icons ──────────────────────────────────────────────────────────

@Composable
fun IconHome(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawPath(Path().apply { moveTo(g(3f,11f).x,g(3f,11f).y); lineTo(g(12f,4f).x,g(12f,4f).y); lineTo(g(21f,11f).x,g(21f,11f).y) }, tint, style = s)
        drawPath(Path().apply { moveTo(g(5.5f,10f).x,g(5.5f,10f).y); lineTo(g(5.5f,20f).x,g(5.5f,20f).y); lineTo(g(18.5f,20f).x,g(18.5f,20f).y); lineTo(g(18.5f,10f).x,g(18.5f,10f).y) }, tint, style = s)
    }

@Composable
fun IconSearch(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val r = 6f; val cx = 10f; val cy = 10f
        drawCircle(tint, r*(size.width/24f), g(cx,cy), style = s)
        val a = (Math.PI/4).toFloat()
        drawLine(tint, g(cx+r*cos(a),cy+r*sin(a)), g(cx+r*2*cos(a),cy+r*2*sin(a)), s.width, cap = StrokeCap.Round)
    }

@Composable
fun IconLibrary(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f; val bw = 4.5f; val gap = 1.5f
        listOf(3.5f, 3.5f+bw+gap, 3.5f+(bw+gap)*2f).zip(listOf(14f,18f,14f)).zip(listOf(5f,3f,5f)).forEach { (wh, top) ->
            val (w, h) = wh
            drawRoundRect(tint, g(w,top), androidx.compose.ui.geometry.Size(bw*scale, h*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        }
    }

@Composable
fun IconDownloads(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(3f,3f), androidx.compose.ui.geometry.Size(18f*scale,18f*scale), androidx.compose.ui.geometry.CornerRadius(3f*scale), style = s)
        drawLine(tint, g(12f,7f), g(12f,14.5f), s.width, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(g(8.5f,11.5f).x,g(8.5f,11.5f).y); lineTo(g(12f,15f).x,g(12f,15f).y); lineTo(g(15.5f,11.5f).x,g(15.5f,11.5f).y) }, tint, style = s)
    }

@Composable
fun IconSettings(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f; val c = g(12f,12f); val or = 8.5f*scale; val ir = 5.5f*scale; val tl = 2f*scale
        for (i in 0 until 8) {
            val a = Math.PI*2*i/8
            drawLine(tint, Offset(c.x+(or-tl/2)*cos(a).toFloat(), c.y+(or-tl/2)*sin(a).toFloat()), Offset(c.x+(or+tl/2)*cos(a).toFloat(), c.y+(or+tl/2)*sin(a).toFloat()), s.width, cap = StrokeCap.Round)
        }
        drawCircle(tint, or-tl/2, c, style = s); drawCircle(tint, 2f*scale, c, style = s)
    }

@Composable
fun IconExtensions(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f; val sq = 8f; val gap = 2f; val st = 4f
        listOf(st to st, st+sq+gap to st, st to st+sq+gap, st+sq+gap to st+sq+gap).forEach { (x,y) ->
            drawRoundRect(tint, g(x,y), androidx.compose.ui.geometry.Size(sq*scale,sq*scale), androidx.compose.ui.geometry.CornerRadius(1.5f*scale), style = s)
        }
    }

@Composable
fun IconTV(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(2.5f,5f), androidx.compose.ui.geometry.Size(19f*scale,14f*scale), androidx.compose.ui.geometry.CornerRadius(2.5f*scale), style = s)
        drawPath(Path().apply { moveTo(g(10f,9f).x,g(10f,9f).y); lineTo(g(16f,12f).x,g(16f,12f).y); lineTo(g(10f,15f).x,g(10f,15f).y); close() }, tint, style = s)
    }

@Composable
fun IconYouTube(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(1.5f,6.5f), androidx.compose.ui.geometry.Size(21f*scale,11f*scale), androidx.compose.ui.geometry.CornerRadius(5.5f*scale), style = s)
        drawPath(Path().apply { moveTo(g(10.5f,9.5f).x,g(10.5f,9.5f).y); lineTo(g(15f,12f).x,g(15f,12f).y); lineTo(g(10.5f,14.5f).x,g(10.5f,14.5f).y); close() }, tint, style = s)
    }

@Composable
fun IconProfile(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 3.2f*scale, g(12f,8f), style = s)
        drawPath(Path().apply { moveTo(g(5f,19.5f).x,g(5f,19.5f).y); quadraticBezierTo(g(12f,12.5f).x,g(12f,12.5f).y,g(19f,19.5f).x,g(19f,19.5f).y) }, tint, style = s)
    }

@Composable
fun IconFav(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        drawPath(Path().apply {
            moveTo(g(12f,19f).x,g(12f,19f).y)
            cubicTo(g(4f,13f).x,g(4f,13f).y,g(3f,8.5f).x,g(3f,8.5f).y,g(6.5f,6.5f).x,g(6.5f,6.5f).y)
            cubicTo(g(9f,5f).x,g(9f,5f).y,g(11f,6.5f).x,g(11f,6.5f).y,g(12f,8f).x,g(12f,8f).y)
            cubicTo(g(13f,6.5f).x,g(13f,6.5f).y,g(15f,5f).x,g(15f,5f).y,g(17.5f,6.5f).x,g(17.5f,6.5f).y)
            cubicTo(g(21f,8.5f).x,g(21f,8.5f).y,g(20f,13f).x,g(20f,13f).y,g(12f,19f).x,g(12f,19f).y); close()
        }, tint, style = afStroke())
    }

// ── Playback icons ────────────────────────────────────────────────────────────

@Composable
fun IconPlay(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier, filled: Boolean = true) =
    IconSlot(iconSize, modifier) {
        val p = Path().apply { moveTo(g(8f,5f).x,g(8f,5f).y); lineTo(g(19f,12f).x,g(19f,12f).y); lineTo(g(8f,19f).x,g(8f,19f).y); close() }
        if (filled) drawPath(p, tint) else drawPath(p, tint, style = afStroke())
    }

@Composable
fun IconPause(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = size.width/24f
        drawRoundRect(tint, g(6f,5f),  androidx.compose.ui.geometry.Size(4f*s,14f*s), androidx.compose.ui.geometry.CornerRadius(1f*s))
        drawRoundRect(tint, g(14f,5f), androidx.compose.ui.geometry.Size(4f*s,14f*s), androidx.compose.ui.geometry.CornerRadius(1f*s))
    }

@Composable
fun IconSkipBack(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = size.width/24f
        drawPath(Path().apply { moveTo(g(17f,5f).x,g(17f,5f).y); lineTo(g(8f,12f).x,g(8f,12f).y); lineTo(g(17f,19f).x,g(17f,19f).y); close() }, tint)
        drawRoundRect(tint, g(5.5f,5f), androidx.compose.ui.geometry.Size(1.6f*s,14f*s), androidx.compose.ui.geometry.CornerRadius(0.6f*s))
    }

@Composable
fun IconSkipFwd(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = size.width/24f
        drawPath(Path().apply { moveTo(g(7f,5f).x,g(7f,5f).y); lineTo(g(16f,12f).x,g(16f,12f).y); lineTo(g(7f,19f).x,g(7f,19f).y); close() }, tint)
        drawRoundRect(tint, g(16.9f,5f), androidx.compose.ui.geometry.Size(1.6f*s,14f*s), androidx.compose.ui.geometry.CornerRadius(0.6f*s))
    }

@Composable
fun IconVolume(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawPath(Path().apply { moveTo(g(4f,10f).x,g(4f,10f).y); lineTo(g(8f,10f).x,g(8f,10f).y); lineTo(g(12.5f,6f).x,g(12.5f,6f).y); lineTo(g(12.5f,18f).x,g(12.5f,18f).y); lineTo(g(8f,14f).x,g(8f,14f).y); lineTo(g(4f,14f).x,g(4f,14f).y); close() }, tint, style = s)
        drawPath(Path().apply { moveTo(g(16f,9f).x,g(16f,9f).y); quadraticBezierTo(g(18.5f,12f).x,g(18.5f,12f).y,g(16f,15f).x,g(16f,15f).y) }, tint, style = s)
        drawPath(Path().apply { moveTo(g(18.2f,6.5f).x,g(18.2f,6.5f).y); quadraticBezierTo(g(22f,12f).x,g(22f,12f).y,g(18.2f,17.5f).x,g(18.2f,17.5f).y) }, tint, style = s)
    }

@Composable
fun IconMenu(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawLine(tint, g(4f,7f), g(20f,7f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(4f,12f), g(20f,12f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(4f,17f), g(20f,17f), s.width, cap = StrokeCap.Round)
    }

@Composable
fun IconInfo(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 9f*scale, g(12f,12f), style = s)
        drawCircle(tint, 1f*scale, g(12f,8f))
        drawLine(tint, g(12f,11f), g(12f,16.5f), s.width, cap = StrokeCap.Round)
    }

@Composable
fun IconAdd(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawLine(tint, g(12f,5f), g(12f,19f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(5f,12f), g(19f,12f), s.width, cap = StrokeCap.Round)
    }

@Composable
fun IconBookmark(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        drawPath(Path().apply { moveTo(g(6f,4f).x,g(6f,4f).y); lineTo(g(18f,4f).x,g(18f,4f).y); lineTo(g(18f,20f).x,g(18f,20f).y); lineTo(g(12f,15.5f).x,g(12f,15.5f).y); lineTo(g(6f,20f).x,g(6f,20f).y); close() }, tint, style = afStroke())
    }

@Composable
fun IconCheck(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        drawPath(Path().apply { moveTo(g(5f,12.5f).x,g(5f,12.5f).y); lineTo(g(10f,17.5f).x,g(10f,17.5f).y); lineTo(g(19f,7f).x,g(19f,7f).y) }, tint, style = afStroke())
    }

@Composable
fun IconClose(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawLine(tint, g(6f,6f), g(18f,18f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(18f,6f), g(6f,18f), s.width, cap = StrokeCap.Round)
    }

@Composable
fun IconStar(tint: Color = AFStarGold, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val cx = 12f; val cy = 12f; val or = 9f; val ir = 3.8f
        val p = Path()
        for (i in 0 until 10) {
            val a = Math.PI/2*3 + Math.PI*i/5; val r = if (i%2==0) or else ir
            val pt = g(cx+r*cos(a).toFloat(), cy+r*sin(a).toFloat())
            if (i==0) p.moveTo(pt.x,pt.y) else p.lineTo(pt.x,pt.y)
        }
        p.close(); drawPath(p, tint)
    }

// ── New icons ─────────────────────────────────────────────────────────────────

/** Sun icon — day/light mode indicator. */
@Composable
fun IconSun(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 4.5f*scale, g(12f,12f), style = s)
        for (i in 0 until 8) {
            val a = (Math.PI*2*i/8).toFloat()
            drawLine(tint, g(12f+6.5f*cos(a),12f+6.5f*sin(a)), g(12f+8.5f*cos(a),12f+8.5f*sin(a)), s.width, cap = StrokeCap.Round)
        }
    }

/** Moon icon — night/dark mode indicator. */
@Composable
fun IconMoon(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawPath(Path().apply {
            moveTo(g(20f,12f).x, g(20f,12f).y)
            quadraticBezierTo(g(20f,18f).x,g(20f,18f).y,g(12f,20f).x,g(12f,20f).y)
            quadraticBezierTo(g(4f,20f).x,g(4f,20f).y,g(4f,12f).x,g(4f,12f).y)
            quadraticBezierTo(g(4f,6f).x,g(4f,6f).y,g(9f,4f).x,g(9f,4f).y)
            quadraticBezierTo(g(14f,4f).x,g(14f,4f).y,g(14f,8f).x,g(14f,8f).y)
            quadraticBezierTo(g(14f,12f).x,g(14f,12f).y,g(20f,12f).x,g(20f,12f).y)
            close()
        }, tint, style = s)
    }

/** OLED/Display icon — circle with inner bars. */
@Composable
fun IconOled(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 9f*scale, g(12f,12f), style = s)
        drawLine(tint, g(9f,9f), g(9f,15f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(12f,8f), g(12f,16f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(15f,9f), g(15f,15f), s.width, cap = StrokeCap.Round)
    }

/** HDR badge icon — H·D·R in minimal strokes. */
@Composable
fun IconHdr(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        // H
        drawLine(tint, g(3f,7f), g(3f,17f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(3f,12f), g(7f,12f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(7f,7f), g(7f,17f), s.width, cap = StrokeCap.Round)
        // D
        drawLine(tint, g(9.5f,7f), g(9.5f,17f), s.width, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(g(9.5f,7f).x,g(9.5f,7f).y); quadraticBezierTo(g(14f,7f).x,g(14f,7f).y,g(14f,12f).x,g(14f,12f).y); quadraticBezierTo(g(14f,17f).x,g(14f,17f).y,g(9.5f,17f).x,g(9.5f,17f).y) }, tint, style = s)
        // R
        drawLine(tint, g(16.5f,7f), g(16.5f,17f), s.width, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(g(16.5f,7f).x,g(16.5f,7f).y); quadraticBezierTo(g(21f,7f).x,g(21f,7f).y,g(21f,12f).x,g(21f,12f).y); quadraticBezierTo(g(21f,12f).x,g(21f,12f).y,g(16.5f,12f).x,g(16.5f,12f).y); lineTo(g(21f,17f).x,g(21f,17f).y) }, tint, style = s)
    }

/** Equalizer bars — audio/EQ icon. */
@Composable
fun IconEqualizer(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        val heights = listOf(10f,14f,8f,16f,12f)
        heights.forEachIndexed { i, h ->
            val x = 3.5f + i * 4f; val top = (20f - h) / 2f
            drawRoundRect(tint, g(x,top), androidx.compose.ui.geometry.Size(2.5f*scale,h*scale), androidx.compose.ui.geometry.CornerRadius(1f*scale), style = s)
        }
    }

/** Colour palette — theme/skin picker. */
@Composable
fun IconPalette(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 8f*scale, g(12f,12f), style = s)
        listOf(7f to 8f, 12f to 6f, 16f to 10f, 16f to 15f, 12f to 18f).forEachIndexed { _, (x,y) ->
            drawCircle(tint, 1.5f*scale, g(x,y))
        }
    }

/** Wand — magic/AI icon. */
@Composable
fun IconWand(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawLine(tint, g(4f,20f), g(20f,4f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(9f,4f), g(9f,2f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(14f,9f), g(16f,9f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(6.5f,6.5f), g(5f,5f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(11f,4f), g(13f,2f), s.width, cap = StrokeCap.Round)
    }

/** Cast / Chromecast icon. */
@Composable
fun IconCast(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(2f,6f), androidx.compose.ui.geometry.Size(20f*scale,14f*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        drawPath(Path().apply { moveTo(g(2f,17f).x,g(2f,17f).y); quadraticBezierTo(g(7f,17f).x,g(7f,17f).y,g(7f,22f).x,g(7f,22f).y) }, tint, style = s)
        drawPath(Path().apply { moveTo(g(2f,13f).x,g(2f,13f).y); quadraticBezierTo(g(11f,13f).x,g(11f,13f).y,g(11f,22f).x,g(11f,22f).y) }, tint, style = s)
        drawLine(tint, g(2f,20f), g(2f,22f), s.width, cap = StrokeCap.Round)
    }

/** Subtitles / CC icon. */
@Composable
fun IconSubtitles(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(2f,5f), androidx.compose.ui.geometry.Size(20f*scale,14f*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        drawLine(tint, g(5f,12f), g(11f,12f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(13f,12f), g(19f,12f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(5f,15f), g(9f,15f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(11f,15f), g(19f,15f), s.width, cap = StrokeCap.Round)
    }

/** Server/network source icon. */
@Composable
fun IconServer(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawRoundRect(tint, g(2f,3f), androidx.compose.ui.geometry.Size(20f*scale,6f*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        drawRoundRect(tint, g(2f,11f), androidx.compose.ui.geometry.Size(20f*scale,6f*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        drawRoundRect(tint, g(2f,19f), androidx.compose.ui.geometry.Size(20f*scale,4f*scale), androidx.compose.ui.geometry.CornerRadius(2f*scale), style = s)
        drawCircle(tint, 1f*scale, g(18f,6f))
        drawCircle(tint, 1f*scale, g(18f,14f))
    }

// ── Additional hub icons (AF3 spec) ───────────────────────────────────────────

/** Clock face — History hub. */
@Composable
fun IconClock(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke(); val scale = size.width/24f
        drawCircle(tint, 8f*scale, g(12f,12f), style = s)
        drawLine(tint, g(12f,12f), g(12f,6f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(12f,12f), g(16f,12f), s.width, cap = StrokeCap.Round)
        drawCircle(tint, 1.5f*scale, g(12f,12f))
    }

/** Puzzle piece — Add-ons hub. */
@Composable
fun IconPuzzle(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawRoundRect(tint, g(3f,7f), androidx.compose.ui.geometry.Size(9f,9f), androidx.compose.ui.geometry.CornerRadius(2f), style = s)
        drawRoundRect(tint, g(13f,3f), androidx.compose.ui.geometry.Size(9f,9f), androidx.compose.ui.geometry.CornerRadius(2f), style = s)
        drawRoundRect(tint, g(13f,13f), androidx.compose.ui.geometry.Size(9f,9f), androidx.compose.ui.geometry.CornerRadius(2f), style = s)
        drawLine(tint, g(7f,12f), g(13f,12f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(12f,7f), g(12f,13f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(14f,8f), g(15f,9f), s.width, cap = StrokeCap.Round)
    }

/** Lightning bolt — Debrid hub. */
@Composable
fun IconZap(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawPath(Path().apply {
            moveTo(g(14f,2f).x,g(14f,2f).y); lineTo(g(5f,13f).x,g(5f,13f).y); lineTo(g(11f,13f).x,g(11f,13f).y)
            lineTo(g(10f,22f).x,g(10f,22f).y); lineTo(g(19f,11f).x,g(19f,11f).y); lineTo(g(13f,11f).x,g(13f,11f).y); close()
        }, tint, style = s)
    }

/** Floppy / save disk — Backup hub. */
@Composable
fun IconSave(tint: Color = AFText, iconSize: Dp = 24.dp, modifier: Modifier = Modifier) =
    IconSlot(iconSize, modifier) {
        val s = afStroke()
        drawRoundRect(tint, g(3f,4f), androidx.compose.ui.geometry.Size(18f,16f), androidx.compose.ui.geometry.CornerRadius(2f), style = s)
        drawRoundRect(tint, g(6f,2f), androidx.compose.ui.geometry.Size(12f,5f), androidx.compose.ui.geometry.CornerRadius(1f), style = s)
        drawLine(tint, g(3f,10f), g(21f,10f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(3f,14f), g(21f,14f), s.width, cap = StrokeCap.Round)
        drawLine(tint, g(12f,10f), g(12f,14f), s.width, cap = StrokeCap.Round)
    }

