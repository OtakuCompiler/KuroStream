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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WebPlayer : PlatformPlayer {
    override suspend fun prepare(mediaUrl: String, headers: Map<String, String>) {
        throw UnsupportedOperationException("WebPlayer.prepare not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun play() {
        throw UnsupportedOperationException("WebPlayer.play not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun pause() {
        throw UnsupportedOperationException("WebPlayer.pause not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun seekTo(position: Long) {
        throw UnsupportedOperationException("WebPlayer.seekTo not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun stop() {
        throw UnsupportedOperationException("WebPlayer.stop not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun release() {
        throw UnsupportedOperationException("WebPlayer.release not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun setVolume(volume: Float) {
        throw UnsupportedOperationException("WebPlayer.setVolume not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        throw UnsupportedOperationException("WebPlayer.setPlaybackSpeed not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override suspend fun setLooping(looping: Boolean) {
        throw UnsupportedOperationException("WebPlayer.setLooping not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override val playbackState: Flow<PlaybackState>
        get() = flow { }

    override val currentPosition: Flow<Long>
        get() = flow { }

    override val duration: Flow<Long>
        get() = flow { }

    override val bufferedPosition: Flow<Long>
        get() = flow { }

    override val isPlaying: Flow<Boolean>
        get() = flow { }

    override val error: Flow<PlayerError?>
        get() = flow { }

    override fun setSurface(surface: Any?) {
        throw UnsupportedOperationException("WebPlayer.setSurface not implemented for webOS/Tizen. Implement in platform-specific module.")
    }

    override fun setSurfaceHolder(holder: Any?) {
        throw UnsupportedOperationException("WebPlayer.setSurfaceHolder not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
}
