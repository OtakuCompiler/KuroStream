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

package com.kurostream.players.media3

import android.content.Context
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroup
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelectionOverride
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.kurostream.players.buffer.DiskBackedLoadControl
import com.kurostream.players.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID

/**
 * Phase 26: Media3 (ExoPlayer) Fallback Backend (FINAL)
 * ExoPlayer 1.4.0 API validated.
 * Updated with disk-backed load control for low memory footprint.
 */
@UnstableApi
class Media3Player(private val context: Context) : PlayerInterface {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private val trackSelector: DefaultTrackSelector
    private var diskBackedLoadControl: DiskBackedLoadControl? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    override val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    override val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val videoTracks: StateFlow<List<TrackInfo>> = _videoTracks.asStateFlow()

    private val _diagnostics = MutableStateFlow(PlaybackDiagnostics())
    override val diagnostics: StateFlow<PlaybackDiagnostics> = _diagnostics.asStateFlow()

    private var diagnosticsOverlayEnabled = false
    private var positionUpdateJob: Job? = null
    private var diagnosticsJob: Job? = null

    private val trackIdToFormat = mutableMapOf<String, Format>()
    private var nextTrackId = 0

    private var droppedFrames = 0
    private var renderedFrames = 0

    init {
        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters()
                .setAllowVideoMixedDecoderSupportAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
            )
        }
        initializeExoPlayer()
    }

    private fun initializeExoPlayer() {
        try {
            val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
                .setResetOnNetworkTypeChange(true)
                .build()

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setLoadErrorHandlingPolicy(AdaptiveLoadErrorPolicy())

            // Initialize disk-backed load control for low memory footprint
            diskBackedLoadControl = DiskBackedLoadControl.getInstance(context, 50_000_000) // 50MB target buffer

            exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setBandwidthMeter(bandwidthMeter)
                .setMediaSourceFactory(mediaSourceFactory)
                .setRenderersFactory(DefaultRenderersFactory(context).apply {
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setEnableDecoderFallback(true)
                })
                .setSeekParameters(SeekParameters.EXACT)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setLoadControl(diskBackedLoadControl!!) // Use disk-backed load control
                .build()
                .apply {
                    addListener(ExoPlayerListener())
                    addAnalyticsListener(ExoAnalyticsListener())
                    playWhenReady = false
                }

            Timber.i("ExoPlayer initialized successfully with disk-backed load control")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ExoPlayer")
            _playbackState.value = PlaybackState.Error(
                PlaybackError.DecoderError(e.message ?: "ExoPlayer init failed")
            )
        }
    }

    override fun play() {
        exoPlayer?.play()
        _playbackState.value = PlaybackState.Playing
        startPositionUpdates()
    }

    override fun pause() {
        exoPlayer?.pause()
        _playbackState.value = PlaybackState.Paused
        stopPositionUpdates()
    }

    override fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
        stopPositionUpdates()
        _positionMs.value = 0
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun seekRelative(deltaMs: Long) {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition + deltaMs).coerceAtLeast(0)
            player.seekTo(newPos)
        }
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer?.setPlaybackSpeed(clamped)
        _speed.value = clamped
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        _volume.value = clamped
    }

    override fun setMute(muted: Boolean) {
        exoPlayer?.let {
            it.volume = if (muted) 0f else _volume.value
        }
        _isMuted.value = muted
    }

    override fun loadMedia(uri: String, headers: Map<String, String>) {
        loadMedia(MediaItem(uri, headers = headers))
    }

    override fun loadMedia(mediaItem: com.kurostream.players.core.MediaItem) {
        _playbackState.value = PlaybackState.Loading

        val mediaItemBuilder = androidx.media3.common.MediaItem.Builder()
            .setUri(mediaItem.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(mediaItem.title)
                    .build()
            )

        if (mediaItem.headers.isNotEmpty()) {
            val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mediaItem.headers)
        }

        mediaItem.drmConfig?.let { drm ->
            val drmUuid = when (drm.drmSchemeUuid.lowercase()) {
                "widevine" -> C.WIDEVINE_UUID
                "playready" -> C.PLAYREADY_UUID
                "clearkey" -> C.CLEARKEY_UUID
                else -> UUID.fromString(drm.drmSchemeUuid)
            }

            val drmCallback = LocalMediaDrmCallback(byteArrayOf())
            val drmSessionManager = DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(drmUuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(drmCallback)
        }

        val subtitleConfigurations = mediaItem.subtitles.map { sub ->
            androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(
                android.net.Uri.parse(sub.uri)
            )
                .setMimeType(sub.mimeType)
                .setLanguage(sub.language)
                .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }

        if (subtitleConfigurations.isNotEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
        }

        exoPlayer?.setMediaItem(mediaItemBuilder.build(), mediaItem.startPositionMs)
        exoPlayer?.prepare()

        if (mediaItem.startPositionMs > 0) {
            exoPlayer?.seekTo(mediaItem.startPositionMs)
        }

        startPositionUpdates()
        startDiagnosticsUpdates()
    }

    override fun release() {
        stopPositionUpdates()
        stopDiagnosticsUpdates()
        scope.cancel()
        exoPlayer?.release()
        exoPlayer = null
        
        // Release disk-backed load control
        diskBackedLoadControl?.release()
        diskBackedLoadControl = null
    }

    override fun attachSurface(surface: Surface) {
        exoPlayer?.setVideoSurface(surface)
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        exoPlayer?.setVideoSurfaceView(surfaceView)
    }

    override fun attachTextureView(textureView: TextureView) {
        exoPlayer?.setVideoTextureView(textureView)
    }

    override fun detachSurface() {
        exoPlayer?.setVideoSurface(null)
    }

    override fun setVideoScalingMode(mode: VideoScalingMode) {
        val exoMode = when (mode) {
            VideoScalingMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            VideoScalingMode.CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            VideoScalingMode.STRETCH -> C.VIDEO_SCALING_MODE_DEFAULT
            VideoScalingMode.ORIGINAL -> C.VIDEO_SCALING_MODE_DEFAULT
        }
        exoPlayer?.videoScalingMode = exoMode
    }

    override fun selectAudioTrack(trackId: String) {
        val format = trackIdToFormat[trackId] ?: return
        val trackGroup = findTrackGroup(format) ?: return

        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setOverrideForType(TrackSelectionOverride(trackGroup, trackGroup.indexOf(format)))
        )
        updateTrackSelection(_audioTracks, trackId)
    }

    override fun selectSubtitleTrack(trackId: String?) {
        if (trackId == null) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
            )
        } else {
            val format = trackIdToFormat[trackId] ?: return
            val trackGroup = findTrackGroup(format) ?: return
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setOverrideForType(TrackSelectionOverride(trackGroup, trackGroup.indexOf(format)))
            )
        }
        updateTrackSelection(_subtitleTracks, trackId)
    }

    override fun selectVideoTrack(trackId: String) {
        val format = trackIdToFormat[trackId] ?: return
        val trackGroup = findTrackGroup(format) ?: return

        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setOverrideForType(TrackSelectionOverride(trackGroup, trackGroup.indexOf(format)))
        )
        updateTrackSelection(_videoTracks, trackId)
    }

    override fun setAudioDelay(delayMs: Long) {
        Timber.w("Audio delay not supported in Media3 fallback")
    }

    override fun setSubtitleDelay(delayMs: Long) {
        Timber.w("Subtitle delay not supported in Media3 fallback")
    }

    override fun setLooping(looping: Boolean) {
        exoPlayer?.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun setAudioFocus(enabled: Boolean) {
        exoPlayer?.audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    override fun setWakeLock(enabled: Boolean) {
        exoPlayer?.let {
            (it as? ExoPlayer)?.setWakeMode(if (enabled) C.WAKE_MODE_NETWORK else C.WAKE_MODE_NONE)
        }
    }

    override fun enableDiagnosticsOverlay(enabled: Boolean) {
        diagnosticsOverlayEnabled = enabled
        if (enabled) startDiagnosticsUpdates() else stopDiagnosticsUpdates()
    }

    override val backendType: PlayerBackend = PlayerBackend.MEDIA3
    override val backendVersion: String
        get() = ExoPlayerLibraryInfo.VERSION

    private inner class ExoPlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = when (state) {
                Player.STATE_IDLE -> PlaybackState.Idle
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> if (exoPlayer?.isPlaying == true)
                    PlaybackState.Playing else PlaybackState.Ready
                Player.STATE_ENDED -> PlaybackState.Ended
                else -> PlaybackState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playbackState.value = PlaybackState.Playing
                startPositionUpdates()
            } else if (_playbackState.value == PlaybackState.Playing) {
                _playbackState.value = PlaybackState.Paused
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val playbackError = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    PlaybackError.NetworkError(error.message ?: "Network error")
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                    PlaybackError.DecoderError(error.message ?: "Decoder error")
                PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    PlaybackError.SourceError(error.message ?: "Source error")
                else -> PlaybackError.UnknownError(error.message ?: "Unknown error")
            }
            _playbackState.value = PlaybackState.Error(playbackError)
            stopPositionUpdates()
        }

        override fun onTracksChanged(tracks: Tracks) {
            extractTracks(tracks)
        }
    }

    private inner class ExoAnalyticsListener : AnalyticsListener {
        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            this@Media3Player.droppedFrames += droppedFrames
        }

        override fun onRenderedFirstFrame(
            eventTime: AnalyticsListener.EventTime,
            output: Any,
            renderTimeMs: Long
        ) {
            renderedFrames++
        }

        override fun onVideoDecoderReleased(
            eventTime: AnalyticsListener.EventTime,
            videoDecoderName: String
        ) {
            _diagnostics.value = _diagnostics.value.copy(decoderName = videoDecoderName)
        }

        override fun onBandwidthEstimate(
            eventTime: AnalyticsListener.EventTime,
            totalLoadTimeMs: Int,
            totalBytesLoaded: Long,
            bitrateEstimate: Long
        ) {
            _diagnostics.value = _diagnostics.value.copy(
                networkSpeedBps = bitrateEstimate,
                currentBitrate = bitrateEstimate
            )
        }
    }

    private fun extractTracks(tracks: Tracks) {
        trackIdToFormat.clear()

        val audioList = mutableListOf<TrackInfo>()
        val subtitleList = mutableListOf<TrackInfo>()
        val videoList = mutableListOf<TrackInfo>()

        tracks.groups.forEach { group ->
            val trackType = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackType.AUDIO
                C.TRACK_TYPE_VIDEO -> TrackType.VIDEO
                C.TRACK_TYPE_TEXT -> TrackType.SUBTITLE
                else -> TrackType.UNKNOWN
            }

            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val id = generateTrackId(trackType.name.lowercase(), nextTrackId++)
                trackIdToFormat[id] = format

                val trackInfo = TrackInfo(
                    id = id,
                    type = trackType,
                    language = format.language,
                    codec = format.sampleMimeType,
                    bitrate = format.bitrate.toLong(),
                    isSelected = group.isTrackSelected(i),
                    metadata = buildMap {
                        put("id", format.id ?: "unknown")
                        if (trackType == TrackType.VIDEO) {
                            put("width", format.width.toString())
                            put("height", format.height.toString())
                            put("frameRate", format.frameRate.toString())
                        }
                        if (trackType == TrackType.AUDIO) {
                            put("channels", format.channelCount.toString())
                            put("sampleRate", format.sampleRate.toString())
                        }
                    }
                )

                when (trackType) {
                    TrackType.AUDIO -> audioList.add(trackInfo)
                    TrackType.VIDEO -> videoList.add(trackInfo)
                    TrackType.SUBTITLE -> subtitleList.add(trackInfo)
                    else -> {}
                }
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
        _videoTracks.value = videoList
    }

    private fun findTrackGroup(format: Format): TrackGroup? {
        exoPlayer?.currentTracks?.groups?.forEach { group ->
            for (i in 0 until group.length) {
                if (group.getTrackFormat(i) == format) {
                    return group.mediaTrackGroup
                }
            }
        }
        return null
    }

    private fun generateTrackId(type: String, index: Int): String {
        return "exo_${type}_${index}"
    }

    private fun updateTrackSelection(flow: MutableStateFlow<List<TrackInfo>>, selectedId: String?) {
        flow.value = flow.value.map { it.copy(isSelected = it.id == selectedId) }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                exoPlayer?.let { player ->
                    _positionMs.value = player.currentPosition
                    _durationMs.value = player.duration.coerceAtLeast(0)
                    _bufferedPositionMs.value = player.bufferedPosition
                }
                delay(250)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun startDiagnosticsUpdates() {
        diagnosticsJob?.cancel()
        diagnosticsJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (diagnosticsOverlayEnabled) {
                    exoPlayer?.let { player ->
                        val videoFormat = player.videoFormat
                        val audioFormat = player.audioFormat

                        _diagnostics.value = _diagnostics.value.copy(
                            bufferDurationMs = player.bufferedPosition - player.currentPosition,
                            bufferedPercentage = player.bufferedPercentage,
                            droppedFrames = droppedFrames,
                            renderedFrames = renderedFrames,
                            currentFps = videoFormat?.frameRate ?: 0f,
                            videoCodec = videoFormat?.sampleMimeType ?: "unknown",
                            audioCodec = audioFormat?.sampleMimeType ?: "unknown",
                            videoResolution = "${videoFormat?.width ?: 0}x${videoFormat?.height ?: 0}",
                            isHardwareDecoding = videoFormat?.decoderName?.contains("OMX", ignoreCase = true) ?: false,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopDiagnosticsUpdates() {
        diagnosticsJob?.cancel()
        diagnosticsJob = null
    }

    private class AdaptiveLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {
        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            return if (loadErrorInfo.error is java.net.UnknownHostException) {
                5000
            } else {
                super.getRetryDelayMsFor(loadErrorInfo)
            }
        }
    }
}
