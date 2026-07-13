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

package com.kurostream.app.ui.wallpaper

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveWallpaper(
    type: WallpaperType,
    modifier: Modifier = Modifier
) {
    when (type) {
        WallpaperType.CHERRY_BLOSSOMS -> CherryBlossomWallpaper(modifier)
        WallpaperType.LEAVES -> FallingLeavesWallpaper(modifier)
        WallpaperType.STARS -> StarryNightWallpaper(modifier)
        WallpaperType.RAIN -> RainWallpaper(modifier)
    }
}

enum class WallpaperType {
    CHERRY_BLOSSOMS,
    LEAVES,
    STARS,
    RAIN
}

@Composable
private fun CherryBlossomWallpaper(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val skyGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFB7C5),
                Color(0xFFFFD1DC),
                Color(0xFFFFE4E1)
            )
        )
        drawRect(skyGradient)
        
        for (i in 0 until 50) {
            val x = ((i * 73) % 100) / 100f * size.width
            val baseY = ((i * 37) % 100) / 100f * size.height * 0.6f
            
            val windOffset = sin(time * 0.1 + i * 0.5) * 30f
            val fallProgress = (time + i * 2) % 20f / 20f
            
            val petalX = x + windOffset * fallProgress
            val petalY = baseY + fallProgress * size.height * 0.4f
            
            drawPetal(petalX, petalY, time + i)
        }
    }
}

private fun DrawScope.drawPetal(x: Float, y: Float, rotation: Float) {
    rotate(rotation * 2) {
        val petalColor = Color(0xFFFFB7C5).copy(alpha = 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    petalColor,
                    petalColor.copy(alpha = 0.3f)
                ),
                center = Offset(x, y),
                radius = 8.dp.toPx()
            ),
            radius = 8.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

@Composable
private fun FallingLeavesWallpaper(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "leaves")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val autumnGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF8C42),
                Color(0xFFFFA94D),
                Color(0xFFFFD93D)
            )
        )
        drawRect(autumnGradient)
        
        for (i in 0 until 40) {
            val x = ((i * 67) % 100) / 100f * size.width
            const val baseY = -50f
            val fallProgress = (time + i * 2.5f) % 25f / 25f
            
            val windOffset = cos(time * 0.08 + i * 0.3) * 50f
            val leafX = x + windOffset * fallProgress
            val leafY = baseY + fallProgress * (size.height + 100f)
            
            drawLeaf(leafX, leafY, time + i * 1.5f, i % 3)
        }
    }
}

private fun DrawScope.drawLeaf(x: Float, y: Float, rotation: Float, colorIndex: Int) {
    val leafColors = listOf(
        Color(0xFFFF6B35),
        Color(0xFFF7C548),
        Color(0xFFD4A574)
    )
    val leafColor = leafColors[colorIndex].copy(alpha = 0.8f)
    
    rotate(rotation * 3) {
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(
                    leafColor,
                    leafColor.copy(alpha = 0.4f)
                ),
                start = Offset(x - 10.dp.toPx(), y - 5.dp.toPx()),
                end = Offset(x + 10.dp.toPx(), y + 5.dp.toPx())
            ),
            topLeft = Offset(x - 10.dp.toPx(), y - 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 10.dp.toPx())
        )
    }
}

@Composable
private fun StarryNightWallpaper(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val nightGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D0D1A),
                Color(0xFF1A1A2E),
                Color(0xFF16213E)
            )
        )
        drawRect(nightGradient)
        
        for (i in 0 until 100) {
            val x = ((i * 137) % 100) / 100f * size.width
            val y = ((i * 53) % 100) / 100f * size.height * 0.7f
            
            val twinkle = (sin(time * 0.2 + i * 0.1) + 1) / 2
            val starBrightness = 0.3f + twinkle * 0.7f
            
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = starBrightness),
                radius = (1 + twinkle * 2).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun RainWallpaper(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val rainGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1A2E),
                Color(0xFF16213E),
                Color(0xFF0F3460)
            )
        )
        drawRect(rainGradient)
        
        for (i in 0 until 80) {
            val x = ((i * 47) % 100) / 100f * size.width
            val dropProgress = (time + i * 1.25f) % 10f / 10f
            
            val dropX = x + dropProgress * 10f
            val dropY = (dropProgress * (size.height + 100f)) - 50f
            
            drawRaindrop(dropX, dropY, dropProgress)
        }
    }
}

private fun DrawScope.drawRaindrop(x: Float, y: Float, progress: Float) {
    val alpha = 0.3f + (1 - progress) * 0.4f
    drawLine(
        color = Color(0xFFA8D5E5).copy(alpha = alpha),
        start = Offset(x, y - 20.dp.toPx()),
        end = Offset(x, y),
        strokeWidth = 2.dp.toPx()
    )
}