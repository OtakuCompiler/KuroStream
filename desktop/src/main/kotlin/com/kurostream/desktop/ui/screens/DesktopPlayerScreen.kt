package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import com.kurostream.desktop.playback.DesktopPlayerFactory
import com.kurostream.desktop.playback.PlayerHandle
import com.kurostream.desktop.playback.DesktopPlaybackSettings

/**
 * Desktop player screen. Embeds a vlcj-backed AWT canvas via `SwingPanel`,
 * letting the Compose shell coexist with native OS-level rendering.
 *
 * 4K + Dolby Atmos passthrough is enabled by default on systems with HDMI
 * ARC/eARC; transcoding is opt-in via `settings.dolbyAtmosPassthrough`.
 */
@Composable
fun DesktopPlayerScreen(
    state: DesktopAppState,
    mediaId: String,
    playerFactory: DesktopPlayerFactory,
    onClose: () -> Unit,
) {
    val handle: PlayerHandle = remember(mediaId) { playerFactory.acquire(mediaId) }
    val settings = remember { state.settings.snapshot() }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(handle, settings) {
        handle.setAudioPassthrough(settings.dolbyAtmosPassthrough)
        // Real impl: resolve playback URL through :domain
        handle.prepare("file:///dev/null")
    }

    DisposableEffect(handle) {
        onDispose {
            handle.pause()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            SwingPanel(
                factory = { handle.videoSurface },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Button(onClick = {
                if (isPlaying) handle.pause() else handle.play()
                isPlaying = !isPlaying
            }) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(onClick = {
                handle.setVolume(0f)
            }) { Text("Mute") }
            Button(onClick = {
                val vlcAvailable = playerFactory.playbackBackendAvailable()
                Text(if (vlcAvailable) "Backend: libVLC ✓" else "Backend: missing")
            })
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }
}
