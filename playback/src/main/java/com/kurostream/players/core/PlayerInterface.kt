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

package com.kurostream.players.core

import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 22: Core Player Abstraction Interface (FINAL)
 * Unified API across all playback backends (mpv, VLC, Media3)
 */
interface PlayerInterface {

    // Playback Control
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekRelative(deltaMs: Long)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setMute(muted: Boolean)

    // State Observation (Kotlin Flow)
    val playbackState: StateFlow<PlaybackState>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val bufferedPositionMs: StateFlow<Long>
    val speed: StateFlow<Float>
    val volume: StateFlow<Float>
    val isMuted: StateFlow<Boolean>

    // Media Loading
    fun loadMedia(uri: String, headers: Map<String, String> = emptyMap())
    fun loadMedia(mediaItem: MediaItem)
    fun release()

    // Video Surface
    fun attachSurface(surface: Surface)
    fun attachSurfaceView(surfaceView: SurfaceView)
    fun attachTextureView(textureView: TextureView)
    fun detachSurface()
    fun setVideoScalingMode(mode: VideoScalingMode)

    // Audio / Subtitle Tracks
    val audioTracks: StateFlow<List<TrackInfo>>
    val subtitleTracks: StateFlow<List<TrackInfo>>
    val videoTracks: StateFlow<List<TrackInfo>>

    fun selectAudioTrack(trackId: String)
    fun selectSubtitleTrack(trackId: String?)
    fun selectVideoTrack(trackId: String)
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)

    // Advanced Features
    fun setLooping(looping: Boolean)
    fun setAudioFocus(enabled: Boolean)
    fun setWakeLock(enabled: Boolean)

    // Diagnostics (Phase 30)
    val diagnostics: StateFlow<PlaybackDiagnostics>
    fun enableDiagnosticsOverlay(enabled: Boolean)

    // Backend Info
    val backendType: PlayerBackend
    val backendVersion: String
}

/**
 * Playback state machine
 */
sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Loading : PlaybackState()
    data object Buffering : PlaybackState()
    data object Ready : PlaybackState()
    data object Playing : PlaybackState()
    data object Paused : PlaybackState()
    data object Ended : PlaybackState()
    data class Error(val error: PlaybackError) : PlaybackState()
}

/**
 * Playback error types
 */
sealed class PlaybackError(val message: String) {
    class NetworkError(msg: String) : PlaybackError(msg)
    class DecoderError(msg: String) : PlaybackError(msg)
    class SourceError(msg: String) : PlaybackError(msg)
    class RendererError(msg: String) : PlaybackError(msg)
    class UnknownError(msg: String) : PlaybackError(msg)
}

/**
 * Media item with metadata
 */
data class MediaItem(
    val uri: String,
    val title: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleSource> = emptyList(),
    val startPositionMs: Long = 0,
    val drmConfig: DrmConfig? = null
)

data class SubtitleSource(
    val uri: String,
    val language: String,
    val mimeType: String = "application/x-subrip",
    val isDefault: Boolean = false
)

data class DrmConfig(
    val drmSchemeUuid: String,
    val licenseUri: String,
    val keyRequestProperties: Map<String, String> = emptyMap()
)

/**
 * Track information
 */
data class TrackInfo(
    val id: String,
    val type: TrackType,
    val language: String?,
    val codec: String?,
    val bitrate: Long?,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

enum class TrackType { AUDIO, VIDEO, SUBTITLE, UNKNOWN }

enum class PlayerBackend {
    MPV, VLC, MEDIA3
}

enum class VideoScalingMode {
    FIT,
    CROP,
    STRETCH,
    ORIGINAL
}

/**
 * Playback diagnostics data (Phase 30)
 */
data class PlaybackDiagnostics(
    val bufferDurationMs: Long = 0,
    val bufferedPercentage: Int = 0,
    val currentBitrate: Long = 0,
    val droppedFrames: Int = 0,
    val renderedFrames: Int = 0,
    val currentFps: Float = 0f,
    val displayRefreshRate: Float = 0f,
    val contentFrameRate: Float = 0f,
    val networkSpeedBps: Long = 0,
    val decoderName: String = "",
    val videoCodec: String = "",
    val audioCodec: String = "",
    val videoResolution: String = "",
    val isHardwareDecoding: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
