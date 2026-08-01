// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import android.util.TypedValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import kotlinx.coroutines.delay

@UnstableApi
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackPressed: () -> Unit,
    hdrMode: HdrMode = HdrMode.SDR,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val engine = viewModel.currentEngine
    var controlsVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(controlsVisible, uiState.isPlaying) {
        if (controlsVisible && uiState.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            focusRequester.requestFocus()
        }
    }

    // Apply subtitle style to PlayerView's SubtitleView
    LaunchedEffect(
        uiState.subtitleFontSize,
        uiState.subtitleFontColorHex,
        uiState.subtitleBgColorHex,
        uiState.subtitleEnabled,
    ) {
        val pv = playerViewRef ?: return@LaunchedEffect
        try {
            val sv = pv.subtitleView ?: return@LaunchedEffect
            val fontColor = android.graphics.Color.parseColor(uiState.subtitleFontColorHex)
            val bgColor = android.graphics.Color.parseColor(uiState.subtitleBgColorHex)
            sv.setStyle(
                CaptionStyleCompat(
                    fontColor,
                    bgColor,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    android.graphics.Color.BLACK,
                    null,
                )
            )
            sv.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, uiState.subtitleFontSize)
        } catch (_: IllegalArgumentException) {
            // ignore invalid hex color strings / view access errors
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            viewModel.togglePlayPause()
                            controlsVisible = true
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.seekBackward()
                            controlsVisible = true
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.seekForward()
                            controlsVisible = true
                            true
                        }
                        Key.Back -> {
                            if (controlsVisible) {
                                controlsVisible = false
                                true
                            } else {
                                onBackPressed()
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
    val isMedia3 = remember(engine) { engine?.nativePlayer() is ExoPlayer }

    if (isMedia3) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    playerViewRef = this
                    (engine?.nativePlayer() as? ExoPlayer)?.let { exo ->
                        this.player = exo
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hdrMode != HdrMode.SDR) {
                        setSecure(true)
                    }
                }
            },
            update = { view ->
                (engine?.nativePlayer() as? ExoPlayer)?.let { player ->
                    view.player = player
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val surfaceView = view.videoSurfaceView as? android.view.SurfaceView
                        surfaceView?.setSecure(true)
                    }
                }
            },
            onReset = { playerViewRef?.player = null },
            onRelease = { view ->
                view.player = null
                playerViewRef = null
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        AndroidView(
            factory = { ctx ->
                android.view.SurfaceView(ctx).apply {
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            if (engine is com.kurostream.players.mpv.MpvPlayer) {
                                engine.setSurface(holder.surface)
                            }
                        }
                        override fun surfaceChanged(
                            holder: android.view.SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) = Unit
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                            if (engine is com.kurostream.players.mpv.MpvPlayer) {
                                engine.setSurface(null)
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

        if (uiState.isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                title = uiState.title,
                isPlaying = uiState.isPlaying,
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                bufferedPosition = uiState.bufferedPosition,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { positionMs -> viewModel.seekTo(positionMs) },
                onSeekForward = { viewModel.seekForward() },
                onSeekBackward = { viewModel.seekBackward() },
                onSkipIntro = { viewModel.skipIntro() },
                onSkipOutro = { viewModel.skipOutro() },
                onNext = { viewModel.playNextEpisode() },
                onSettings = { showSettings = !showSettings },
                onBack = onBackPressed,
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerSettingsPanel(
                currentSpeed = uiState.playbackSpeed,
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                subtitleFontSize = uiState.subtitleFontSize,
                subtitleFontColorHex = uiState.subtitleFontColorHex,
                subtitleBgColorHex = uiState.subtitleBgColorHex,
                subtitleEnabled = uiState.subtitleEnabled,
                onSubtitleFontSizeChange = { viewModel.setSubtitleFontSize(it) },
                onSubtitleFontColorChange = { viewModel.setSubtitleFontColor(it) },
                onSubtitleBgColorChange = { viewModel.setSubtitleBgColor(it) },
                onSubtitleEnabledChange = { viewModel.setSubtitleEnabled(it) },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSkipIntro: () -> Unit,
    onSkipOutro: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipIntro, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Skip Intro",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onSeekBackward, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Back 10s",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(80.dp)
                    .focusRequester(focusRequester),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = onSeekForward, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(onClick = onSkipOutro, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Outro",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Slider(
                value = progress,
                onValueChange = { onSeek((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Episode",
                            tint = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun PlayerSettingsPanel(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    subtitleFontSize: Float,
    subtitleFontColorHex: String,
    subtitleBgColorHex: String,
    subtitleEnabled: Boolean,
    onSubtitleFontSizeChange: (Float) -> Unit,
    onSubtitleFontColorChange: (String) -> Unit,
    onSubtitleBgColorChange: (String) -> Unit,
    onSubtitleEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    var selectedSpeed by remember { mutableFloatStateOf(currentSpeed) }
    var subtitleDelay by remember { mutableLongStateOf(0L) }
    var audioDelay by remember { mutableLongStateOf(0L) }
    var selectedQuality by remember { mutableStateOf("Auto") }
    var selectedAudioTrack by remember { mutableStateOf("Default") }
    var selectedSubtitleTrack by remember { mutableStateOf("Off") }

    val fontColorPresets = listOf(
        "#FFFFFF" to "White",
        "#FFE066" to "Yellow",
        "#66E0FF" to "Cyan",
        "#66FF99" to "Green",
        "#FF66B2" to "Pink",
    )
    var showSubtitleStyling by remember { mutableStateOf(false) }

    val qualities = listOf("Auto", "1080p", "720p", "480p", "360p")
    val audioTracks = listOf("Default", "English", "Japanese")
    val subtitleTracks = listOf("Off", "English", "Japanese", "Spanish")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .fillMaxWidth(0.85f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Playback Speed
                item {
                    Column {
                        Text(
                            text = "Playback Speed",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playbackSpeeds.forEach { speed ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedSpeed = speed
                                            onSpeedChange(speed)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedSpeed == speed)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Gray.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Quality Selection
                item {
                    Column {
                        Text(
                            text = "Quality",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            qualities.take(4).forEach { quality ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedQuality = quality },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedQuality == quality)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Gray.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = quality,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Audio Track Selection
                item {
                    Column {
                        Text(
                            text = "Audio Track",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAudioTrack = track }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAudioTrack == track,
                                    onClick = { selectedAudioTrack = track },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = track,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Subtitle Track Selection + Styling
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subtitles",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Switch(
                                checked = subtitleEnabled,
                                onCheckedChange = { onSubtitleEnabledChange(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                        if (subtitleEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            subtitleTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSubtitleTrack = track }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSubtitleTrack == track,
                                        onClick = { selectedSubtitleTrack = track },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = track,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subtitle Styling toggle
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSubtitleStyling = !showSubtitleStyling },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Gray.copy(alpha = 0.2f),
                            ) {
                                Text(
                                    text = if (showSubtitleStyling) "▼ Subtitle Styling" else "▶ Subtitle Styling",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                )
                            }

                            if (showSubtitleStyling) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Font Size
                                Text(
                                    text = "Font Size: ${subtitleFontSize.toInt()}sp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Slider(
                                    value = subtitleFontSize,
                                    onValueChange = { onSubtitleFontSizeChange(it) },
                                    valueRange = 12f..48f,
                                    steps = 17,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Font Color presets
                                Text(
                                    text = "Font Color",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    fontColorPresets.forEach { (hex, label) ->
                                        val isSelected = subtitleFontColorHex.equals(hex, ignoreCase = true)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onSubtitleFontColorChange(hex) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                Color.Gray.copy(alpha = 0.3f),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                // Color swatch
                                                Surface(
                                                    modifier = Modifier.size(20.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = try {
                                                        val c = android.graphics.Color.parseColor(hex)
                                                        Color(c)
                                                    } catch (_: IllegalArgumentException) {
                                                        Color.White
                                                    },
                                                ) {}
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center,
                                                )
            }
        }
    }
}

enum class HdrMode { SDR, HDR10, HDR10_PLUS, DOLBY_VISION }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Background Opacity
                                val currentAlpha = try {
                                    val c = android.graphics.Color.parseColor(subtitleBgColorHex)
                                    android.graphics.Color.alpha(c) / 255f * 100f
                                } catch (_: IllegalArgumentException) { 50f }
                                Text(
                                    text = "Background: ${currentAlpha.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Slider(
                                    value = currentAlpha,
                                    onValueChange = { alpha ->
                                        val a = (alpha / 100f * 255).toInt().coerceIn(0, 255)
                                        val hex = "#%02X000000".format(a)
                                        onSubtitleBgColorChange(hex)
                                    },
                                    valueRange = 0f..100f,
                                    steps = 19,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Subtitle Delay
                item {
                    Column {
                        Text(
                            text = "Subtitle Delay: ${subtitleDelay}ms",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Slider(
                            value = subtitleDelay.toFloat(),
                            onValueChange = { subtitleDelay = it.toLong() },
                            valueRange = -5000f..5000f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                // Audio Delay
                item {
                    Column {
                        Text(
                            text = "Audio Delay: ${audioDelay}ms",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Slider(
                            value = audioDelay.toFloat(),
                            onValueChange = { audioDelay = it.toLong() },
                            valueRange = -5000f..5000f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
