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

class WebPlayer : PlatformPlayer {
    override fun initialize() {
        throw UnsupportedOperationException("WebPlayer.initialize not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun play(mediaUrl: String, headers: Map<String, String>? = null) {
        throw UnsupportedOperationException("WebPlayer.play not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun pause() {
        throw UnsupportedOperationException("WebPlayer.pause not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun resume() {
        throw UnsupportedOperationException("WebPlayer.resume not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun stop() {
        throw UnsupportedOperationException("WebPlayer.stop not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun seekTo(position: Long) {
        throw UnsupportedOperationException("WebPlayer.seekTo not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun setVolume(volume: Float) {
        throw UnsupportedOperationException("WebPlayer.setVolume not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        throw UnsupportedOperationException("WebPlayer.setPlaybackSpeed not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
    
    override val playbackState: kotlinx.coroutines.flow.Flow<PlaybackState>
        get() = kotlinx.coroutines.flow.flow { }
    
    override val currentPosition: kotlinx.coroutines.flow.Flow<Long>
        get() = kotlinx.coroutines.flow.flow { }
    
    override val duration: kotlinx.coroutines.flow.Flow<Long>
        get() = kotlinx.coroutines.flow.flow { }
    
    override val isPlaying: kotlinx.coroutines.flow.Flow<Boolean>
        get() = kotlinx.coroutines.flow.flow { }
    
    override fun release() {
        throw UnsupportedOperationException("WebPlayer.release not implemented for webOS/Tizen. Implement in platform-specific module.")
    }
}