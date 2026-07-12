package com.kurostream.players.overlay

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.SignalWifiStatusbar1Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbar2Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbar3Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

@Composable
fun StreamHealthMonitorOverlay(
    streamHealth: com.kurostream.torrent.streaming.StreamingTorrentManager.StreamHealth,
    bufferHealth: com.kurostream.torrent.streaming.StreamingTorrentManager.BufferHealth?,
    fallbackState: com.kurostream.torrent.streaming.HttpFallbackManager.FallbackState?,
    isVisible: Boolean = true,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    if (!isVisible) return

    val bufferPercent = bufferHealth?.bufferPercentage ?: 0f
    val downloadSpeed = bufferHealth?.downloadSpeedBps ?: 0L
    val piecesAhead = bufferHealth?.piecesAhead ?: 0
    val piecesNeeded = bufferHealth?.piecesNeeded ?: 0
    val isUnderrun = bufferHealth?.isUnderrun == true
    val consecutiveUnderruns = bufferHealth?.consecutiveUnderruns ?: 0

    val bufferColor = when {
        bufferPercent >= 0.7f -> Color.Green
        bufferPercent >= 0.3f -> Color.Yellow
        bufferPercent >= 0.1f -> Color.Orange
        else -> Color.Red
    }

    val speedColor = when {
        downloadSpeed >= 2_000_000 -> Color.Green
        downloadSpeed >= 1_000_000 -> Color.Yellow
        downloadSpeed >= 500_000 -> Color.Orange
        else -> Color.Red
    }

    val swarmHealth = streamHealth.avgBufferHealth

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(if (expanded) 280.dp else 120.dp)
            .animateFloatAsState(targetValue = if (expanded) 1f else 0f, label = "expansion"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC000000),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Stream Health",
                            tint = if (isUnderrun) Color.Red else Color.Green,
                        )
                        Text(
                            text = "Stream Health",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        if (isUnderrun) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, RoundedCornerShape(4.dp)),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (fallbackState?.isFallbackActive == true) {
                            FallbackBadge()
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                if (!expanded) {
                    CompactView(
                        bufferPercent = bufferPercent,
                        bufferColor = bufferColor,
                        downloadSpeed = downloadSpeed,
                        speedColor = speedColor,
                        swarmHealth = swarmHealth,
                        piecesAhead = piecesAhead,
                        piecesNeeded = piecesNeeded,
                        isUnderrun = isUnderrun,
                        consecutiveUnderruns = consecutiveUnderruns,
                    )
                } else {
                    ExpandedView(
                        bufferPercent = bufferPercent,
                        bufferColor = bufferColor,
                        downloadSpeed = downloadSpeed,
                        speedColor = speedColor,
                        swarmHealth = swarmHealth,
                        piecesAhead = piecesAhead,
                        piecesNeeded = piecesNeeded,
                        isUnderrun = isUnderrun,
                        consecutiveUnderruns = consecutiveUnderruns,
                        streamHealth = streamHealth,
                        fallbackState = fallbackState,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactView(
    bufferPercent: Float,
    bufferColor: Color,
    downloadSpeed: Long,
    speedColor: Color,
    swarmHealth: Float,
    piecesAhead: Int,
    piecesNeeded: Int,
    isUnderrun: Boolean,
    consecutiveUnderruns: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BufferIndicator(
            percent = bufferPercent,
            color = bufferColor,
            modifier = Modifier.width(80.dp).height(12.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatSpeed(downloadSpeed),
                    fontSize = 14.sp,
                    color = speedColor,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "/s",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pieces: $piecesAhead/$piecesNeeded",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                SwarmIndicator(health = swarmHealth)
            }
        }

        if (isUnderrun || consecutiveUnderruns > 0) {
            UnderrunWarning(consecutiveUnderruns = consecutiveUnderruns)
        }
    }
}

@Composable
private fun ExpandedView(
    bufferPercent: Float,
    bufferColor: Color,
    downloadSpeed: Long,
    speedColor: Color,
    swarmHealth: Float,
    piecesAhead: Int,
    piecesNeeded: Int,
    isUnderrun: Boolean,
    consecutiveUnderruns: Int,
    streamHealth: com.kurostream.torrent.streaming.StreamingTorrentManager.StreamHealth,
    fallbackState: com.kurostream.torrent.streaming.HttpFallbackManager.FallbackState?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BufferIndicator(
                percent = bufferPercent,
                color = bufferColor,
                showLabel = true,
                modifier = Modifier.weight(1f).height(24.dp),
            )

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatSpeed(downloadSpeed),
                    fontSize = 24.sp,
                    color = speedColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Download Speed",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailCard(
                title = "Buffer",
                value = "${(bufferPercent * 100).toInt()}%",
                subtitle = "${piecesAhead}/${piecesNeeded} pieces",
                icon = Icons.Default.Memory,
                color = bufferColor,
            )

            DetailCard(
                title = "Swarm",
                value = "${(swarmHealth * 100).toInt()}%",
                subtitle = "${streamHealth.activeStreams} active streams",
                icon = Icons.Default.People,
                color = when {
                    swarmHealth >= 0.7f -> Color.Green
                    swarmHealth >= 0.4f -> Color.Yellow
                    else -> Color.Red
                },
            )

            DetailCard(
                title = "Health",
                value = if (isUnderrun) "UNDERRUN" else "OK",
                subtitle = if (consecutiveUnderruns > 0) "$consecutiveUnderruns consecutive" else "Stable",
                icon = if (isUnderrun) Icons.Default.Warning else Icons.Default.CheckCircle,
                color = if (isUnderrun) Color.Red else Color.Green,
            )
        }

        if (fallbackState?.isFallbackActive == true) {
            FallbackDetailCard(fallbackState = fallbackState!!)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatItem(
                label = "Total Speed",
                value = formatSpeed(streamHealth.totalDownloadSpeed),
                color = Color.White,
            )
            StatItem(
                label = "Fallbacks",
                value = "${streamHealth.fallbackActive}",
                color = if (streamHealth.fallbackActive > 0) Color.Orange else Color.Green,
            )
            StatItem(
                label = "Success Rate",
                value = if (streamHealth.activeStreams > 0) {
                    "${((streamHealth.healthyStreams.toFloat() / streamHealth.activeStreams) * 100).toInt()}%"
                } else "N/A",
                color = Color.White,
            )
        }
    }
}

