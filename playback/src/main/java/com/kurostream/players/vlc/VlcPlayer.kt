package com.kurostream.players.vlc

import android.content.Context
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.common.memory.RamTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.kurostream.players.selector.PlaybackEngine

@Singleton
class VlcPlayer @Inject constructor(
    private val context: Context,
) : PlaybackEngine {

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: String? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentPosition   = MutableStateFlow(0L)
    private val _duration          = MutableStateFlow(0L)
    private val _bufferedPosition  = MutableStateFlow(0L)
    private val _isPlaying         = MutableStateFlow(false)
    private val _playbackState     = MutableStateFlow(PlaybackEngine.PlaybackState.IDLE)
    private val _errorMessage      = MutableSharedFlow<String?>()

    override val currentPosition:  StateFlow<Long>                        = _currentPosition.asStateFlow()
    override val duration:         StateFlow<Long>                        = _duration.asStateFlow()
    override val bufferedPosition: StateFlow<Long>                        = _bufferedPosition.asStateFlow()
    override val isPlaying:        StateFlow<Boolean>                     = _isPlaying.asStateFlow()
    override val playbackState:    StateFlow<PlaybackEngine.PlaybackState> = _playbackState.asStateFlow()
    override val errorMessage:     SharedFlow<String?>                    = _errorMessage.asSharedFlow()

    private val listeners = mutableSetOf<PlaybackEngine.Listener>()

    override suspend fun initialize() {
        try {
            libVlc = LibVLC(context, buildVlcOptions())
            mediaPlayer = MediaPlayer(libVlc).also { mp ->
                mp.setEventListener(eventListener)
            }
            startPositionPolling()
            Timber.d("VlcPlayer: initialized (tier=${LowRamDevice.ramTier})")
        } catch (e: Exception) {
            Timber.e(e, "VLC native initialization failed")
            libVlc    = null
            mediaPlayer = null
        }
    }

    /**
     * Build the libVLC init args.
     *
     * Key decisions:
     * - `--avcodec-hw=any`  — prefer hardware decode for every codec.
     * - `--network-caching` — tier-tuned, keeps buffered RAM under budget.
     * - `--file-caching`    — fast-start for local/NFS files.
     * - `--audio-resampler=soxr` — high-quality resampler for Atmos downmix.
     * - `--sout-display-delay=0` — remove artificial A/V sync padding.
     * - No `--vout=android_display` forced — let VLC pick the best output for
     *   the surface type (SurfaceView for tunneling, TextureView otherwise).
     */
    private fun buildVlcOptions(): ArrayList<String> = arrayListOf(
        "--avcodec-hw=any",
        "--avcodec-fast",
        "--no-drop-late-frames",
        "--no-skip-frames",
        "--network-caching=${LowRamDevice.vlcNetworkCacheMs}",
        "--file-caching=500",
        "--live-caching=1000",
        "--rtsp-caching=500",
        "--audio-resampler=soxr",
        "--sout-display-delay=0",
        "--clock-jitter=0",
        "--clock-synchro=0",
        // Subtitle rendering
        "--freetype-rel-fontsize=16",
        // Suppress verbose logs in release
        "--quiet",
        "--no-stats",
        // Reduce decoder thread count on low-RAM to avoid RSS spikes
        "--avcodec-threads=${if (LowRamDevice.isLowRamDevice) 2 else 0}",
    )

    private val eventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.EncounteredError -> {
                Timber.e("VLC error encountered")
                _errorMessage.tryEmit("VLC encountered an error")
                _playbackState.value = PlaybackEngine.PlaybackState.ERROR
                listeners.forEach { it.onError("VLC encountered an error") }
            }
            MediaPlayer.Event.EndReached -> {
                _playbackState.value = PlaybackEngine.PlaybackState.ENDED
                listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.ENDED) }
            }
            MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {
                _isPlaying.value = false
                listeners.forEach { it.onIsPlayingChanged(false) }
            }
            MediaPlayer.Event.Playing -> {
                _playbackState.value = PlaybackEngine.PlaybackState.READY
                _isPlaying.value     = true
                listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.READY) }
                listeners.forEach { it.onIsPlayingChanged(true) }
            }
            MediaPlayer.Event.Buffering -> {
                // event.buffering is 0-100; only flip state on transitions
                val bufPct = event.buffering
                if (bufPct < 100f) {
                    _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
                    listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
                } else {
                    if (_playbackState.value == PlaybackEngine.PlaybackState.BUFFERING) {
                        _playbackState.value = PlaybackEngine.PlaybackState.READY
                        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.READY) }
                    }
                }
                // Update buffered position from buffer percentage
                val dur = _duration.value
                if (dur > 0) _bufferedPosition.value = (dur * bufPct / 100f).toLong()
            }
            MediaPlayer.Event.TimeChanged -> {
                _currentPosition.value = event.timeChanged.coerceAtLeast(0L)
            }
            MediaPlayer.Event.LengthChanged -> {
                _duration.value = event.lengthChanged.coerceAtLeast(0L)
            }
            else -> Unit
        }
    }

    // Poll at 250 ms — smooth enough for the progress bar while staying cheap.
    // TimeChanged / LengthChanged events fire continuously during playback so
    // this is mostly a safety net for edge cases where events are missed.
    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { mp ->
                        val t = mp.time
                        val l = mp.length
                        if (t >= 0) _currentPosition.value = t
                        if (l > 0)  _duration.value        = l
                    }
                } catch (e: Exception) {
                    Timber.w(e, "VLC position poll failed")
                }
                delay(250)
            }
        }
    }

    override fun addListener(listener: PlaybackEngine.Listener)    { listeners.add(listener) }
    override fun removeListener(listener: PlaybackEngine.Listener) { listeners.remove(listener) }

    override fun setMedia(uri: String, title: String?, startPositionMs: Long) {
        currentUri = uri
        val vlc    = libVlc ?: return
        val media  = Media(vlc, android.net.Uri.parse(uri)).also { m ->
            // HDR / 10-bit pass-through hint
            m.addOption(":avcodec-hw=any")
            m.addOption(":no-video-dfilter")
        }
        mediaPlayer?.media = media
        media.release()
        if (startPositionMs > 0) {
            mediaPlayer?.time = startPositionMs
        }
        mediaPlayer?.play()
        _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
    }

    override fun play()  { mediaPlayer?.play() }
    override fun pause() { mediaPlayer?.pause() }

    override fun seekTo(positionMs: Long) { mediaPlayer?.time = positionMs }

    override fun seekForward() { seekTo((_currentPosition.value + 10_000L)) }
    override fun seekBack()    { seekTo((_currentPosition.value - 10_000L).coerceAtLeast(0L)) }

    override fun setPlaybackSpeed(speed: Float) { mediaPlayer?.rate = speed }

    override fun stop() {
        mediaPlayer?.stop()
        _isPlaying.value    = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.IDLE) }
    }

    override fun setSubtitleEnabled(enabled: Boolean) {
        // VLC: set subtitle track to -1 (none) to disable; to re-enable caller must set a track index
        if (!enabled) mediaPlayer?.spuTrack = -1
    }

    override fun release() {
        scope.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        libVlc?.release()
        libVlc    = null
        _currentPosition.value  = 0L
        _duration.value         = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value        = false
        _playbackState.value    = PlaybackEngine.PlaybackState.IDLE
        listeners.clear()
    }
}
