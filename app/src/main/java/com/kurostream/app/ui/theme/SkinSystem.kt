package com.kurostream.app.ui.theme

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Skin(
    val label: String,
    val description: String,
) {
    AMOLED_BLACK("AMOLED Black", "Pure black background — best battery life"),
    DEEP_PURPLE("Deep Purple", "Dark purple tones with subtle gradient"),
    OCEAN_BLUE("Ocean Blue", "Deep navy to teal gradient"),
    FOREST_GREEN("Forest Green", "Rich emerald and dark green tones"),
    CHERRY_BLOSSOM("Cherry Blossom", "Animated falling cherry blossom petals"),
    STARRY_NIGHT("Starry Night", "Twinkling stars on a deep blue sky"),
}

fun Skin.colorPalette(): DynamicColorPalette = when (this) {
    Skin.AMOLED_BLACK -> generateAmoledBlackScheme().toDynamicPalette()
    Skin.DEEP_PURPLE -> TvDarkColorScheme.toDynamicPalette()
    Skin.OCEAN_BLUE -> DynamicColorPalette(
        primary = Color(0xFF448AFF), onPrimary = Color.White,
        primaryContainer = Color(0xFF448AFF).copy(alpha = 0.15f), onPrimaryContainer = Color(0xFF448AFF),
        secondary = Color(0xFF00BCD4), onSecondary = Color.Black,
        secondaryContainer = Color(0xFF00BCD4).copy(alpha = 0.15f), onSecondaryContainer = Color(0xFF00BCD4),
        tertiary = Color(0xFF26C6DA), onTertiary = Color.Black,
        background = Color(0xFF0A0E1A), onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF111827), onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF1A2332), onSurfaceVariant = Color(0xFFB0BEC5),
    )
    Skin.FOREST_GREEN -> DynamicColorPalette(
        primary = Color(0xFF4CAF50), onPrimary = Color.White,
        primaryContainer = Color(0xFF4CAF50).copy(alpha = 0.15f), onPrimaryContainer = Color(0xFF4CAF50),
        secondary = Color(0xFF8BC34A), onSecondary = Color.Black,
        secondaryContainer = Color(0xFF8BC34A).copy(alpha = 0.15f), onSecondaryContainer = Color(0xFF8BC34A),
        tertiary = Color(0xFFCDDC39), onTertiary = Color.Black,
        background = Color(0xFF0A120A), onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF0F1A0F), onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF1A2A1A), onSurfaceVariant = Color(0xFFB0C0B0),
    )
    Skin.CHERRY_BLOSSOM -> DynamicColorPalette(
        primary = Color(0xFFE91E8C), onPrimary = Color.White,
        primaryContainer = Color(0xFFE91E8C).copy(alpha = 0.15f), onPrimaryContainer = Color(0xFFE91E8C),
        secondary = Color(0xFFFF6B9D), onSecondary = Color.Black,
        secondaryContainer = Color(0xFFFF6B9D).copy(alpha = 0.15f), onSecondaryContainer = Color(0xFFFF6B9D),
        tertiary = Color(0xFFFFAB91), onTertiary = Color.Black,
        background = Color(0xFF1A0E14), onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF24141C), onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF301E28), onSurfaceVariant = Color(0xFFD0B0C0),
    )
    Skin.STARRY_NIGHT -> DynamicColorPalette(
        primary = Color(0xFF7C4DFF), onPrimary = Color.White,
        primaryContainer = Color(0xFF7C4DFF).copy(alpha = 0.15f), onPrimaryContainer = Color(0xFF7C4DFF),
        secondary = Color(0xFF536DFE), onSecondary = Color.White,
        secondaryContainer = Color(0xFF536DFE).copy(alpha = 0.15f), onSecondaryContainer = Color(0xFF536DFE),
        tertiary = Color(0xFF448AFF), onTertiary = Color.White,
        background = Color(0xFF05070F), onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF0A0D1A), onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF141A2E), onSurfaceVariant = Color(0xFFB0B8D0),
    )
}

fun Skin.backgroundColor(): Color = colorPalette().background

