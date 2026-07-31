// This file is part of KuroStream.
//
// PremiumPlayerOverlay — enhanced player UI with:
//   - OLED black player controls (pure black backgrounds)
//   - Blue cinematic progress bar
//   - Glass playback controls
//   - Subtle HDR style highlights
//
// Wraps ArcticFusePlayerOverlay with premium enhancements.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kurostream.app.ui.theme.CinematicBluePrimary
import com.kurostream.app.ui.theme.OledPureBlack
import com.kurostream.app.model.MediaItem

@Composable
fun PremiumPlayerOverlay(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    progressPercent: Float = 0f,
    volumePercent: Float = 80f,
    totalDurationMs: Long = 7200000L,
    onPlayPause: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onSkipBack: () -> Unit = {},
    onSkipForward: () -> Unit = {},
) {
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(visible, showControls) {
        if (visible && showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { showControls = !showControls },
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(OledPureBlack.copy(alpha = 0.9f), Color.Transparent),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item?.title ?: "Now Playing",
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                PremiumPlayerIconButton(icon = { IconClose(tint = Color.White) }, onClick = onClose)
            }

            // Center controls
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                PremiumPlayerIconButton(icon = { IconSkipBack(tint = Color.White) }, onClick = onSkipBack, size = 48.dp)
                PremiumPlayerIconButton(
                    icon = { if (isPlaying) IconPause(tint = OledPureBlack) else IconPlay(tint = OledPureBlack) },
                    onClick = onPlayPause,
                    size = 80.dp,
                    primary = true,
                )
                PremiumPlayerIconButton(icon = { IconSkipFwd(tint = Color.White) }, onClick = onSkipForward, size = 48.dp)
            }

            // Bottom bar with cinematic blue progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, OledPureBlack.copy(alpha = 0.85f)),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
                    .align(Alignment.BottomCenter),
            ) {
                val elapsedMs = (progressPercent * totalDurationMs).toLong()
                val remainingMs = totalDurationMs - elapsedMs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = formatDuration(elapsedMs), color = Color.Gray, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                    PremiumProgressBar(
                        progress = progressPercent,
                        onSeek = onSeek,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "-${formatDuration(remainingMs)}", color = Color.Gray, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconVolume(tint = Color.Gray, iconSize = 20.dp)
                        PremiumProgressBar(
                            progress = volumePercent / 100f,
                            onSeek = { onVolumeChange(it * 100) },
                            modifier = Modifier.width(100.dp),
                        )
                    }

                    if (remainingMs < 30000 && isPlaying) {
                        Text(text = "Next episode starting soon...", color = CinematicBluePrimary, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumPlayerIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    primary: Boolean = false,
    contentDescription: String = "",
) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .size(size)
            .background(if (primary) CinematicBluePrimary else Color.Transparent, CircleShape)
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .border(width = if (isFocused) 2.dp else 0.dp, color = CinematicBluePrimary, shape = CircleShape)
            .padding(if (primary) 16.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun PremiumProgressBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = modifier
            .height(6.dp)
            .background(Color.DarkGray, RoundedCornerShape(3.dp))
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .border(width = if (isFocused) 2.dp else 0.dp, color = CinematicBluePrimary, shape = RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CinematicBluePrimary, Color(0xFF0066FF)),
                    ),
                    RoundedCornerShape(3.dp),
                ),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
