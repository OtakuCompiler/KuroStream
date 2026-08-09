// This file is part of KuroStream.
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.arctic.LocalArcticFuse3Extras
import com.kurostream.app.ui.arctic.LocalArcticFuse3Tokens
import com.kurostream.app.ui.arctic.LocalArcticFusePalette
import kotlinx.coroutines.delay

/**
 * AF3 Sleep Timer — pick a duration or turn off. The active timer
 * counts down and emits [SleepTimerEvent.Expired] when it hits zero.
 */
enum class SleepTimerOption(val label: String, val minutes: Int?) {
    Off("Off", null),
    Fifteen("15 min", 15),
    Thirty("30 min", 30),
    FortyFive("45 min", 45),
    Sixty("60 min", 60),
    Ninety("90 min", 90),
    EndOfEpisode("End of episode", -1),
}

sealed interface SleepTimerEvent {
    data object Expired : SleepTimerEvent
    data class Tick(val remainingMs: Long) : SleepTimerEvent
}

@Composable
fun Af3SleepTimerPanel(
    currentOption: SleepTimerOption,
    onOptionChange: (SleepTimerOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = tokens.space48, top = tokens.space48, bottom = tokens.space48)
                .width(320.dp)
                .clip(RoundedCornerShape(tokens.radiusLarge))
                .background(palette.surface.copy(alpha = 0.96f))
                .border(1.dp, palette.border, RoundedCornerShape(tokens.radiusLarge))
                .padding(tokens.space16),
        ) {
            Text(
                "Sleep Timer",
                color = palette.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(tokens.space12))
            SleepTimerOption.entries.forEach { opt ->
                val interaction = remember(opt) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val bg = when {
                    opt == currentOption -> palette.surfaceActive
                    focused -> palette.surfaceHighlight
                    else -> Color.Transparent
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.radiusMedium))
                        .background(bg)
                        .focusable(interactionSource = interaction)
                        .clickable { onOptionChange(opt); onClose() }
                        .padding(horizontal = tokens.space12, vertical = tokens.space12),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (opt == currentOption) palette.cyan else palette.border),
                    )
                    Spacer(Modifier.width(tokens.space12))
                    Text(opt.label, color = palette.text, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * AF3 audio/subtitle track picker — horizontal chips.
 */
data class TrackInfo(
    val id: String,
    val label: String,
    val language: String? = null,
    val isDefault: Boolean = false,
)

@Composable
fun Af3TrackPicker(
    title: String,
    tracks: List<TrackInfo>,
    selectedTrackId: String?,
    onTrackSelected: (TrackInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    Column(modifier = modifier) {
        Text(
            title,
            color = palette.textSec,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(tokens.space8))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.space8),
        ) {
            items(tracks, key = { it.id }) { track ->
                val isSelected = track.id == selectedTrackId
                val interaction = remember(track.id) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val bg = when {
                    isSelected -> palette.cyan.copy(alpha = 0.85f)
                    focused -> palette.surfaceActive
                    else -> palette.surface
                }
                val fg = if (isSelected) Color.Black else palette.text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(50))
                        .focusable(interactionSource = interaction)
                        .clickable { onTrackSelected(track) }
                        .padding(horizontal = tokens.space12, vertical = tokens.space6),
                ) {
                    Text(
                        "${track.label}${track.language?.let { " · ${it.uppercase()}" } ?: ""}${if (track.isDefault) " ★" else ""}",
                        color = fg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * AF3 ABR (quality) picker — Auto / 1080p / 720p / 480p chips.
 */
enum class AbrQuality(val label: String, val maxHeightPx: Int?) {
    Auto("Auto", null),
    Ultra("4K", 2160),
    Full("1080p", 1080),
    Hd("720p", 720),
    Sd("480p", 480),
}

@Composable
fun Af3AbrPicker(
    current: AbrQuality,
    onChange: (AbrQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current
    val extras = LocalArcticFuse3Extras.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.space8),
    ) {
        AbrQuality.entries.forEach { q ->
            val interaction = remember(q) { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val isCurrent = q == current
            val bg = when {
                isCurrent -> extras.hudBlue
                focused -> palette.surfaceActive
                else -> palette.surface
            }
            val fg = if (isCurrent) Color.White else palette.text
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.radiusSmall))
                    .background(bg)
                    .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(tokens.radiusSmall))
                    .focusable(interactionSource = interaction)
                    .clickable { onChange(q) }
                    .padding(horizontal = tokens.space12, vertical = tokens.space6),
            ) {
                Text(
                    q.label,
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * AF3 chapter pill row — for media with chapters.
 */
data class ChapterMarker(
    val startMs: Long,
    val title: String?,
)

@Composable
fun Af3ChapterRow(
    chapters: List<ChapterMarker>,
    currentPositionMs: Long,
    onJumpTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    if (chapters.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.space8),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = tokens.space12),
    ) {
        items(chapters, key = { it.startMs }) { ch ->
            val isCurrent = currentPositionMs >= ch.startMs &&
                (chapters.indexOf(ch) == chapters.lastIndex ||
                    currentPositionMs < chapters[chapters.indexOf(ch) + 1].startMs)
            val interaction = remember(ch.startMs) { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val bg = when {
                isCurrent -> palette.cyan.copy(alpha = 0.85f)
                focused -> palette.surfaceActive
                else -> palette.surfaceVariant
            }
            val fg = if (isCurrent) Color.Black else palette.text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.radiusMedium))
                    .background(bg)
                    .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(tokens.radiusMedium))
                    .focusable(interactionSource = interaction)
                    .clickable { onJumpTo(ch.startMs) }
                    .padding(horizontal = tokens.space12, vertical = tokens.space8),
            ) {
                Text(
                    ch.title ?: "Ch ${chapters.indexOf(ch) + 1}",
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * AF3 PiP toggle — minimalist pill button.
 */
@Composable
fun Af3PipToggleButton(
    isPipActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current
    val extras = LocalArcticFuse3Extras.current

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        isPipActive -> extras.pipBorder
        focused -> palette.surfaceActive
        else -> palette.surface
    }
    val fg = if (isPipActive) Color.Black else palette.text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(50))
            .focusable(interactionSource = interaction)
            .clickable { onToggle() }
            .padding(horizontal = tokens.space12, vertical = tokens.space6),
    ) {
        Box(
            Modifier
                .size(width = 14.dp, height = 10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPipActive) Color.Black else palette.text),
        )
        Spacer(Modifier.width(tokens.space8))
        Text(
            if (isPipActive) "Exit PiP" else "Picture in Picture",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * AF3 sleep timer countdown overlay — small pill that appears when
 * a timer is active and counts down.
 */
@Composable
fun Af3SleepTimerCountdown(
    remainingMs: Long,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    val minutes = (remainingMs / 60_000L).toInt()
    val seconds = ((remainingMs / 1000L) % 60L).toInt()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = tokens.space12, vertical = tokens.space6),
    ) {
        Text(
            "⏰",
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(tokens.space6))
        Text(
            "${minutes}m ${seconds}s",
            color = palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Hook helper — runs a sleep timer countdown and emits events.
 * Returns the latest event via callback. Pass `null` option to cancel.
 */
@Composable
fun rememberAf3SleepTimer(
    option: SleepTimerOption,
    onEvent: (SleepTimerEvent) -> Unit,
) {
    LaunchedEffect(option) {
        val mins = option.minutes
        if (mins == null) return@LaunchedEffect
        val total = mins * 60_000L
        var remaining = total
        while (remaining > 0L) {
            onEvent(SleepTimerEvent.Tick(remaining))
            delay(1000L)
            remaining -= 1000L
        }
        onEvent(SleepTimerEvent.Expired)
    }
}
