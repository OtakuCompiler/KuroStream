package com.kurostream.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JvmPlayer : PlatformPlayer {
    override suspend fun prepare(mediaUrl: String, headers: Map<String, String>) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun play() {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun pause() {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun seekTo(position: Long) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun stop() {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun release() {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun setVolume(volume: Float) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun setPlaybackSpeed(speed: Float) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override suspend fun setLooping(looping: Boolean) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override val playbackState: Flow<PlaybackState> = flow { }
    override val currentPosition: Flow<Long> = flow { }
    override val duration: Flow<Long> = flow { }
    override val bufferedPosition: Flow<Long> = flow { }
    override val isPlaying: Flow<Boolean> = flow { }
    override val error: Flow<PlayerError?> = flow { }
    override fun setSurface(surface: Any?) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
    override fun setSurfaceHolder(holder: Any?) {
        throw UnsupportedOperationException("JvmPlayer not implemented for JVM.")
    }
}
