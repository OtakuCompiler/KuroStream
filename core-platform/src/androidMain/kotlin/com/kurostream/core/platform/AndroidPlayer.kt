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

package com.kurostream.core.platform

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) : PlatformPlayer {

    private val mediaPlayer = MediaPlayer()
    private var pendingMediaUrl: String? = null
    private var pendingHeaders: Map<String, String>? = null

    private val _playbackState = MutableStateFlow(
        PlaybackState(state = PlaybackState.State.IDLE)
    )
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _bufferedPosition = MutableStateFlow(0L)
    private val _isPlaying = MutableStateFlow(false)
    private val _error = MutableStateFlow<PlayerError?>(null)

    override val playbackState = _playbackState.asStateFlow()
    override val currentPosition = _currentPosition.asStateFlow()
    override val duration = _duration.asStateFlow()
    override val bufferedPosition = _bufferedPosition.asStateFlow()
    override val isPlaying = _isPlaying.asStateFlow()
    override val error = _error.asStateFlow()

    init {
        mediaPlayer.setOnPreparedListener { mp ->
            _duration.value = mp.duration.toLong()
            _playbackState.value = PlaybackState(
                state = PlaybackState.State.PLAYING,
                position = _currentPosition.value,
                duration = mp.duration.toLong()
            )
            _isPlaying.value = true
            mp.start()
            startPositionUpdates()
        }
        mediaPlayer.setOnCompletionListener {
            _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.COMPLETED)
            _isPlaying.value = false
        }
        mediaPlayer.setOnErrorListener { _, what, extra ->
            _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.ERROR)
            _error.value = PlayerError(code = what, message = "MediaPlayer error: $what, $extra")
            _isPlaying.value = false
            true
        }
    }

    override suspend fun prepare(mediaUrl: String, headers: Map<String, String>) {
        pendingMediaUrl = mediaUrl
        pendingHeaders = headers
        _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.PREPARING)
        try {
            mediaPlayer.reset()
            val uri = Uri.parse(mediaUrl)
            if (headers.isNotEmpty()) {
                val headersArray = headers.entries.map { "${it.key}: ${it.value}" }.toTypedArray()
                mediaPlayer.setDataSource(context, uri, headersArray.toMap())
            } else {
                mediaPlayer.setDataSource(context, uri)
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.ERROR)
            _error.value = PlayerError(code = -1, message = e.message ?: "Unknown error", cause = e)
        }
    }

    override suspend fun play() {
        try {
            mediaPlayer.start()
            _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.PLAYING)
            _isPlaying.value = true
            startPositionUpdates()
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.ERROR)
            _error.value = PlayerError(code = -1, message = e.message ?: "Unknown error", cause = e)
        }
    }

    override suspend fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _playbackState.value = _playbackState.value.copy(
                state = PlaybackState.State.PAUSED,
                position = _currentPosition.value
            )
            _isPlaying.value = false
        }
    }

    override suspend fun seekTo(position: Long) {
        mediaPlayer.seekTo(position.toInt())
    }

    override suspend fun stop() {
        mediaPlayer.stop()
        _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.IDLE, position = 0)
        _currentPosition.value = 0
        _isPlaying.value = false
    }

    override suspend fun release() {
        mediaPlayer.release()
        _playbackState.value = _playbackState.value.copy(state = PlaybackState.State.RELEASED)
    }

    override suspend fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(mediaPlayer.playbackParams.setSpeed(speed))
        }
    }

    override suspend fun setLooping(looping: Boolean) {
        mediaPlayer.isLooping = looping
    }

    override fun setSurface(surface: Any?) {
        if (surface is Surface) {
            mediaPlayer.setSurface(surface)
        }
    }

    override fun setSurfaceHolder(holder: Any?) {
        if (holder is SurfaceHolder) {
            mediaPlayer.setDisplay(holder)
        }
    }

    private fun startPositionUpdates() {
        scope.launch(Dispatchers.Main) {
            while (mediaPlayer.isPlaying && !Thread.currentThread().isInterrupted) {
                _currentPosition.value = mediaPlayer.currentPosition.toLong()
                kotlinx.coroutines.delay(100)
            }
        }
    }
}
