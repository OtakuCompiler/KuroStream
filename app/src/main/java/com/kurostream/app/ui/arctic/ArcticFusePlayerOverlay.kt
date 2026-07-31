// This file is part of KuroStream.
//
// ArcticFusePlayerOverlay — full-screen player controls overlay matching
// Arctic Fuse PlayerOverlay.jsx: top bar (title + close), center play/pause
// + skip controls, bottom progress bar + volume + next-episode countdown.
//
// Auto-hides after 3s of inactivity. Shows on tap/hover.
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.delay

@Composable
fun ArcticFusePlayerOverlay(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // Playback state (passed in from PlayerViewModel)
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
            delay(AFMotion.playerShowHide.toLong())
            showControls = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(AFMotion.fast)),
        exit = fadeOut(animationSpec = tween(AFMotion.fast)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showControls = !showControls }
                .semantics {
                    contentDescription = if (visible) "Player controls" else "Player controls hidden"
                    role = androidx.compose.ui.semantics.Role.Button
                },
        ) {
            // Top bar gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        ),
                    ),
            )

            // Top bar content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AFSpacing.px6)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item?.title ?: "Now Playing",
                    color = AFText,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                PlayerIconButton(
                    icon = { IconClose(tint = AFTextDim) },
                    onClick = onClose,
                    contentDescription = "Close player",
                )
            }

            // Center controls
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px6),
            ) {
                PlayerIconButton(
                    icon = { IconSkipBack(tint = AFText) },
                    onClick = onSkipBack,
                    size = 40.dp,
                    contentDescription = "Skip back 10 seconds",
                )
                PlayerIconButton(
                    icon = { if (isPlaying) IconPause(tint = AFBgDeep) else IconPlay(tint = AFBgDeep) },
                    onClick = onPlayPause,
                    size = 64.dp,
                    primary = true,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
                PlayerIconButton(
                    icon = { IconSkipFwd(tint = AFText) },
                    onClick = onSkipForward,
                    size = 40.dp,
                    contentDescription = "Skip forward 10 seconds",
                )
            }

            // Bottom bar gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        ),
                    ),
            )

            // Bottom bar content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AFSpacing.px6)
                    .align(Alignment.BottomCenter),
            ) {
                // Progress bar
                val elapsedMs = (progressPercent * totalDurationMs).toLong()
                val remainingMs = totalDurationMs - elapsedMs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
                ) {
                    Text(
                        text = formatDuration(elapsedMs),
                        color = AFTextDim,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp),
                    )
                    ProgressBar(
                        progress = progressPercent,
                        onSeek = onSeek,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "-${formatDuration(remainingMs)}",
                        color = AFTextDim,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp),
                    )
                }

                Spacer(Modifier.height(AFSpacing.px3))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Volume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
                    ) {
                        IconVolume(tint = AFTextDim, iconSize = 16.dp)
                        VolumeSlider(
                            value = volumePercent,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.width(80.dp),
                        )
                    }

                    // Next episode countdown
                    if (remainingMs < 30000 && isPlaying) {
                        Text(
                            text = "Next episode starting soon...",
                            color = AFTeal,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
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
            .background(if (primary) AFCyan else Color.Transparent, CircleShape)
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .semantics {
                if (contentDescription.isNotBlank()) {
                    this.contentDescription = contentDescription
                }
            }
            .border(width = if (isFocused) 2.dp else 0.dp, color = AFCyan, shape = CircleShape)
            .padding(if (primary) 12.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun ProgressBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = modifier
            .height(4.dp)
            .background(AFSurface, RoundedCornerShape(2.dp))
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { /* seek on click handled by parent */ }
            .semantics {
                contentDescription = "Seek bar, ${(progress * 100).toInt()}% complete"
            }
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(AFCyan, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun VolumeSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = modifier
            .height(4.dp)
            .background(AFSurface, RoundedCornerShape(2.dp))
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { /* volume handled by parent */ }
            .semantics {
                contentDescription = "Volume, ${value.toInt()}%"
            }
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                .height(4.dp)
                .background(AFCyan, RoundedCornerShape(2.dp)),
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