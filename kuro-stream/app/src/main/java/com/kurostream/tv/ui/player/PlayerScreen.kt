package com.kurostream.tv.ui.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kurostream.tv.ui.theme.KuroColors
import kotlinx.coroutines.delay

/**
 * Full-screen video player screen with TV-optimized controls
 * Supports D-pad navigation and skip overlays
 */
@Composable
fun PlayerScreen(
    animeId: String,
    episodeNumber: Int,
    onBackPressed: () -> Unit,
    onNextEpisode: (Int) -> Unit = {},
    onPreviousEpisode: (Int) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val skipState by viewModel.skipState.collectAsState()
    val subtitleState by viewModel.subtitleState.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    
    val controlsFocusRequester = remember { FocusRequester() }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && playerState.isPlaying) {
            delay(5000)
            showControls = false
        }
    }
    
    // Load episode
    LaunchedEffect(animeId, episodeNumber) {
        viewModel.loadEpisode(animeId, episodeNumber)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else {
                            viewModel.togglePlayPause()
                            true
                        }
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (showControls) {
                            showControls = false
                            true
                        } else {
                            onBackPressed()
                            true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (showControls) {
                            viewModel.seekBackward()
                        } else {
                            showControls = true
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (showControls) {
                            viewModel.seekForward()
                        } else {
                            showControls = true
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        showControls = true
                        false // Let focus system handle
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        viewModel.togglePlayPause()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        viewModel.play()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        viewModel.pause()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        viewModel.seekForward()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        viewModel.seekBackward()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        // Video player surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.getPlayer()
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = viewModel.getPlayer()
            }
        )
        
        // Skip button overlay
        AnimatedVisibility(
            visible = skipState.showSkipButton,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 120.dp)
        ) {
            SkipButton(
                text = skipState.skipButtonText,
                onSkip = { viewModel.skipCurrentSegment() },
                onDismiss = { viewModel.dismissSkipButton() }
            )
        }
        
        // Player controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                state = playerState,
                skipState = skipState,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeekBack = { viewModel.seekBackward() },
                onSeekForward = { viewModel.seekForward() },
                onSeekTo = { viewModel.seekTo(it) },
                onPreviousEpisode = { viewModel.previousEpisode() },
                onNextEpisode = { viewModel.nextEpisode() },
                onSubtitles = { showSubtitleMenu = true },
                onQuality = { showQualityMenu = true },
                onBack = onBackPressed,
                focusRequester = controlsFocusRequester
            )
        }
        
        // Buffering indicator
        if (playerState.isBuffering) {
            BufferingIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Error overlay
        playerState.error?.let { error ->
            ErrorOverlay(
                error = error,
                onRetry = { viewModel.retry() },
                onBack = onBackPressed,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    state: PlayerUiState,
    skipState: com.kurostream.tv.core.player.SkipOverlayState,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSubtitles: () -> Unit,
    onQuality: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        // Top bar - title and back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        id = android.R.drawable.ic_menu_close_clear_cancel
                    ),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = state.animeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Episode ${state.episodeNumber}: ${state.episodeTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Center controls - play/pause, seek
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous episode
            if (state.hasPreviousEpisode) {
                IconButton(
                    onClick = onPreviousEpisode,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = android.R.drawable.ic_media_previous
                        ),
                        contentDescription = "Previous Episode",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Seek backward
            IconButton(
                onClick = onSeekBack,
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = android.R.drawable.ic_media_rew
                        ),
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "10s",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Play/Pause
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(80.dp)
                    .focusRequester(focusRequester)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = KuroColors.Crimson,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (state.isPlaying) android.R.drawable.ic_media_pause
                                     else android.R.drawable.ic_media_play
                            ),
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // Seek forward
            IconButton(
                onClick = onSeekForward,
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = android.R.drawable.ic_media_ff
                        ),
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "10s",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Next episode
            if (state.hasNextEpisode) {
                IconButton(
                    onClick = onNextEpisode,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = android.R.drawable.ic_media_next
                        ),
                        contentDescription = "Next Episode",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Bottom bar - progress, time, settings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            // Progress bar with skip markers
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                SeekBar(
                    progress = state.progress,
                    bufferedProgress = state.bufferedProgress,
                    duration = state.duration,
                    onSeek = onSeekTo,
                    skipMarkers = skipState.availableSkips,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Time and controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time display
                Text(
                    text = "${formatTime(state.position)} / ${formatTime(state.duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                // Settings buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Subtitles
                    IconButton(
                        onClick = onSubtitles,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = android.R.drawable.ic_menu_edit
                            ),
                            contentDescription = "Subtitles",
                            tint = Color.White
                        )
                    }
                    
                    // Quality
                    IconButton(
                        onClick = onQuality,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = android.R.drawable.ic_menu_preferences
                            ),
                            contentDescription = "Quality",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(
    progress: Float,
    bufferedProgress: Float,
    duration: Long,
    onSeek: (Long) -> Unit,
    skipMarkers: List<com.kurostream.tv.data.remote.skip.SkipType>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Buffered progress
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedProgress)
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.5f))
        )
        
        // Played progress
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .background(KuroColors.Crimson)
        )
    }
}

@Composable
private fun SkipButton(
    text: String,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.colors(
            containerColor = KuroColors.Crimson,
            contentColor = Color.White
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BufferingIndicator(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                color = KuroColors.Crimson,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = KuroColors.CardBackground
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = android.R.drawable.stat_notify_error
                ),
                contentDescription = "Error",
                tint = KuroColors.Crimson,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Playback Error",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Go Back")
                }
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.colors(
                        containerColor = KuroColors.Crimson,
                        contentColor = Color.White
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * UI state for player screen
 */
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val progress: Float = 0f,
    val bufferedProgress: Float = 0f,
    val animeTitle: String = "",
    val episodeTitle: String = "",
    val episodeNumber: Int = 0,
    val hasPreviousEpisode: Boolean = false,
    val hasNextEpisode: Boolean = false,
    val currentQuality: String = "Auto",
    val availableQualities: List<String> = emptyList(),
    val error: String? = null
)
