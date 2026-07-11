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

package com.kurostream.players.vlc

import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import com.kurostream.players.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import timber.log.Timber

/**
 * Phase 25: libVLC Backend Implementation (FINAL)
 *
 * Real libVLC Android API (3.6.0-eap14):
 * - LibVLC constructor: LibVLC(Context, ArrayList<String> options)
 * - MediaPlayer.setMedia(Media) sets the media
 * - Tracks: getAudioTracks(), getSpuTracks(), getVideoTracks() return TrackDescription[]
 * - Track IDs are integers
 * - Events via MediaPlayer.EventListener with event.type and event.getTimeChanged(), etc.
 * - Video output via vlcVout (setVideoSurface, attachViews, detachViews)
 */
class VlcPlayer(private val context: Context) : PlayerInterface {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentMedia: Media? = null

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

    private val trackIdToVlcId = mutableMapOf<String, Int>()
    private var nextTrackId = 0

    init {
        initializeVlc()
    }

    private fun initializeVlc() {
        try {
            val options = ArrayList<String>().apply {
                add("--network-caching=2000")
                add("--file-caching=1000")
                add("--live-caching=1500")
                add("--disc-caching=3000")
                add("--codec=mediacodec,iomx,all")
                add("--aout=opensles")
                add("--vout=android-display")
                add("--subsdec-encoding=UTF-8")
                add("--enable-mkv")
                add("--enable-hevc")
                add("--audio-passthrough")
                add("--deinterlace=auto")
                add("--deinterlace-mode=yadif")
                add("--no-video-title-show")
                add("--stats-overlay")
            }

            libVLC = LibVLC(context, options)
            mediaPlayer = MediaPlayer(libVLC).apply {
                setEventListener(VlcEventListener())
            }

            Timber.i("VLC initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize VLC")
            _playbackState.value = PlaybackState.Error(
                PlaybackError.DecoderError(e.message ?: "VLC init failed")
            )
        }
    }

    override fun play() {
        mediaPlayer?.play()
        _playbackState.value = PlaybackState.Playing
        startPositionUpdates()
    }

    override fun pause() {
        mediaPlayer?.pause()
        _playbackState.value = PlaybackState.Paused
        stopPositionUpdates()
    }

    override fun stop() {
        mediaPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
        stopPositionUpdates()
        _positionMs.value = 0
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer?.time = positionMs
    }

    override fun seekRelative(deltaMs: Long) {
        val newTime = (_positionMs.value + deltaMs).coerceAtLeast(0)
        mediaPlayer?.time = newTime
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        mediaPlayer?.rate = clamped
        _speed.value = clamped
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaPlayer?.volume = (clamped * 100).toInt()
        _volume.value = clamped
    }

    override fun setMute(muted: Boolean) {
        if (muted) {
            mediaPlayer?.volume = 0
        } else {
            mediaPlayer?.volume = (_volume.value * 100).toInt()
        }
        _isMuted.value = muted
    }

    override fun loadMedia(uri: String, headers: Map<String, String>) {
        loadMedia(MediaItem(uri, headers = headers))
    }

    override fun loadMedia(mediaItem: MediaItem) {
        _playbackState.value = PlaybackState.Loading

        currentMedia?.release()

        currentMedia = Media(libVLC, Uri.parse(mediaItem.uri)).apply {
            mediaItem.headers.forEach { (key, value) ->
                addOption(":http-$key=$value")
            }
            addOption(":http-user-agent=MediaPlayer/1.0")
            setHWDecoderEnabled(true, false)
            parse(Media.ParseNetworkOnly)
        }

        mediaItem.subtitles.forEach { sub ->
            currentMedia?.addOption(":sub-file=${sub.uri}")
        }

        mediaPlayer?.media = currentMedia

        scope.launch {
            delay(500)
            extractTracks()
        }

        if (mediaItem.startPositionMs > 0) {
            mediaPlayer?.time = mediaItem.startPositionMs
        }

        mediaPlayer?.play()
        startPositionUpdates()
        startDiagnosticsUpdates()
    }

