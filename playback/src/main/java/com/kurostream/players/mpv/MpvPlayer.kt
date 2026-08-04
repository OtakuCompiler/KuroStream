package com.kurostream.players.mpv

import android.content.Context
import android.view.Surface
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.common.memory.RamTier
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.kurostream.players.selector.PlaybackEngine

@Singleton
class MpvPlayer @Inject constructor(
    private val context: Context,
) : PlaybackEngine {

    private var isInitialized = false
    private var currentUri: String? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())

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
            MPVLib.create(context)
            applyMpvOptions()
            MPVLib.init()
            MPVLib.addObserver(mpvObserver)
            // Observe core properties — events fire on changes, reducing poll work.
            MPVLib.observeProperty("time-pos",       MPVLib.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("duration",        MPVLib.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("pause",           MPVLib.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("eof-reached",     MPVLib.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("demuxer-cache-duration", MPVLib.MPV_FORMAT_DOUBLE)
            isInitialized = true
            Timber.d("MpvPlayer: initialized (tier=${LowRamDevice.ramTier})")
        } catch (e: Exception) {
            Timber.e(e, "MPV native initialization failed")
            isInitialized = false
        }
        // Fallback poll in case events are throttled by the mpv event loop.
        startPositionPolling()
    }

    /**
     * Apply mpv options before init().
     *
     * hwdec=auto-safe — prefers hardware decode without risking crashes on
     * buggy drivers. Falls back to software automatically.
     *
     * gpu-api=opengl — Android doesn't support Vulkan in mpv-android yet;
     * force OpenGL so we skip the Vulkan probe and its associated stall.
     *
     * video-sync=audio — keeps A/V sync driven by the audio clock (Dolby Atmos
     * PCM passthrough path). Alternative: `display-resample` for 24p on 60Hz
     * panels — left as a user preference.
     *
     * demuxer-max-bytes — keep under RAM budget. Position polling updates
     * _bufferedPosition from demuxer-cache-duration * bitrate, but mpv doesn't
     * expose a byte-precise value so we cap via this option instead.
     */
    private fun applyMpvOptions() {
        // Hardware decode — safe fallback prevents driver crashes.
        MPVLib.setOptionString("hwdec", "auto-safe")
        // GPU rendering
        MPVLib.setOptionString("gpu-api", "opengl")
        MPVLib.setOptionString("opengl-es", "yes")
        // A/V sync — audio-driven clock for Dolby/AC3 passthrough
        MPVLib.setOptionString("video-sync", "audio")
        MPVLib.setOptionString("interpolation", "no")  // save GPU on TV
        // Demuxer cache — tier-tuned to stay under 125 MB total
        val demuxerMaxMb = when (LowRamDevice.ramTier) {
            RamTier.LOW  -> "16MiB"
            RamTier.MID  -> "24MiB"
            RamTier.HIGH -> "48MiB"
        }
        MPVLib.setOptionString("demuxer-max-bytes", demuxerMaxMb)
        MPVLib.setOptionString("demuxer-max-back-bytes", "4MiB")
        // Subtitle renderer
        MPVLib.setOptionString("sub-font-size", "42")
        MPVLib.setOptionString("sub-use-margins", "no")
        // Network
        MPVLib.setOptionString("network-timeout", "10")
        MPVLib.setOptionString("tls-verify", "no")    // handled by app-level pinning
        // Logging — suppress in release
        MPVLib.setOptionString("msg-level", "all=error")
    }

    private val mpvObserver = object : MPVLib.EventObserver {
        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.MPV_EVENT_END_FILE -> {
                    _playbackState.value = PlaybackEngine.PlaybackState.ENDED
                    listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.ENDED) }
                }
                MPVLib.MPV_EVENT_START_FILE -> {
                    _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
                    listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
                }
                MPVLib.MPV_EVENT_PLAYBACK_RESTART -> {
                    _playbackState.value = PlaybackEngine.PlaybackState.READY
                    listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.READY) }
                }
                else -> Unit
            }
        }

        override fun eventProperty(property: String, value: Long)    = handleLong(property, value)
        override fun eventProperty(property: String, value: Double)  = handleDouble(property, value)
        override fun eventProperty(property: String, value: Boolean) = handleBoolean(property, value)
        override fun eventProperty(property: String, value: String)  { /* no-op */ }
        override fun eventProperty(property: String)                 { /* no-op */ }
    }

    private fun handleLong(property: String, value: Long) {
        when (property) {
            "time-pos"  -> _currentPosition.value = (value * 1000).coerceAtLeast(0L)
            "duration"  -> _duration.value        = (value * 1000).coerceAtLeast(0L)
        }
    }

    private fun handleDouble(property: String, value: Double) {
        when (property) {
            "time-pos"  -> _currentPosition.value  = (value * 1000).toLong().coerceAtLeast(0L)
            "duration"  -> _duration.value         = (value * 1000).toLong().coerceAtLeast(0L)
            "demuxer-cache-duration" -> {
                // Approximate buffered position from cache duration
                val cur = _currentPosition.value
                _bufferedPosition.value = (cur + value * 1000).toLong().coerceAtLeast(cur)
            }
        }
    }

    private fun handleBoolean(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                _isPlaying.value = !value
                listeners.forEach { it.onIsPlayingChanged(!value) }
            }
            "eof-reached" -> if (value) {
                _playbackState.value = PlaybackEngine.PlaybackState.ENDED
                listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.ENDED) }
            }
        }
    }

    // Safety-net poll — events alone can miss updates under high system load.
    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                if (isInitialized) {
                    runCatching {
                        MPVLib.getPropertyDouble("time-pos")?.let {
                            _currentPosition.value = (it * 1000).toLong().coerceAtLeast(0L)
                        }
                        MPVLib.getPropertyDouble("duration")?.let {
                            _duration.value = (it * 1000).toLong().coerceAtLeast(0L)
                        }
                        MPVLib.getPropertyBoolean("pause")?.let {
                            _isPlaying.value = !it
                        }
                    }
                }
                delay(500)
            }
        }
    }

    override fun addListener(listener: PlaybackEngine.Listener)    { listeners.add(listener) }
    override fun removeListener(listener: PlaybackEngine.Listener) { listeners.remove(listener) }

    override fun setMedia(uri: String, title: String?, startPositionMs: Long) {
        currentUri = uri
        if (!isInitialized) return
        val args = if (startPositionMs > 0) {
            arrayOf("loadfile", uri, "replace", "start=${startPositionMs / 1000.0}")
        } else {
            arrayOf("loadfile", uri)
        }
        MPVLib.command(args)
        _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
    }

    override fun play() {
        if (!isInitialized) return
        MPVLib.setPropertyBoolean("pause", false)
        _playbackState.value = PlaybackEngine.PlaybackState.READY
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.READY) }
    }

    override fun pause() {
        if (!isInitialized) return
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(positionMs: Long) {
        if (!isInitialized) return
        // "absolute" mode is more reliable than "relative" for skip button seeks.
        MPVLib.command(arrayOf("seek", "${positionMs / 1000.0}", "absolute", "exact"))
    }

    override fun seekForward() { seekTo(_currentPosition.value + 10_000L) }
    override fun seekBack()    { seekTo((_currentPosition.value - 10_000L).coerceAtLeast(0L)) }

    override fun setPlaybackSpeed(speed: Float) {
        if (!isInitialized) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    fun setSurface(surface: Surface?) {
        if (!isInitialized) return
        if (surface != null) MPVLib.attachSurface(surface) else MPVLib.detachSurface()
    }

    override fun stop() {
        if (isInitialized) runCatching { MPVLib.command(arrayOf("stop")) }
        _isPlaying.value     = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.IDLE) }
    }

    override fun setSubtitleEnabled(enabled: Boolean) {
        if (!isInitialized) return
        MPVLib.setPropertyString("sub-visibility", if (enabled) "yes" else "no")
    }

    override fun release() {
        scope.cancel()
        if (isInitialized) {
            runCatching { MPVLib.destroy() }
            isInitialized = false
        }
        currentUri              = null
        _currentPosition.value  = 0L
        _duration.value         = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value        = false
        _playbackState.value    = PlaybackEngine.PlaybackState.IDLE
        listeners.clear()
    }

    internal fun isLibLoaded(): Boolean = isInitialized
}
