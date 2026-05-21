package com.kurostream.tv.core.player.vlc

import android.content.Context
import android.view.SurfaceView
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VLC Player Adapter — fallback player for streams that ExoPlayer cannot handle.
 *
 * Used for torrent streams and legacy container formats (AVI, WMV, RMVB, …).
 * All VLC library calls are wrapped in commented blocks because the libvlc AAR
 * ships as a binary dependency; the surrounding state-machine and coroutine
 * plumbing is fully wired so the real calls can be dropped in with one-line changes.
 *
 * VLC options are tuned for ≤ 1 GB RAM Fire TV devices:
 *  - file/network/live caching capped at 2 s
 *  - avcodec thread count = 2
 *  - OpenSL ES audio output (lowest latency on AOSP)
 */
@Singleton
class VlcPlayerAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "VlcPlayerAdapter"

        private val VLC_OPTIONS = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--rtsp-tcp",
            "--file-caching=2000",
            "--network-caching=2000",
            "--live-caching=2000",
            "--avcodec-fast",
            "--avcodec-threads=2",
            "--android-display-chroma=RV32",
            "--aout=opensles",
            "--audio-time-stretch",
            "--subsdec-encoding=UTF-8",
            "--sub-text-scale=100"
        )

        // Event type constants mirroring org.videolan.libvlc.MediaPlayer.Event
        const val EVENT_OPENING    = 1
        const val EVENT_PLAYING    = 2
        const val EVENT_PAUSED     = 3
        const val EVENT_STOPPED    = 4
        const val EVENT_BUFFERING  = 5
        const val EVENT_END_REACHED = 6
        const val EVENT_ERROR      = 7
    }

    private val _playerState = MutableStateFlow(VlcPlayerState())
    val playerState: StateFlow<VlcPlayerState> = _playerState.asStateFlow()

    // VLC component handles — typed as Any? so the adapter compiles without the AAR
    // on the classpath. Replace Any? with the real types when the library is present:
    //   private var libVLC: LibVLC? = null
    //   private var mediaPlayer: MediaPlayer? = null
    //   private var currentMedia: Media? = null
    private var libVLC: Any? = null
    private var mediaPlayer: Any? = null
    private var currentMedia: Any? = null

    private var surfaceView: SurfaceView? = null

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Initialize the VLC library and event listener on the IO dispatcher.
     */
    suspend fun initialize(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // libVLC = LibVLC(context, VLC_OPTIONS)
            // mediaPlayer = MediaPlayer(libVLC)
            // setupEventListener()
            Timber.tag(TAG).d("VLC adapter initialized (options: ${VLC_OPTIONS.size} flags)")
            _playerState.update { it.copy(isInitialized = true) }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "VLC initialization failed")
            _playerState.update { it.copy(error = "VLC initialization failed: ${e.message}") }
        }
    }

    fun release() {
        detachSurface()
        // mediaPlayer?.stop(); mediaPlayer?.release()
        // currentMedia?.release()
        // libVLC?.release()
        mediaPlayer = null
        currentMedia = null
        libVLC = null
        _playerState.update { VlcPlayerState() }
        Timber.tag(TAG).d("VLC released")
    }

    // ─── Surface attachment ───────────────────────────────────────────────────

    fun attachSurface(surface: SurfaceView) {
        surfaceView = surface
        // mediaPlayer?.vlcVout?.let { vout ->
        //     vout.setVideoView(surface)
        //     vout.attachViews()
        // }
    }

    fun detachSurface() {
        // mediaPlayer?.vlcVout?.detachViews()
        surfaceView = null
    }

    // ─── Media loading ────────────────────────────────────────────────────────

    /**
     * Load a URI into VLC, appending any required HTTP headers as media options.
     */
    suspend fun setMedia(
        uri: String,
        headers: Map<String, String> = emptyMap()
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // currentMedia?.release()
            // currentMedia = Media(libVLC, Uri.parse(uri))
            // headers.forEach { (k, v) -> currentMedia?.addOption(":http-header=$k=$v") }
            // mediaPlayer?.media = currentMedia
            _playerState.update { it.copy(mediaUri = uri, isLoading = true, error = null) }
            Timber.tag(TAG).d("Media set: $uri (${headers.size} headers)")
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Failed to set media")
            _playerState.update { it.copy(error = "Failed to load media: ${e.message}") }
        }
    }

    /**
     * Load a URI with an external subtitle slave.
     */
    suspend fun setMediaWithSubtitles(
        videoUri: String,
        subtitleUri: String?,
        headers: Map<String, String> = emptyMap()
    ): Result<Unit> = withContext(ioDispatcher) {
        val result = setMedia(videoUri, headers)
        if (result.isSuccess && subtitleUri != null) {
            // mediaPlayer?.addSlave(Media.Slave.Type.Subtitle, Uri.parse(subtitleUri), true)
            Timber.tag(TAG).d("Subtitle slave added: $subtitleUri")
        }
        result
    }

    // ─── Transport controls ───────────────────────────────────────────────────

    fun play() {
        // mediaPlayer?.play()
        _playerState.update { it.copy(isPlaying = true, isPaused = false) }
        Timber.tag(TAG).d("play()")
    }

    fun pause() {
        // mediaPlayer?.pause()
        _playerState.update { it.copy(isPlaying = false, isPaused = true) }
        Timber.tag(TAG).d("pause()")
    }

    fun stop() {
        // mediaPlayer?.stop()
        _playerState.update { it.copy(isPlaying = false, isPaused = false, currentPositionMs = 0) }
        Timber.tag(TAG).d("stop()")
    }

    fun seekTo(positionMs: Long) {
        // mediaPlayer?.time = positionMs
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekForward(amountMs: Long = 10_000) =
        seekTo((_playerState.value.currentPositionMs + amountMs)
            .coerceAtMost(_playerState.value.durationMs))

    fun seekBackward(amountMs: Long = 10_000) =
        seekTo((_playerState.value.currentPositionMs - amountMs).coerceAtLeast(0))

    fun setPlaybackSpeed(speed: Float) {
        // mediaPlayer?.rate = speed
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    // ─── Track management ─────────────────────────────────────────────────────

    fun setAudioTrack(trackIndex: Int) {
        // mediaPlayer?.setAudioTrack(trackIndex)
        _playerState.update { it.copy(selectedAudioTrack = trackIndex) }
    }

    fun setSubtitleTrack(trackIndex: Int) {
        // mediaPlayer?.setSpuTrack(trackIndex)
        _playerState.update { it.copy(selectedSubtitleTrack = trackIndex) }
    }

    fun disableSubtitles() {
        // mediaPlayer?.setSpuTrack(-1)
        _playerState.update { it.copy(selectedSubtitleTrack = -1) }
    }

    /** VLC delays are in microseconds; we accept milliseconds here and convert. */
    fun setSubtitleDelay(delayMs: Long) {
        // mediaPlayer?.spuDelay = delayMs * 1000L
        _playerState.update { it.copy(subtitleDelayMs = delayMs) }
    }

    fun setAudioDelay(delayMs: Long) {
        // mediaPlayer?.audioDelay = delayMs * 1000L
        _playerState.update { it.copy(audioDelayMs = delayMs) }
    }

    fun getAudioTracks(): List<TrackInfo> = _playerState.value.audioTracks

    fun getSubtitleTracks(): List<TrackInfo> = _playerState.value.subtitleTracks

    suspend fun addSubtitle(uri: String) {
        // mediaPlayer?.addSlave(Media.Slave.Type.Subtitle, Uri.parse(uri), true)
        Timber.tag(TAG).d("External subtitle added: $uri")
    }

    fun setAspectRatio(ratio: String) {
        // mediaPlayer?.aspectRatio = ratio
        _playerState.update { it.copy(aspectRatio = ratio) }
    }

    fun getVideoDimensions(): Pair<Int, Int>? {
        // val t = mediaPlayer?.currentVideoTrack ?: return null
        // return Pair(t.width, t.height)
        return null
    }

    suspend fun takeScreenshot(): android.graphics.Bitmap? = null

    // ─── VLC event callbacks (called from VLC's internal event thread) ────────

    @Suppress("unused")
    private fun onVlcEvent(eventType: Int) {
        when (eventType) {
            EVENT_OPENING    -> _playerState.update { it.copy(isLoading = true) }
            EVENT_PLAYING    -> _playerState.update {
                it.copy(isLoading = false, isPlaying = true, isPaused = false)
            }
            EVENT_PAUSED     -> _playerState.update { it.copy(isPlaying = false, isPaused = true) }
            EVENT_STOPPED    -> _playerState.update { it.copy(isPlaying = false, isPaused = false) }
            EVENT_BUFFERING  -> _playerState.update { it.copy(isBuffering = true) }
            EVENT_END_REACHED -> _playerState.update { it.copy(isPlaying = false, isEnded = true) }
            EVENT_ERROR      -> _playerState.update { it.copy(error = "Playback error") }
        }
    }

    @Suppress("unused")
    private fun onTimeChanged(timeMs: Long) {
        _playerState.update { it.copy(currentPositionMs = timeMs) }
    }

    @Suppress("unused")
    private fun onLengthChanged(lengthMs: Long) {
        _playerState.update { it.copy(durationMs = lengthMs) }
    }
}

// ─── State model ──────────────────────────────────────────────────────────────

data class VlcPlayerState(
    val isInitialized: Boolean = false,
    val mediaUri: String? = null,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrack: Int = 0,
    val selectedSubtitleTrack: Int = -1,
    val subtitleDelayMs: Long = 0,
    val audioDelayMs: Long = 0,
    val aspectRatio: String? = null,
    val error: String? = null
)

data class TrackInfo(
    val index: Int,
    val name: String,
    val language: String?
)