    override fun release() {
        stopPositionUpdates()
        stopDiagnosticsUpdates()
        scope.cancel()
        mediaPlayer?.release()
        currentMedia?.release()
        libVLC?.release()
        mediaPlayer = null
        currentMedia = null
        libVLC = null
    }

    override fun attachSurface(surface: Surface) {
        mediaPlayer?.vlcVout?.setVideoSurface(surface, null)
        mediaPlayer?.vlcVout?.attachViews()
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer?.vlcVout?.setVideoSurface(holder.surface, holder)
                mediaPlayer?.vlcVout?.attachViews()
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                mediaPlayer?.vlcVout?.setWindowSize(width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mediaPlayer?.vlcVout?.detachViews()
            }
        })
    }

    override fun attachTextureView(textureView: TextureView) {
        Timber.w("VLC backend prefers SurfaceView over TextureView")
    }

    override fun detachSurface() {
        mediaPlayer?.vlcVout?.detachViews()
    }

    override fun setVideoScalingMode(mode: VideoScalingMode) {
        val vlcScale = when (mode) {
            VideoScalingMode.FIT -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
            VideoScalingMode.CROP -> MediaPlayer.ScaleType.SURFACE_FILL
            VideoScalingMode.STRETCH -> MediaPlayer.ScaleType.SURFACE_FILL
            VideoScalingMode.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
        }
        mediaPlayer?.videoScale = vlcScale
    }

    override fun selectAudioTrack(trackId: String) {
        val vlcId = trackIdToVlcId[trackId] ?: return
        mediaPlayer?.audioTrack = vlcId
        updateTrackSelection(_audioTracks, trackId)
    }

    override fun selectSubtitleTrack(trackId: String?) {
        if (trackId == null) {
            mediaPlayer?.spuTrack = -1
        } else {
            val vlcId = trackIdToVlcId[trackId] ?: return
            mediaPlayer?.spuTrack = vlcId
        }
        updateTrackSelection(_subtitleTracks, trackId)
    }

    override fun selectVideoTrack(trackId: String) {
        val vlcId = trackIdToVlcId[trackId] ?: return
        mediaPlayer?.videoTrack = vlcId
        updateTrackSelection(_videoTracks, trackId)
    }

    override fun setAudioDelay(delayMs: Long) {
        mediaPlayer?.audioDelay = delayMs * 1000
    }

    override fun setSubtitleDelay(delayMs: Long) {
        mediaPlayer?.spuDelay = delayMs * 1000
    }

    override fun setLooping(looping: Boolean) {
        currentMedia?.addOption(":input-repeat=${if (looping) -1 else 0}")
    }

    override fun setAudioFocus(enabled: Boolean) {}
    override fun setWakeLock(enabled: Boolean) {}

    override fun enableDiagnosticsOverlay(enabled: Boolean) {
        diagnosticsOverlayEnabled = enabled
        if (enabled) startDiagnosticsUpdates() else stopDiagnosticsUpdates()
    }

    override val backendType: PlayerBackend = PlayerBackend.VLC
    override val backendVersion: String
        get() = libVLC?.version ?: "unknown"

    private fun extractTracks() {
        val mp = mediaPlayer ?: return

        val audioList = mp.audioTracks?.mapIndexed { index, track ->
            val id = generateTrackId("audio", index)
            trackIdToVlcId[id] = track.id
            TrackInfo(
                id = id,
                type = TrackType.AUDIO,
                language = track.language,
                codec = track.codec,
                bitrate = track.bitrate.toLong(),
                isSelected = track.id == mp.audioTrack,
                metadata = mapOf(
                    "channels" to track.channels.toString(),
                    "sampleRate" to track.sampleRate.toString()
                )
            )
        } ?: emptyList()
        _audioTracks.value = audioList

        val subList = mp.spuTracks?.mapIndexed { index, track ->
            val id = generateTrackId("sub", index)
            trackIdToVlcId[id] = track.id
            TrackInfo(
                id = id,
                type = TrackType.SUBTITLE,
                language = track.language,
                codec = track.codec,
                isSelected = track.id == mp.spuTrack,
                metadata = mapOf("encoding" to (track.encoding ?: "UTF-8"))
            )
        } ?: emptyList()
        _subtitleTracks.value = subList

        val videoList = mp.videoTracks?.mapIndexed { index, track ->
            val id = generateTrackId("video", index)
            trackIdToVlcId[id] = track.id
            TrackInfo(
                id = id,
                type = TrackType.VIDEO,
                language = null,
                codec = track.codec,
                bitrate = track.bitrate.toLong(),
                isSelected = track.id == mp.videoTrack,
                metadata = mapOf(
                    "width" to track.width.toString(),
                    "height" to track.height.toString(),
                    "frameRate" to track.frameRate.toString()
                )
            )
        } ?: emptyList()
        _videoTracks.value = videoList
    }

    private fun generateTrackId(type: String, index: Int): String {
        return "vlc_${type}_${index}_${nextTrackId++}"
    }

    private fun updateTrackSelection(flow: MutableStateFlow<List<TrackInfo>>, selectedId: String?) {
        flow.value = flow.value.map { it.copy(isSelected = it.id == selectedId) }
    }

    private inner class VlcEventListener : MediaPlayer.EventListener {
        override fun onEvent(event: MediaPlayer.Event) {
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    _playbackState.value = PlaybackState.Loading
                }
                MediaPlayer.Event.Playing -> {
                    _playbackState.value = PlaybackState.Playing
                    startPositionUpdates()
                }
                MediaPlayer.Event.Paused -> {
                    _playbackState.value = PlaybackState.Paused
                }
                MediaPlayer.Event.Stopped -> {
                    _playbackState.value = PlaybackState.Idle
                    stopPositionUpdates()
                }
                MediaPlayer.Event.EndReached -> {
                    _playbackState.value = PlaybackState.Ended
                    stopPositionUpdates()
                }
                MediaPlayer.Event.Buffering -> {
                    _playbackState.value = PlaybackState.Buffering
                }
                MediaPlayer.Event.EncounteredError -> {
                    _playbackState.value = PlaybackState.Error(
                        PlaybackError.DecoderError("VLC playback error: ${event.type}")
                    )
                    stopPositionUpdates()
                }
                MediaPlayer.Event.TimeChanged -> {
                    _positionMs.value = event.timeChanged
                }
                MediaPlayer.Event.LengthChanged -> {
                    _durationMs.value = event.lengthChanged
                }
                MediaPlayer.Event.Vout -> {
                    // Video output ready
                }
                MediaPlayer.Event.ESAdded,
                MediaPlayer.Event.ESDeleted -> {
                    scope.launch { extractTracks() }
                }
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_playbackState.value == PlaybackState.Playing) {
                    _positionMs.value = mediaPlayer?.time ?: 0L
                    _durationMs.value = mediaPlayer?.length ?: 0L
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
                    val mp = mediaPlayer ?: continue
                    val stats = mp.media?.stats

                    _diagnostics.value = _diagnostics.value.copy(
                        bufferDurationMs = stats?.demuxBitrate?.let { (it * 1000).toLong() } ?: 0L,
                        currentBitrate = stats?.inputBitrate?.toLong() ?: 0L,
                        droppedFrames = stats?.lostPictures ?: 0,
                        renderedFrames = stats?.displayedPictures ?: 0,
                        currentFps = stats?.fps?.toFloat() ?: 0f,
                        decoderName = "VLC",
                        videoCodec = mp.currentVideoTrack?.codec ?: "unknown",
                        audioCodec = mp.currentAudioTrack?.codec ?: "unknown",
                        videoResolution = "${mp.currentVideoTrack?.width ?: 0}x${mp.currentVideoTrack?.height ?: 0}",
                        isHardwareDecoding = mp.currentVideoTrack?.isHardwareAccelerated ?: false,
                        timestamp = System.currentTimeMillis()
                    )
                }
                delay(1000)
            }
        }
    }

    private fun stopDiagnosticsUpdates() {
        diagnosticsJob?.cancel()
        diagnosticsJob = null
    }
}
