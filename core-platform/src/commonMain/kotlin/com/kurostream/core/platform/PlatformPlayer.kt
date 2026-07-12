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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface PlatformPlayer {
    suspend fun prepare(mediaUrl: String, headers: Map<String, String> = emptyMap())
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(position: Long)
    suspend fun stop()
    suspend fun release()
    suspend fun setVolume(volume: Float)
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setLooping(looping: Boolean)
    
    val playbackState: Flow<PlaybackState>
    val currentPosition: Flow<Long>
    val duration: Flow<Long>
    val bufferedPosition: Flow<Long>
    val isPlaying: Flow<Boolean>
    val error: Flow<PlayerError?>
    
    fun setSurface(surface: Any?)
    fun setSurfaceHolder(holder: Any?)
}

data class PlaybackState(
    val state: State,
    val position: Long = 0,
    val duration: Long = 0,
    val bufferedPosition: Long = 0,
    val error: String? = null
) {
    enum class State {
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        BUFFERING,
        COMPLETED,
        ERROR,
        RELEASED
    }
}

data class PlayerError(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
)