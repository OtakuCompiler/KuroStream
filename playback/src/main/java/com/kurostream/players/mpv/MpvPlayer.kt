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

package com.kurostream.players.mpv

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import com.kurostream.players.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class MpvPlayer(private val context: Context) : PlayerInterface {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mpvHandle: Long = 0
    private var currentSurface: Surface? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

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
    private var eventLoopJob: Job? = null

    private val trackIdToMpvId = ConcurrentHashMap<String, Int>()
    private var nextTrackId = 0

    init {
        initializeMpv()
    }

    private fun initializeMpv() {
        try {
            MPVLib.init()
            mpvHandle = MPVLib.create()
            if (mpvHandle == 0L) {
                throw RuntimeException("Failed to create mpv instance")
            }

            setOption("config", "yes")
            setOption("config-dir", context.filesDir.absolutePath + "/mpv")
            setOption("hwdec", "auto-safe")
            setOption("hwdec-codecs", "all")
            setOption("vo", "gpu")
            setOption("gpu-context", "android")
            setOption("ao", "audiotrack,opensles")
            setOption("demuxer-max-bytes", "50M")
            setOption("demuxer-max-back-bytes", "25M")
            setOption("cache-secs", "120")
            setOption("cache-pause-wait", "10")
            setOption("network-timeout", "60")
            setOption("user-agent", "MediaPlayer/1.0")
            setOption("sub-fonts-dir", context.filesDir.absolutePath + "/fonts")
            setOption("sub-ass-override", "force")
            setOption("vd-lavc-threads", Runtime.getRuntime().availableProcessors().toString())
            setOption("vd-lavc-fast", "yes")
            setOption("hdr-compute-peak", "yes")
            setOption("tone-mapping", "hable")
            setOption("tone-mapping-param", "0.5")
            setOption("audio-spdif", "ac3,dts,eac3,truehd,dts-hd")

            MPVLib.init(mpvHandle)
            startEventLoop()

            Timber.i("MPV initialized, handle=$mpvHandle")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MPV")
            _playbackState.value = PlaybackState.Error(
                PlaybackError.DecoderError(e.message ?: "MPV init failed")
            )
        }
    }

    private fun startEventLoop() {
        eventLoopJob = scope.launch(Dispatchers.IO) {
            while (isActive && mpvHandle != 0L) {
                try {
                    val event = MPVLib.waitEvent(mpvHandle, 0.1) ?: continue
                    handleMpvEvent(event)
                } catch (e: Exception) {
                    Timber.e(e, "Error in MPV event loop")
                }
            }
        }
    }

    private fun setOption(name: String, value: String) {
        if (mpvHandle != 0L) {
            MPVLib.setOptionString(mpvHandle, name, value)
        }
    }

    private fun setProperty(name: String, value: String) {
        if (mpvHandle != 0L) {
            MPVLib.setPropertyString(mpvHandle, name, value)
        }
    }

    private fun getPropertyString(name: String): String? {
        return if (mpvHandle != 0L) {
            try {
                MPVLib.getPropertyString(mpvHandle, name)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private fun getPropertyDouble(name: String): Double {
        return try {
            getPropertyString(name)?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getPropertyInt(name: String): Int {
        return try {
            getPropertyString(name)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun command(vararg args: String) {
        if (mpvHandle != 0L) {
            MPVLib.command(mpvHandle, args)
        }
    }

    override fun play() {
        command("set", "pause", "no")
        _playbackState.value = PlaybackState.Playing
        startPositionUpdates()
    }

    override fun pause() {
        command("set", "pause", "yes")
        _playbackState.value = PlaybackState.Paused
        stopPositionUpdates()
    }

    override fun stop() {
        command("stop")
        _playbackState.value = PlaybackState.Idle
        stopPositionUpdates()
        _positionMs.value = 0
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        command("seek", seconds.toString(), "absolute")
    }

    override fun seekRelative(deltaMs: Long) {
        val seconds = deltaMs / 1000.0
        command("seek", seconds.toString(), "relative")
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        setProperty("speed", clamped.toString())
        _speed.value = clamped
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        setProperty("volume", (clamped * 100).toString())
        _volume.value = clamped
    }

    override fun setMute(muted: Boolean) {
        setProperty("mute", if (muted) "yes" else "no")
        _isMuted.value = muted
    }

    override fun loadMedia(uri: String, headers: Map<String, String>) {
        loadMedia(MediaItem(uri, headers = headers))
    }

    override fun loadMedia(mediaItem: MediaItem) {
        _playbackState.value = PlaybackState.Loading

        if (mediaItem.headers.isNotEmpty()) {
            val headerString = mediaItem.headers.entries.joinToString(",") {
                "${it.key}: ${it.value}"
            }
            setProperty("http-header-fields", headerString)
        }

        mediaItem.subtitles.forEach { sub ->
            command("sub-add", sub.uri, "auto", sub.language)
        }

        command("loadfile", mediaItem.uri)

        if (mediaItem.startPositionMs > 0) {
            seekTo(mediaItem.startPositionMs)
        }

        startPositionUpdates()
        startDiagnosticsUpdates()
    }

    override fun release() {
        stopPositionUpdates()
        stopDiagnosticsUpdates()
        eventLoopJob?.cancel()
        scope.cancel()

        if (mpvHandle != 0L) {
            MPVLib.destroy(mpvHandle)
            mpvHandle = 0
        }

        currentSurface?.release()
        currentSurface = null
    }

    override fun attachSurface(surface: Surface) {
        currentSurface = surface
        if (mpvHandle != 0L) {
            MPVLib.setPropertyString(mpvHandle, "android-surface", surface.toString())
            command("vo-resize")
        }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                attachSurface(holder.surface)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
                setProperty("android-surface-width", width.toString())
                setProperty("android-surface-height", height.toString())
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                detachSurface()
            }
        })
    }

    override fun attachTextureView(textureView: TextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
                attachSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                detachSurface()
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    override fun detachSurface() {
        currentSurface = null
        if (mpvHandle != 0L) {
            setProperty("vo", "null")
        }
    }

    override fun setVideoScalingMode(mode: VideoScalingMode) {
        val mpvMode = when (mode) {
            VideoScalingMode.FIT -> "letterbox"
            VideoScalingMode.CROP -> "crop"
            VideoScalingMode.STRETCH -> "stretch"
            VideoScalingMode.ORIGINAL -> "original"
        }
        setProperty("video-scale", mpvMode)
    }

    override fun selectAudioTrack(trackId: String) {
        val mpvId = trackIdToMpvId[trackId] ?: return
        setProperty("aid", mpvId.toString())
        updateTrackSelection(_audioTracks, trackId)
    }

    override fun selectSubtitleTrack(trackId: String?) {
        if (trackId == null) {
            setProperty("sid", "no")
        } else {
            val mpvId = trackIdToMpvId[trackId] ?: return
            setProperty("sid", mpvId.toString())
        }
        updateTrackSelection(_subtitleTracks, trackId)
    }

    override fun selectVideoTrack(trackId: String) {
        val mpvId = trackIdToMpvId[trackId] ?: return
        setProperty("vid", mpvId.toString())
        updateTrackSelection(_videoTracks, trackId)
    }

    override fun setAudioDelay(delayMs: Long) {
        setProperty("audio-delay", (delayMs / 1000.0).toString())
    }

    override fun setSubtitleDelay(delayMs: Long) {
        setProperty("sub-delay", (delayMs / 1000.0).toString())
    }

    override fun setLooping(looping: Boolean) {
        setProperty("loop-file", if (looping) "inf" else "no")
    }

    override fun setAudioFocus(enabled: Boolean) {}
    override fun setWakeLock(enabled: Boolean) {}

    override fun enableDiagnosticsOverlay(enabled: Boolean) {
        diagnosticsOverlayEnabled = enabled
        if (enabled) startDiagnosticsUpdates() else stopDiagnosticsUpdates()
    }

    override val backendType: PlayerBackend = PlayerBackend.MPV
    override val backendVersion: String
        get() = getPropertyString("mpv-version") ?: "unknown"

    private fun handleMpvEvent(event: MPVEvent) {
        mainHandler.post {
            when (event.eventId) {
                MPV_EVENT_START_FILE -> _playbackState.value = PlaybackState.Loading
                MPV_EVENT_FILE_LOADED -> {
                    _playbackState.value = PlaybackState.Ready
                    scope.launch { extractTracks() }
                }
                MPV_EVENT_PAUSE -> _playbackState.value = PlaybackState.Paused
                MPV_EVENT_UNPAUSE -> _playbackState.value = PlaybackState.Playing
                MPV_EVENT_END_FILE -> {
                    _playbackState.value = PlaybackState.Ended
                    stopPositionUpdates()
                }
                MPV_EVENT_SEEK -> _playbackState.value = PlaybackState.Buffering
                MPV_EVENT_PLAYBACK_RESTART -> {
                    if (_playbackState.value != PlaybackState.Paused) {
                        _playbackState.value = PlaybackState.Playing
                    }
                }
                MPV_EVENT_SHUTDOWN -> _playbackState.value = PlaybackState.Idle
                MPV_EVENT_PROPERTY_CHANGE -> handlePropertyChange(event)
            }
        }
    }

    private fun handlePropertyChange(event: MPVEvent) {
        when (event.name) {
            "time-pos" -> {
                val seconds = event.data?.toDoubleOrNull() ?: 0.0
                _positionMs.value = (seconds * 1000).toLong()
            }
            "duration" -> {
                val seconds = event.data?.toDoubleOrNull() ?: 0.0
                _durationMs.value = (seconds * 1000).toLong()
            }
            "demuxer-cache-duration" -> {
                val seconds = event.data?.toDoubleOrNull() ?: 0.0
                _bufferedPositionMs.value = _positionMs.value + (seconds * 1000).toLong()
            }
            "speed" -> _speed.value = (event.data?.toDoubleOrNull() ?: 1.0).toFloat()
            "volume" -> _volume.value = (((event.data?.toDoubleOrNull() ?: 100.0) / 100.0)).toFloat()
            "mute" -> _isMuted.value = event.data == "yes" || event.data == "true"
            "track-list" -> event.data?.let { parseTrackList(it) }
            "video-params" -> event.data?.let { parseVideoParams(it) }
            "decoder-frame-drop-count" -> updateFrameDrops()
            "vo-drop-frame-count" -> updateFrameDrops()
            "display-fps" -> {
                val fps = event.data?.toDoubleOrNull() ?: 0.0
                _diagnostics.value = _diagnostics.value.copy(displayRefreshRate = fps.toFloat())
            }
            "estimated-vf-fps" -> {
                val fps = event.data?.toDoubleOrNull() ?: 0.0
                _diagnostics.value = _diagnostics.value.copy(currentFps = fps.toFloat())
            }
        }
    }

    private fun parseTrackList(json: String) {
        try {
            val audioList = mutableListOf<TrackInfo>()
            val subtitleList = mutableListOf<TrackInfo>()
            val videoList = mutableListOf<TrackInfo>()

            val trackPattern = """"id":(\d+).*?"type":"(\w+)".*?"lang":"([^"]*)".*?"codec":"([^"]*)"""".toRegex()
            trackPattern.findAll(json).forEach { match ->
                val id = match.groupValues[1].toInt()
                val type = match.groupValues[2]
                val lang = match.groupValues[3]
                val codec = match.groupValues[4]
                val trackId = generateTrackId(type, id)
                trackIdToMpvId[trackId] = id

                val trackInfo = TrackInfo(
                    id = trackId,
                    type = when (type) {
                        "audio" -> TrackType.AUDIO
                        "video" -> TrackType.VIDEO
                        "sub" -> TrackType.SUBTITLE
                        else -> TrackType.UNKNOWN
                    },
                    language = lang.ifEmpty { null },
                    codec = codec.ifEmpty { null },
                    isSelected = false
                )

                when (trackInfo.type) {
                    TrackType.AUDIO -> audioList.add(trackInfo)
                    TrackType.VIDEO -> videoList.add(trackInfo)
                    TrackType.SUBTITLE -> subtitleList.add(trackInfo)
                    else -> {}
                }
            }

            _audioTracks.value = audioList
            _subtitleTracks.value = subtitleList
            _videoTracks.value = videoList
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse track list")
        }
    }

    private fun parseVideoParams(json: String) {
        try {
            val wPattern = """"w":(\d+)""".toRegex()
            val hPattern = """"h":(\d+)""".toRegex()
            val fpsPattern = """"fps":([\d.]+)""".toRegex()

            val width = wPattern.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val height = hPattern.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val fps = fpsPattern.find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            _diagnostics.value = _diagnostics.value.copy(
                videoResolution = "${width}x${height}",
                contentFrameRate = fps
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse video params")
        }
    }

    private fun updateFrameDrops() {
        val decoderDrops = getPropertyInt("decoder-frame-drop-count")
        val voDrops = getPropertyInt("vo-drop-frame-count")
        _diagnostics.value = _diagnostics.value.copy(
            droppedFrames = decoderDrops + voDrops
        )
    }

    private fun extractTracks() {
        val trackListJson = getPropertyString("track-list") ?: return
        parseTrackList(trackListJson)
    }

    private fun generateTrackId(type: String, mpvId: Int): String {
        return "mpv_${type}_${mpvId}_${nextTrackId++}"
    }

    private fun updateTrackSelection(flow: MutableStateFlow<List<TrackInfo>>, selectedId: String?) {
        flow.value = flow.value.map { it.copy(isSelected = it.id == selectedId) }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_playbackState.value == PlaybackState.Playing) {
                    val pos = getPropertyDouble("time-pos")
                    _positionMs.value = (pos * 1000).toLong()
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
                    val cacheDuration = getPropertyDouble("demuxer-cache-duration")
                    val videoBitrate = getPropertyInt("video-bitrate")
                    val audioBitrate = getPropertyInt("audio-bitrate")
                    val hwdec = getPropertyString("hwdec-current") ?: "no"
                    val decoder = getPropertyString("current-vo") ?: "unknown"
                    val videoCodec = getPropertyString("video-codec") ?: "unknown"
                    val audioCodec = getPropertyString("audio-codec") ?: "unknown"
                    val width = getPropertyInt("width")
                    val height = getPropertyInt("height")

                    _diagnostics.value = _diagnostics.value.copy(
                        bufferDurationMs = (cacheDuration * 1000).toLong(),
                        bufferedPercentage = if (_durationMs.value > 0) {
                            ((_bufferedPositionMs.value * 100) / _durationMs.value).toInt()
                        } else 0,
                        currentBitrate = (videoBitrate + audioBitrate).toLong(),
                        decoderName = decoder,
                        videoCodec = videoCodec,
                        audioCodec = audioCodec,
                        videoResolution = "${width}x${height}",
                        isHardwareDecoding = hwdec != "no" && hwdec != "null",
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

    companion object {
        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_GET_PROPERTY_REPLY = 3
        const val MPV_EVENT_SET_PROPERTY_REPLY = 4
        const val MPV_EVENT_COMMAND_REPLY = 5
        const val MPV_EVENT_START_FILE = 6
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_TRACKS_CHANGED = 9
        const val MPV_EVENT_TRACK_SWITCHED = 10
        const val MPV_EVENT_IDLE = 11
        const val MPV_EVENT_PAUSE = 12
        const val MPV_EVENT_UNPAUSE = 13
        const val MPV_EVENT_TICK = 14
        const val MPV_EVENT_SCRIPT_INPUT_DISPATCH = 15
        const val MPV_EVENT_CLIENT_MESSAGE = 16
        const val MPV_EVENT_VIDEO_RECONFIG = 17
        const val MPV_EVENT_AUDIO_RECONFIG = 18
        const val MPV_EVENT_SEEK = 19
        const val MPV_EVENT_PLAYBACK_RESTART = 20
        const val MPV_EVENT_PROPERTY_CHANGE = 21
    }

    data class MPVEvent(
        val eventId: Int,
        val name: String = "",
        val data: String? = null,
        val error: Int = 0
    )
}

object MPVLib {
    init {
        System.loadLibrary("mpv")
    }

    @JvmStatic external fun init()
    @JvmStatic external fun create(): Long
    @JvmStatic external fun init(handle: Long)
    @JvmStatic external fun destroy(handle: Long)
    @JvmStatic external fun setOptionString(handle: Long, name: String, value: String)
    @JvmStatic external fun setPropertyString(handle: Long, name: String, value: String)
    @JvmStatic external fun getPropertyString(handle: Long, name: String): String?
    @JvmStatic external fun command(handle: Long, args: Array<out String>)
    @JvmStatic external fun waitEvent(handle: Long, timeout: Double): MpvPlayer.MPVEvent?
}