@Composable
fun SkinBackground(
    skin: Skin,
    modifier: Modifier = Modifier,
    reduceMotionEnabled: Boolean = false,
    isLowRamDevice: Boolean = false,
) {
    when (skin) {
        Skin.AMOLED_BLACK, Skin.DEEP_PURPLE -> {}
        Skin.OCEAN_BLUE -> GradientBackground(
            listOf(Color(0xFF0A0E1A), Color(0xFF0D2137), Color(0xFF0A1628)),
            modifier,
        )
        Skin.FOREST_GREEN -> GradientBackground(
            listOf(Color(0xFF0A120A), Color(0xFF0F1F0F), Color(0xFF0A150A)),
            modifier,
        )
        Skin.CHERRY_BLOSSOM -> {
            if (!reduceMotionEnabled && !isLowRamDevice) {
                ParticleBackground(type = skin, count = 30, modifier = modifier)
            }
        }
        Skin.STARRY_NIGHT -> {
            val starCount = when {
                reduceMotionEnabled -> 0
                isLowRamDevice -> 20
                else -> 80
            }
            if (starCount > 0) {
                ParticleBackground(type = skin, count = starCount, modifier = modifier)
            }
        }
    }
}

@Composable
private fun GradientBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(colors),
            size = size,
        )
    }
}

private data class Particle(
    var x: Float, var y: Float, var size: Float,
    var alpha: Float, var rotation: Float, var rotationSpeed: Float,
    var dx: Float, var dy: Float, var swayPhase: Float, var swayAmplitude: Float,
)

@Composable
private fun ParticleBackground(type: Skin, count: Int, modifier: Modifier = Modifier) {
    val particles = remember {
        val rng = Random(type.ordinal)
        mutableStateListOf(*Array(count) {
            Particle(
                x = rng.nextFloat(), y = rng.nextFloat(),
                size = if (type == Skin.STARRY_NIGHT) rng.nextFloat() * 3f + 1f
                       else rng.nextFloat() * 12f + 6f,
                alpha = if (type == Skin.STARRY_NIGHT) rng.nextFloat() * 0.8f + 0.2f
                        else rng.nextFloat() * 0.5f + 0.3f,
                rotation = rng.nextFloat() * 360f,
                rotationSpeed = if (type == Skin.STARRY_NIGHT) 0f
                                else (rng.nextFloat() - 0.5f) * 4f,
                dx = if (type == Skin.STARRY_NIGHT) 0f else (rng.nextFloat() - 0.5f) * 0.4f,
                dy = if (type == Skin.STARRY_NIGHT) 0f else rng.nextFloat() * 0.6f + 0.3f,
                swayPhase = rng.nextFloat() * 6.28f,
                swayAmplitude = if (type == Skin.STARRY_NIGHT) 0f else rng.nextFloat() * 40f + 20f,
            )
        })
    }

    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            val frameMs = withInfiniteAnimationFrameMillis { it }
            val dt = ((frameMs - lastFrameMs).coerceAtMost(50)) / 1000f
            lastFrameMs = frameMs
            val rng = Random(type.ordinal)
            particles.forEach { p ->
                p.x += p.dx * dt
                p.y += p.dy * dt
                p.rotation += p.rotationSpeed
                if (p.y > 1.1f) { p.y = -0.1f; p.x = rng.nextFloat(); p.rotation = rng.nextFloat() * 360f }
                if (p.x < -0.1f) p.x = 1.1f
                if (p.x > 1.1f) p.x = -0.1f
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val twinkle = (sin(System.nanoTime() * 3e-9) + 1f) / 2f
        particles.forEach { p ->
            val sway = sin(p.swayPhase + frameTime * 0.002f) * p.swayAmplitude
            val cx = p.x * w + sway
            val cy = p.y * h
            when (type) {
                Skin.STARRY_NIGHT -> {
                    val a = p.alpha * (0.3f + 0.7f * twinkle)
                    drawCircle(color = Color.White.copy(alpha = a), radius = p.size, center = Offset(cx, cy))
                    if (p.size > 2f) {
                        drawCircle(color = Color.White.copy(alpha = a * 0.2f), radius = p.size * 3f, center = Offset(cx, cy))
                    }
                }
                Skin.CHERRY_BLOSSOM -> drawBlossom(cx, cy, p.size, p.alpha, p.rotation)
                else -> {}
            }
        }
    }
}

private val frameTime: Long get() = System.nanoTime()

private fun DrawScope.drawBlossom(cx: Float, cy: Float, size: Float, alpha: Float, rotation: Float) {
    val petalColor = Color(0xFFFFB7C5).copy(alpha = alpha)
    val centerColor = Color(0xFFFFE082).copy(alpha = alpha)
    for (i in 0 until 5) {
        val angle = Math.toRadians((rotation + i * 72.0).toDouble())
        val px = cx + size * 0.3f * cos(angle).toFloat()
        val py = cy + size * 0.3f * sin(angle).toFloat()
        drawOval(
            color = petalColor,
            topLeft = Offset(px, py),
            size = androidx.compose.ui.geometry.Size(size * 0.5f, size * 0.25f),
        )
    }
    drawCircle(color = centerColor, radius = size * 0.12f, center = Offset(cx, cy))
}
