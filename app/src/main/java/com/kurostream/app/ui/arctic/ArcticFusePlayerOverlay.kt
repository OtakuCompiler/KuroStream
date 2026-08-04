// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.flow.StateFlow

// ── Simplified player modal (used from HomeScreen) ───────────────────────────

/**
 * Lightweight player overlay shown from the home screen before the real
 * PlayerActivity starts. Shows title + close button only.
 */
@Composable
fun ArcticFusePlayerOverlay(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalArcticFusePalette.current
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(AFMotion.fast)),
        exit    = fadeOut(tween(AFMotion.fast)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg.copy(alpha = 0.96f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = palette.cyan, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text  = item?.title ?: "Loading…",
                    color = palette.text,
                    fontSize = AFTypo.heading,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "Starting player…",
                    color = palette.textSec,
                    fontSize = AFTypo.body,
                )
            }
        }
    }
}

// ── Full player controls overlay (used inside PlayerActivity) ────────────────

/**
 * Full-featured Arctic Fuse 3 player overlay.
 *
 * Features:
 * - Top gradient scrim: title + episode metadata
 * - Centre controls: skip back, play/pause (animated), skip forward
 * - Bottom scrim: Lanczos progress bar, timestamps, volume row
 * - **Skip Intro / Skip Outro** animated chips (AniSkip + IntroDB)
 * - Auto-hide after [AFMotion.playerShowHide] ms of inactivity
 * - Full D-pad / TV remote key handling
 */
@Composable
fun FullPlayerOverlay(
    title:         String,
    episodeLabel:  String,
    positionMs:    Long,
    durationMs:    Long,
    bufferedMs:    Long,
    isPlaying:     Boolean,
    isBuffering:   Boolean,
    visible:       Boolean,
    skipIntro:     StateFlow<Boolean>,
    skipOutro:     StateFlow<Boolean>,
    onSkipIntro:   () -> Unit,
    onSkipOutro:   () -> Unit,
    onPlayPause:   () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack:    () -> Unit,
    onToggleVisible: () -> Unit = {},
    modifier:      Modifier = Modifier,
) {
    val palette        = LocalArcticFusePalette.current
    val showIntroChip  by skipIntro.collectAsState()
    val showOutroChip  by skipOutro.collectAsState()

    // Progress fractions
    val playFraction     = if (durationMs > 0) (positionMs.toFloat()  / durationMs).coerceIn(0f, 1f) else 0f
    val bufferFraction   = if (durationMs > 0) (bufferedMs.toFloat()  / durationMs).coerceIn(0f, 1f) else 0f

    Box(modifier = modifier.fillMaxSize().clickable { onToggleVisible() }) {

        // ── Top scrim ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn(tween(AFMotion.fast)),
            exit     = fadeOut(tween(AFMotion.fast)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(AFGradientHeroTop))
                    .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.safeZoneV),
            ) {
                Column {
                    Text(title,        color = palette.text,    fontSize = AFTypo.playerTitle, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(episodeLabel, color = palette.textSec, fontSize = AFTypo.playerMeta)
                }
            }
        }

        // ── Buffering spinner ─────────────────────────────────────────────────
        if (isBuffering) {
            CircularProgressIndicator(
                color    = palette.cyan,
                modifier = Modifier.size(48.dp).align(Alignment.Center),
            )
        }

        // ── Centre playback controls ──────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible && !isBuffering,
            enter    = fadeIn(tween(AFMotion.fast)),
            exit     = fadeOut(tween(AFMotion.fast)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                PlayerControlButton(
                    icon  = { IconSkipBack(tint = palette.text, iconSize = 32.dp) },
                    onClick  = onSeekBack,
                    accent   = palette.cyan,
                    size     = 60.dp,
                )
                PlayerControlButton(
                    icon  = { if (isPlaying) IconPause(tint = palette.bg, iconSize = 36.dp)
                              else IconPlay(tint = palette.bg, iconSize = 36.dp) },
                    onClick  = onPlayPause,
                    accent   = palette.cyan,
                    size     = 88.dp,
                    primary  = true,
                )
                PlayerControlButton(
                    icon  = { IconSkipFwd(tint = palette.text, iconSize = 32.dp) },
                    onClick  = onSeekForward,
                    accent   = palette.cyan,
                    size     = 60.dp,
                )
            }
        }

        // ── Skip chips ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = AFSpacing.safeZoneH, bottom = 140.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(
                visible = showIntroChip,
                enter   = slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it } + fadeIn(),
                exit    = slideOutVertically(tween(AFMotion.skipChipExit)) { it } + fadeOut(),
            ) {
                SkipChip("Skip Intro", palette.cyan, onSkipIntro)
            }
            AnimatedVisibility(
                visible = showOutroChip,
                enter   = slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it } + fadeIn(),
                exit    = slideOutVertically(tween(AFMotion.skipChipExit)) { it } + fadeOut(),
            ) {
                SkipChip("Skip Outro", palette.teal, onSkipOutro)
            }
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = visible,
            enter    = fadeIn(tween(AFMotion.fast)),
            exit     = fadeOut(tween(AFMotion.fast)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(AFGradientHeroBottom))
                    .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.safeZoneV),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dual-layer progress bar: buffered + played
                    DualProgressBar(
                        playFraction   = playFraction,
                        bufferFraction = bufferFraction,
                        accent         = palette.cyan,
                        buffer         = palette.surfaceVariant,
                        track          = palette.border,
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatMs(positionMs), color = palette.textSec, fontSize = AFTypo.meta)
                        Text(formatMs(durationMs), color = palette.textSec, fontSize = AFTypo.meta)
                    }
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SkipChip(label: String, accent: Color, onClick: () -> Unit) {
    val fr     = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AFRadius.pill))
            .background(accent.copy(alpha = if (focused) 0.28f else 0.16f))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = accent,
                shape = RoundedCornerShape(AFRadius.pill),
            )
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp &&
                    (ev.key == Key.Enter || ev.key == Key.NumPadEnter || ev.key == Key.DirectionCenter)) {
                    onClick(); true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = AFTypo.skipChipLabel, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerControlButton(
    icon:    @Composable () -> Unit,
    onClick: () -> Unit,
    accent:  Color,
    size:    androidx.compose.ui.unit.Dp,
    primary: Boolean = false,
) {
    val fr     = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) accent else Color.Transparent)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = accent,
                shape = CircleShape,
            )
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp &&
                    (ev.key == Key.Enter || ev.key == Key.NumPadEnter || ev.key == Key.DirectionCenter)) {
                    onClick(); true
                } else false
            }
            .clickable { onClick() }
            .padding(if (primary) 20.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun DualProgressBar(
    playFraction:   Float,
    bufferFraction: Float,
    accent:         Color,
    buffer:         Color,
    track:          Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(track),
    ) {
        // Buffer layer
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferFraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(buffer),
        )
        // Played layer
        Box(
            modifier = Modifier
                .fillMaxWidth(playFraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.85f)))),
        )
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
