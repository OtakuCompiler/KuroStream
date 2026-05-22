package com.kurostream.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
    val qualityLabel: String = ""
)

@Singleton
class PlayerController @Inject constructor(
    val exoPlayer: ExoPlayer,
    private val memoryManager: MemoryManager
) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playerState.value = _playerState.value.copy(isBuffering = true)
                    memoryManager.logMemoryStatus()
                }
                Player.STATE_READY -> {
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        durationMs = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                Player.STATE_ENDED -> {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                }
                Player.STATE_IDLE -> {
                    _playerState.value = _playerState.value.copy(isBuffering = false)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Player error: ${error.errorCodeName}")
            _playerState.value = _playerState.value.copy(
                error = "Playback error: ${error.message}",
                isBuffering = false,
                isPlaying = false
            )
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    fun loadAndPlay(url: String) {
        try {
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            _playerState.value = PlayerState(isBuffering = true)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load stream: $url")
            _playerState.value = _playerState.value.copy(error = "Failed to load stream")
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekForward(ms: Long = 10_000) {
        exoPlayer.seekTo((exoPlayer.currentPosition + ms).coerceAtMost(exoPlayer.duration))
    }

    fun seekBackward(ms: Long = 10_000) {
        exoPlayer.seekTo((exoPlayer.currentPosition - ms).coerceAtLeast(0))
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition

    fun stop() {
        exoPlayer.stop()
        memoryManager.trimMemory()
    }

    fun release() {
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        memoryManager.trimMemory()
    }
}
