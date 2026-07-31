package com.kurostream.players.selector

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common playback engine interface.
 *
 * Each backend (Media3, VLC, mpv) implements this so [PlayerViewModel] can be
 * backend-agnostic. The method set mirrors what the current [PlayerViewModel]
 * actually calls on [androidx.media3.common.Player].
 */
interface PlaybackEngine {

    interface Listener {
        fun onPlaybackStateChanged(state: PlaybackState)
        fun onIsPlayingChanged(playing: Boolean)
        fun onError(message: String?)
    }

    enum class PlaybackState { IDLE, BUFFERING, READY, ENDED, ERROR }

    suspend fun initialize()

    fun setMedia(uri: String, title: String?, startPositionMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekForward()
    fun seekBack()
    fun setPlaybackSpeed(speed: Float)

    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val bufferedPosition: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val playbackState: StateFlow<PlaybackState>
    val errorMessage: SharedFlow<String?>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    fun release()

    /** Backend-specific native handle for UI that requires it (e.g. [androidx.media3.ui.PlayerView]). */
    fun nativePlayer(): Any? = null
}
