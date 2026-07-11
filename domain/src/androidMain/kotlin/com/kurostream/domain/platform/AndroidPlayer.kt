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

package com.kurostream.domain.platform

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

actual class AndroidPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) : PlatformPlayer {
    private val mediaPlayer = MediaPlayer()
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _isPlaying = MutableStateFlow(false)
    
    override val playbackState: kotlinx.coroutines.flow.StateFlow<PlaybackState> = _playbackState
    override val currentPosition: kotlinx.coroutines.flow.StateFlow<Long> = _currentPosition
    override val duration: kotlinx.coroutines.flow.StateFlow<Long> = _duration
    override val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying
    
    init {
        mediaPlayer.setOnPreparedListener { mp ->
            _duration.value = mp.duration.toLong()
            _playbackState.value = PlaybackState.Playing(0)
            _isPlaying.value = true
            mp.start()
            startPositionUpdates()
        }
        mediaPlayer.setOnCompletionListener {
            _playbackState.value = PlaybackState.Completed
            _isPlaying.value = false
        }
        mediaPlayer.setOnErrorListener { _, what, extra ->
            _playbackState.value = PlaybackState.Error("MediaPlayer error: $what, $extra")
            _isPlaying.value = false
            true
        }
    }
    
    override fun initialize() {
    }
    
    override fun play(mediaUrl: String, headers: Map<String, String>? = null) {
        try {
            mediaPlayer.reset()
            val dataSource = if (headers != null && headers.isNotEmpty()) {
                val uri = Uri.parse(mediaUrl)
                val headersArray = headers.entries.map { "${it.key}: ${it.value}" }.toTypedArray()
                android.media.MediaPlayer.create(context, uri).also { player ->
                    player.setDataSource(context, uri, headersArray.toMap())
                }
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(mediaUrl))
            }
            mediaPlayer.prepareAsync()
            _playbackState.value = PlaybackState.Buffering(0f)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.Error(e.message ?: "Unknown error")
        }
    }
    
    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _playbackState.value = PlaybackState.Paused(_currentPosition.value)
            _isPlaying.value = false
        }
    }
    
    override fun resume() {
        if (!_isPlaying.value) {
            mediaPlayer.start()
            _playbackState.value = PlaybackState.Playing(_currentPosition.value)
            _isPlaying.value = true
            startPositionUpdates()
        }
    }
    
    override fun stop() {
        mediaPlayer.stop()
        _playbackState.value = PlaybackState.Idle
        _currentPosition.value = 0
        _isPlaying.value = false
    }
    
    override fun seekTo(position: Long) {
        mediaPlayer.seekTo(position.toInt())
    }
    
    override fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(
                mediaPlayer.playbackParams.setSpeed(speed)
            )
        }
    }
    
    override fun release() {
        mediaPlayer.release()
    }
    
    private fun startPositionUpdates() {
        scope.launch {
            while (mediaPlayer.isPlaying && !Thread.currentThread().isInterrupted) {
                _currentPosition.value = mediaPlayer.currentPosition.toLong()
                kotlinx.coroutines.delay(100)
            }
        }
    }
}