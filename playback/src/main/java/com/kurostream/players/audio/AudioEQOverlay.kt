package com.kurostream.players.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioEQOverlay(
    bandGains: FloatArray,
    isVisible: Boolean = true,
    onBandGainChanged: (Int, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val frequencies = listOf("32", "64", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
    val maxGain = bandGains.maxOrNull() ?: 0f
    val minGain = bandGains.minOrNull() ?: 0f

    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC000000)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("10-Band Equalizer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                frequencies.forEachIndexed { index, freq ->
                    EqualizerBand(
                        frequency = freq,
                        gain = bandGains.getOrElse(index) { 0f },
                        onGainChanged = { onBandGainChanged(index, it) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Gain: ${maxGain.toInt()}dB / ${minGain.toInt()}dB", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun EqualizerBand(
    frequency: String,
    gain: Float,
    onGainChanged: (Float) -> Unit,
) {
    val animatedGain by animateFloatAsState(targetValue = (gain + 10) / 20f, label = "gain")
    val gainColor = when {
        gain > 0 -> Color.Green
        gain < 0 -> Color.Red
        else -> Color.White
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Box(
            modifier = Modifier.width(32.dp).height(120.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(modifier = Modifier.fillMaxHeight()) {
                val barHeight = size.height * animatedGain
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Green.copy(alpha = 0.8f), Color.Yellow.copy(alpha = 0.6f), Color.Red.copy(alpha = 0.4f)),
                        startY = size.height,
                        endY = 0f,
                    ),
                    topLeft = Offset(0f, size.height - barHeight),
                    size = size.copy(height = barHeight),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(0f, size.height * 0.5f),
                    end = Offset(size.width, size.height * 0.5f),
                    strokeWidth = 1f,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(frequency, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
        Text("${gain.toInt()}dB", fontSize = 9.sp, color = gainColor)
    }
}