@Composable
private fun BufferIndicator(
    percent: Float,
    color: Color,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .width(percent.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.6f), color),
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
                .animateFloatAsState(targetValue = percent, label = "buffer"),
        )
        if (showLabel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${(percent * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SwarmIndicator(health: Float) {
    val icon = when {
        health >= 0.8f -> Icons.Default.SignalWifi4Bar
        health >= 0.6f -> Icons.Default.SignalWifiStatusbar3Bar
        health >= 0.4f -> Icons.Default.SignalWifiStatusbar2Bar
        health >= 0.2f -> Icons.Default.SignalWifiStatusbar1Bar
        else -> Icons.Default.SignalWifiOff
    }
    val color = when {
        health >= 0.7f -> Color.Green
        health >= 0.4f -> Color.Yellow
        else -> Color.Red
    }

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(imageVector = icon, contentDescription = "Swarm health", tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = "${(health * 100).toInt()}%",
            fontSize = 10.sp,
            color = color,
        )
    }
}

@Composable
private fun UnderrunWarning(consecutiveUnderruns: Int) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Buffer underrun",
                tint = Color.Red,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Buffer Underrun ${if (consecutiveUnderruns > 1) "x$consecutiveUnderruns" else ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
            )
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.material.icons.filled.Icon,
    color: Color,
) {
    Card(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Center,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            Text(text = subtitle, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FallbackBadge() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.Orange.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Fallback", tint = Color.Orange, modifier = Modifier.size(12.dp))
            Text(text = "HTTP Fallback", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Orange)
        }
    }
}

@Composable
private fun FallbackDetailCard(fallbackState: com.kurostream.torrent.streaming.HttpFallbackManager.FallbackState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Orange.copy(alpha = 0.1f),
        ),
        border = androidx.compose.ui.graphics.drawscope.Stroke(1.dp, Color.Orange.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "HTTP Fallback Active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Orange)
                Text(text = "Attempt ${fallbackState.fallbackAttempts}/3", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem(label = "Torrent Speed", value = formatSpeed(fallbackState.originalTorrentSpeed), color = Color.Red)
                StatItem(label = "HTTP Speed", value = formatSpeed(fallbackState.httpSpeed), color = if (fallbackState.httpSpeed > fallbackState.originalTorrentSpeed) Color.Green else Color.White)
                StatItem(label = "Duration", value = formatDuration(System.currentTimeMillis() - fallbackState.fallbackStartTime), color = Color.White)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Center, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
        Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "${bytesPerSec} B/s"
        bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds < 60) {
        "${seconds}s"
    } else if (seconds < 3600) {
        "${seconds / 60}m ${seconds % 60}s"
    } else {
        "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}