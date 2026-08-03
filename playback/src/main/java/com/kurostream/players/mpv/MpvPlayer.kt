package com.kurostream.players.mpv

import android.content.Context
import android.view.Surface
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

    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _bufferedPosition = MutableStateFlow(0L)
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(PlaybackEngine.PlaybackState.IDLE)
    private val _errorMessage = MutableSharedFlow<String?>()
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    override val duration: StateFlow<Long> = _duration.asStateFlow()
    override val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val playbackState: StateFlow<PlaybackEngine.PlaybackState> = _playbackState.asStateFlow()
    override val errorMessage: SharedFlow<String?> = _errorMessage.asSharedFlow()

    private val listeners = mutableSetOf<PlaybackEngine.Listener>()

    override suspend fun initialize() {
        // Native lib load is guarded here so a missing/failed native lib does not
        // crash the app during DI construction; BackendSelector will fall back
        // to VLC or Media3 if this returns with isInitialized still false.
        try {
            MPVLib.create(context)
            MPVLib.init()
            MPVLib.addObserver(object : MPVLib.EventObserver {
                override fun event(eventId: Int) {
                    when (eventId) {
                        MPVLib.MPV_EVENT_END_FILE -> {
                            _playbackState.value = PlaybackEngine.PlaybackState.ENDED
                            listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.ENDED) }
                        }
                        else -> Unit
                    }
                }

                override fun eventProperty(property: String, value: Long) {
                    handleEventPropertyLong(property, value)
                }

                override fun eventProperty(property: String, value: Double) {
                    handleEventPropertyDouble(property, value)
                }

                override fun eventProperty(property: String, value: Boolean) {
                    handleEventPropertyBoolean(property, value)
                }

                override fun eventProperty(property: String, value: String) {
                    handleEventPropertyString(property, value)
                }

                override fun eventProperty(property: String) {
                    handleEventPropertyUnset(property)
                }
            })
            // Observe core properties
            MPVLib.observeProperty("time-pos", MPVLib.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("duration", MPVLib.MPV_FORMAT_DOUBLE)
            MPVLib.observeProperty("pause", MPVLib.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("eof-reached", MPVLib.MPV_FORMAT_FLAG)
            isInitialized = true
        } catch (e: Exception) {
            Timber.e(e, "MPV native initialization failed")
            isInitialized = false
        }
        startPositionPolling()
    }

    private fun handleEventPropertyLong(property: String, value: Long) {
        when (property) {
            "time-pos" -> _currentPosition.value = (value * 1000).coerceAtLeast(0L)
            "duration" -> _duration.value = (value * 1000).coerceAtLeast(0L)
        }
    }

    private fun handleEventPropertyDouble(property: String, value: Double) {
        when (property) {
            "time-pos" -> _currentPosition.value = (value * 1000).toLong().coerceAtLeast(0L)
            "duration" -> _duration.value = (value * 1000).toLong().coerceAtLeast(0L)
        }
    }

    private fun handleEventPropertyBoolean(property: String, value: Boolean) {
        when (property) {
            "pause" -> _isPlaying.value = !value
            "eof-reached" -> {
                if (value) {
                    _playbackState.value = PlaybackEngine.PlaybackState.ENDED
                    listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.ENDED) }
                }
            }
        }
    }

    private fun handleEventPropertyString(property: String, value: String) {
        // no-op
    }

    private fun handleEventPropertyUnset(property: String) {
        // no-op
    }

    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                try {
                    if (!isInitialized) {
                        delay(500)
                        continue
                    }
                    MPVLib.getPropertyDouble("time-pos")?.let { pos ->
                        _currentPosition.value = (pos * 1000).toLong().coerceAtLeast(0L)
                    }
                    MPVLib.getPropertyDouble("duration")?.let { dur ->
                        _duration.value = (dur * 1000).toLong().coerceAtLeast(0L)
                    }
                    MPVLib.getPropertyBoolean("pause")?.let { paused ->
                        _isPlaying.value = !paused
                    }
                } catch (e: Exception) {
                    Timber.e(e, "MPV property poll failed")
                }
                delay(500)
            }
        }
    }

    override fun addListener(listener: PlaybackEngine.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlaybackEngine.Listener) {
        listeners.remove(listener)
    }

    override fun setMedia(uri: String, title: String?, startPositionMs: Long) {
        currentUri = uri
        if (!isInitialized) return
        MPVLib.command(arrayOf("loadfile", uri))
        if (startPositionMs > 0) {
            // Delay seek slightly to allow file load
            scope.launch {
                delay(200)
                seekTo(startPositionMs)
            }
        }
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
        val seconds = positionMs / 1000.0
        MPVLib.setPropertyDouble("time-pos", seconds)
    }

    override fun seekForward() {
        val current = _currentPosition.value
        seekTo(current + 10_000L)
    }

    override fun seekBack() {
        val current = _currentPosition.value
        seekTo((current - 10_000L).coerceAtLeast(0L))
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (!isInitialized) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    fun setSurface(surface: Surface?) {
        if (!isInitialized) return
        if (surface != null) {
            MPVLib.attachSurface(surface)
        } else {
            MPVLib.detachSurface()
        }
    }

    override fun release() {
        scope.cancel()
        if (isInitialized) {
            runCatching { MPVLib.destroy() }
            isInitialized = false
        }
        currentUri = null
        _currentPosition.value = 0L
        _duration.value = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.clear()
    }

    internal fun isLibLoaded(): Boolean = isInitialized
}
