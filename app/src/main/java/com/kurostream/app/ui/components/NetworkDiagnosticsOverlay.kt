package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NetworkDiagnosticsOverlay(
    visible: Boolean,
    downloadSpeedMbps: Double,
    latencyMs: Double,
    bufferHealthMs: Long,
    resolution: String,
    codec: String
) {
    if (!visible) return
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("Network", color = Color.Cyan, fontSize = 14.sp)
        Text("↓ ${String.format("%.1f", downloadSpeedMbps)} Mbps", color = Color.White, fontSize = 12.sp)
        Text("Latency: ${String.format("%.0f", latencyMs)} ms", color = Color.White, fontSize = 12.sp)
        Text("Buffer: ${bufferHealthMs}ms", color = Color.White, fontSize = 12.sp)
        Text("Video: $resolution ($codec)", color = Color.White, fontSize = 12.sp)
    }
}
