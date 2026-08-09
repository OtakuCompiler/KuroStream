// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kurostream.app.ui.theme.Af3Theme
import kotlinx.coroutines.delay

// =============================================================================
// AF3 playback features: sleep timer, track picker, ABR picker, PiP toggle.
// =============================================================================

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
fun rememberAf3SleepTimer(
    option: SleepTimerOption,
    onEvent: (SleepTimerEvent) -> Unit,
) {
    LaunchedEffect(option) {
        val mins = option.minutes ?: return@LaunchedEffect
        if (mins <= 0) return@LaunchedEffect
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

@Composable
fun Af3SleepTimerPanel(
    currentOption: SleepTimerOption,
    onOptionChange: (SleepTimerOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(space.safeH)
                .clip(RoundedCornerShape(space.s16))
                .background(palette.surface.copy(alpha = 0.96f))
                .border(1.dp, palette.border, RoundedCornerShape(space.s16))
                .padding(space.s16),
        ) {
            Text("Sleep Timer", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(space.s12))
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
                        .clip(RoundedCornerShape(space.s12))
                        .background(bg)
                        .focusable(interactionSource = interaction)
                        .clickable { onOptionChange(opt); onClose() }
                        .padding(horizontal = space.s12, vertical = space.s12),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (opt == currentOption) palette.accent else palette.border),
                    )
                    Spacer(Modifier.width(space.s12))
                    Text(opt.label, color = palette.text, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun Af3SleepTimerCountdown(remainingMs: Long, modifier: Modifier = Modifier) {
    val palette = Af3Theme.palette
    val minutes = (remainingMs / 60_000L).toInt()
    val seconds = ((remainingMs / 1000L) % 60L).toInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("⏰", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "${minutes}m ${seconds}s",
            color = palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

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
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Column(modifier = modifier) {
        Text(title, color = palette.textSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(space.s8)) {
            items(tracks, key = { it.id }) { track ->
                val isSelected = track.id == selectedTrackId
                val interaction = remember(track.id) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val bg = when {
                    isSelected -> palette.accent.copy(alpha = 0.85f)
                    focused -> palette.surfaceActive
                    else -> palette.surface
                }
                val fg = if (isSelected) palette.bgDeep else palette.text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(50))
                        .focusable(interactionSource = interaction)
                        .clickable { onTrackSelected(track) }
                        .padding(horizontal = space.s12, vertical = space.s6),
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

enum class AbrQuality(val label: String) {
    Auto("Auto"),
    Ultra("4K"),
    Full("1080p"),
    Hd("720p"),
    Sd("480p"),
}

@Composable
fun Af3AbrPicker(
    current: AbrQuality,
    onChange: (AbrQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(space.s8)) {
        AbrQuality.entries.forEach { q ->
            val interaction = remember(q) { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val isCurrent = q == current
            val bg = when {
                isCurrent -> palette.accentSec
                focused -> palette.surfaceActive
                else -> palette.surface
            }
            val fg = if (isCurrent) palette.bgDeep else palette.text
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(space.s4))
                    .background(bg)
                    .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(space.s4))
                    .focusable(interactionSource = interaction)
                    .clickable { onChange(q) }
                    .padding(horizontal = space.s12, vertical = space.s6),
            ) {
                Text(q.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun Af3PipToggleButton(
    isPipActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        isPipActive -> palette.accent
        focused -> palette.surfaceActive
        else -> palette.surface
    }
    val fg = if (isPipActive) palette.bgDeep else palette.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (focused) palette.borderFocus else palette.border, RoundedCornerShape(50))
            .focusable(interactionSource = interaction)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(width = 14.dp, height = 10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPipActive) palette.bgDeep else palette.text),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isPipActive) "Exit PiP" else "Picture in Picture",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
