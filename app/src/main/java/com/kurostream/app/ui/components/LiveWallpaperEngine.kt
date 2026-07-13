package com.kurostream.app.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class LiveWallpaperType(val label: String) {
    CHERRY_BLOSSOM("Cherry Blossoms"), FALLING_LEAVES("Falling Leaves")
}

private data class Particle(
    var x: Float, var y: Float, var size: Float, var alpha: Float,
    var rotation: Float, var rotationSpeed: Float, var dx: Float, var dy: Float,
    var swayPhase: Float, var swayAmplitude: Float,
)

private fun particleSize(type: LiveWallpaperType) = when (type) {
    LiveWallpaperType.CHERRY_BLOSSOM -> Random.nextFloat() * 8f + 4f
    LiveWallpaperType.FALLING_LEAVES -> Random.nextFloat() * 12f + 6f
}

private fun randomParticle(type: LiveWallpaperType, width: Float) = Particle(
    x = Random.nextFloat() * width, y = -Random.nextFloat() * 500f, size = particleSize(type),
    alpha = 0.3f, rotation = Random.nextFloat() * 360f, rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
    dx = (Random.nextFloat() - 0.5f) * 0.5f, dy = Random.nextFloat() * 1.5f + 0.5f,
    swayPhase = Random.nextFloat() * 360f, swayAmplitude = Random.nextFloat() * 0.5f + 0.2f,
)

@Composable
fun LiveWallpaperView(
    wallpaperType: LiveWallpaperType,
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
) {
    val particles = remember(wallpaperType, particleCount) {
        List(particleCount) { randomParticle(wallpaperType, 2000f) }
    }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(wallpaperType) {
        while (true) withInfiniteAnimationFrameMillis { _ ->
            particles.forEach { p ->
                p.x += p.dx; p.y += p.dy; p.rotation += p.rotationSpeed
                p.swayPhase += 0.05f; p.dx += cos(p.swayPhase) * p.swayAmplitude * 0.01f; p.dx *= 0.98f
                p.alpha = (p.alpha + 0.005f).coerceAtMost(1f)
            }; frame++
        }
    }
    Canvas(modifier = modifier) {
        frame.let { }
        particles.forEach { p ->
            if (p.y > size.height + p.size) {
                val s = particleSize(wallpaperType)
                p.x = Random.nextFloat() * size.width; p.y = -s; p.size = s; p.alpha = 0.3f
                p.rotation = Random.nextFloat() * 360f; p.rotationSpeed = (Random.nextFloat() - 0.5f) * 2f
                p.dx = (Random.nextFloat() - 0.5f) * 0.5f; p.dy = Random.nextFloat() * 1.5f + 0.5f
                p.swayPhase = Random.nextFloat() * 360f; p.swayAmplitude = Random.nextFloat() * 0.5f + 0.2f
            }
            drawParticle(p, wallpaperType)
        }
    }
}

private fun DrawScope.drawParticle(p: Particle, type: LiveWallpaperType) {
    val c = when (type) {
        LiveWallpaperType.CHERRY_BLOSSOM -> Color(1f, .5f + p.alpha * .3f, .6f + p.alpha * .2f, p.alpha * .5f)
        LiveWallpaperType.FALLING_LEAVES -> Color(.5f + p.alpha * .4f, .3f + p.alpha * .5f, .1f, p.alpha * .6f)
    }
    rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        if (type == LiveWallpaperType.CHERRY_BLOSSOM) {
            repeat(5) { i ->
                val a = (i * 72).toFloat() * 0.0174533f
                drawCircle(c, p.size * .6f, Offset(p.x + cos(a) * p.size * .5f, p.y + sin(a) * p.size * .5f))
            }
            drawCircle(Color(0xFFFFD700).copy(alpha = p.alpha * .4f), p.size * .3f, Offset(p.x, p.y))
        } else {
            drawCircle(c, p.size, Offset(p.x, p.y))
            drawCircle(c.copy(alpha = c.alpha * .4f), p.size * .6f, Offset(p.x - p.size * .3f, p.y - p.size * .3f))
        }
    }
}
