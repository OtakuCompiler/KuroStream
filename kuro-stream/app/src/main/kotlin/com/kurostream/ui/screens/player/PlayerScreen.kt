package com.kurostream.ui.screens.player

import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.ui.components.FocusableCard
import com.kurostream.ui.components.LoadingScreen
import com.kurostream.ui.theme.KuroBackground
import com.kurostream.ui.theme.KuroPrimary
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val contentId = remember { streamUrl.hashCode().toString() }
    var controlsVisible by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(streamUrl) {
        viewModel.initPlayer(streamUrl, contentId, title)
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown -> {
                        controlsVisible = true
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter ->
                                viewModel.togglePlayPause().let { true }
                            Key.DirectionRight ->
                                viewModel.seekForward().let { true }
                            Key.DirectionLeft ->
                                viewModel.seekBackward().let { true }
                            Key.Back ->
                                onBack().let { true }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    viewModel.playerController.exoPlayer.setVideoSurfaceView(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playerState.isBuffering) {
            LoadingScreen("Buffering...")
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title = title,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                error = playerState.error,
                onPlayPause = viewModel::togglePlayPause,
                onSeekForward = viewModel::seekForward,
                onSeekBackward = viewModel::seekBackward,
                onSeekTo = viewModel::seekTo,
                onBack = onBack
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    error: String?,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FocusableCard(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        error?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (durationMs > 0) {
                Slider(
                    value = currentPositionMs.toFloat() / durationMs,
                    onValueChange = { onSeekTo((it * durationMs).toLong()) },
                    colors = SliderDefaults.colors(thumbColor = KuroPrimary, activeTrackColor = KuroPrimary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableCard(onClick = onSeekBackward) {
                    Icon(Icons.Default.FastRewind, "Rewind 10s", tint = Color.White, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.width(24.dp))
                FocusableCard(onClick = onPlayPause, containerColor = KuroPrimary) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(Modifier.width(24.dp))
                FocusableCard(onClick = onSeekForward) {
                    Icon(Icons.Default.FastForward, "Forward 10s", tint = Color.White, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
