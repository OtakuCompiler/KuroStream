package com.kurostream.players.health

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbar4Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.SignalWifiStatusbarNull
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.players.core.PlaybackDiagnostics
import com.kurostream.torrent.streaming.StreamingTorrentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamHealthMonitorOverlay(
    streamingManager: StreamingTorrentManager,
    infoHash: String,
    diagnostics: PlaybackDiagnostics,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    val streamState by streamingManager.getStreamState(infoHash).collectAsStateWithLifecycle()
    val bufferHealth = streamState?.bufferHealth
    val globalHealth = streamingManager.globalStreamHealth.collectAsStateWithLifecycle()
    val diagnosticsState = diagnostics

    var expanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val alpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
    )

    if (!isVisible && alpha.value == 0f) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!expanded) {
                CompactHealthView(
                    bufferHealth = bufferHealth,
                    globalHealth = globalHealth.value,
                    diagnostics = diagnosticsState,
                    onExpand = { expanded = true },
                )
            } else {
                ExpandedHealthView(
                    bufferHealth = bufferHealth,
                    globalHealth = globalHealth.value,
                    diagnostics = diagnosticsState,
                    streamState = streamState,
                    onCollapse = { expanded = false },
                    onToggleDetails = { showDetails = !showDetails },
                    showDetails = showDetails,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactHealthView(
    bufferHealth: StreamingTorrentManager.BufferHealth?,
    globalHealth: StreamingTorrentManager.StreamHealth,
    diagnostics: PlaybackDiagnostics,
    onExpand: () -> Unit,
) {
    val bufferPercent = bufferHealth?.bufferPercentage ?: 0f
    val isUnderrun = bufferHealth?.isUnderrun == true
    val downloadSpeed = bufferHealth?.downloadSpeedBps ?: globalHealth.totalDownloadSpeed
    val bufferColor = when {
        bufferPercent >= 0.5f -> Color.Green
        bufferPercent >= 0.2f -> Color.Yellow
        else -> Color.Red
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(100.dp)
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📡 Stream Health",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "${(bufferPercent * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = bufferColor,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SpeedIndicator(speed = downloadSpeed)
                    if (isUnderrun) {
                        UnderrunIndicator(consecutive = bufferHealth?.consecutiveUnderruns ?: 0)
                    }
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = formatSpeed(downloadSpeed),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                BufferProgressBar(
                    progress = bufferPercent,
                    color = bufferColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Color.White,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedHealthView(
    bufferHealth: StreamingTorrentManager.BufferHealth?,
    globalHealth: StreamingTorrentManager.StreamHealth,
    diagnostics: PlaybackDiagnostics,
    streamState: StreamingTorrentManager.StreamState?,
    onCollapse: () -> Unit,
    onToggleDetails: () -> Unit,
    showDetails: Boolean,
) {
    Column(modifier = Modifier.width(320.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stream Diagnostics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onToggleDetails) {
                            Icon(
                                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle details",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = onCollapse) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                            )
                        }
                    }
                }

                if (bufferHealth != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthMetricRow(
                            label = "Buffer",
                            value = "${(bufferHealth.bufferPercentage * 100).toInt()}%",
                            subtitle = "${formatDuration(bufferHealth.bufferedMs)} / ${formatDuration(bufferHealth.bufferedMs + (bufferHealth.downloadSpeedBps * 1000 / maxOf(1, bufferHealth.piecesNeeded)))}",
                            icon = Icons.Default.SignalWifiStatusbar4Bar,
                            valueColor = when {
                                bufferHealth.bufferPercentage >= 0.5f -> Color.Green
                                bufferHealth.bufferPercentage >= 0.2f -> Color.Yellow
                                else -> Color.Red
                            }
                        )

                        HealthMetricRow(
                            label = "Download Speed",
                            value = formatSpeed(bufferHealth.downloadSpeedBps),
                            subtitle = "${bufferHealth.piecesAhead} pieces ahead, ${bufferHealth.piecesNeeded} needed",
                            icon = Icons.Default.Speed,
                        )

                        HealthMetricRow(
                            label = "Swarm Health",
                            value = "${globalHealth.healthyStreams}/${globalHealth.activeStreams} healthy",
                            subtitle = "Peers: ${streamState?.pieceRequests?.size ?: 0} | Seeds: ${globalHealth.activeStreams}",
                            icon = Icons.Default.SignalWifi4Bar,
                        )

                        if (bufferHealth.isUnderrun) {
                            HealthMetricRow(
                                label = "⚠ Buffer Underrun",
                                value = "${bufferHealth.consecutiveUnderruns} consecutive",
                                subtitle = "Retry ${streamState?.totalRetries ?: 0}/${StreamingTorrentManager.MAX_RETRIES}",
                                icon = Icons.Default.Warning,
                                valueColor = Color.Red,
                            )
                        }

                        if (showDetails) {
                            Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            DetailedDiagnosticsSection(diagnostics = diagnostics, streamState = streamState)
                        }
                    }
                } else {
                    Text(
                        text = "No active torrent stream",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetricRow(
    label: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.material.icons.filled.SignalWifiStatusbar4Bar,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
                Text(text = subtitle, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SpeedIndicator(speed: Long) {
    val speedKbps = speed / 1024
    val color = when {
        speedKbps >= 5000 -> Color.Green
        speedKbps >= 1000 -> Color.Yellow
        else -> Color.Red
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = "",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = formatSpeed(speed),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun UnderrunIndicator(consecutive: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "",
            tint = Color.Red,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$consecutive underruns",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
        )
    }
}

@Composable
private fun BufferProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width((progress * 100).toInt().coerceIn(0, 100).toFloat().dp)
                .background(color, RoundedCornerShape(3.dp))
                .animateContentSize()
        )
    }
}

@Composable
private fun DetailedDiagnosticsSection(
    diagnostics: PlaybackDiagnostics,
    streamState: StreamingTorrentManager.StreamState?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Playback Diagnostics",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f),
        )

        DiagnosticRow("Video Codec", diagnostics.videoCodec)
        DiagnosticRow("Audio Codec", diagnostics.audioCodec)
        DiagnosticRow("Resolution", diagnostics.videoResolution)
        DiagnosticRow("Decoder", diagnostics.decoderName)
        DiagnosticRow("HW Decoding", if (diagnostics.isHardwareDecoding) "Yes" else "No")
        DiagnosticRow("Dropped Frames", diagnostics.droppedFrames.toString())
        DiagnosticRow("Rendered Frames", diagnostics.renderedFrames.toString())
        DiagnosticRow("Current FPS", String.format("%.1f", diagnostics.currentFps))
        DiagnosticRow("Display Refresh", String.format("%.1f Hz", diagnostics.displayRefreshRate))
        DiagnosticRow("Content FPS", String.format("%.1f", diagnostics.contentFrameRate))
        DiagnosticRow("Bitrate", formatBitrate(diagnostics.currentBitrate))
        DiagnosticRow("Network Speed", formatBitrate(diagnostics.networkSpeedBps))

        if (streamState != null) {
            Text(
                text = "Torrent Stream",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
            DiagnosticRow("Pieces Requested", streamState.pieceRequests.size.toString())
            DiagnosticRow("Pieces Completed", streamState.pieceRequests.count { it.completed }.toString())
            DiagnosticRow("Total Retries", streamState.totalRetries.toString())
            DiagnosticRow("Fallback Triggered", if (streamState.fallbackTriggered) "Yes" else "No")
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Divider(color: Color, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.height(1.dp).background(color)
    )
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "${bytesPerSec} B/s"
        bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
    }
}

private fun formatBitrate(bps: Long): String {
    return when {
        bps < 1000 -> "${bps} bps"
        bps < 1_000_000 -> String.format("%.1f Kbps", bps / 1000.0)
        bps < 1_000_000_000 -> String.format("%.1f Mbps", bps / 1_000_000.0)
        else -> String.format("%.1f Gbps", bps / 1_000_000_000.0)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "%d:%02d".format(min, sec) else "%ds".format(sec)
}