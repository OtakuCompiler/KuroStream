package com.kurostream.players.vlc

import android.content.Context
import android.view.Surface
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
import kotlinx.coroutines.launch
import org.videolan.libvlc.IVLCVout
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
        // to Media3 if this returns with libVlc still null.
        try {
            libVlc = LibVLC(context)
            mediaPlayer = MediaPlayer(libVlc).also { mp ->
                mp.setEventListener(eventListener)
            }
        } catch (e: Exception) {
            Timber.e(e, "VLC native initialization failed")
            libVlc = null
            mediaPlayer = null
        }
        startPositionPolling()
    }

    private val eventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.EncounteredError -> {
                Timber.e("VLC error: %s", event)
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
                _isPlaying.value = true
                listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.READY) }
                listeners.forEach { it.onIsPlayingChanged(true) }
            }
            MediaPlayer.Event.Buffering -> {
                _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
                listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
            }
            else -> Unit
        }
    }

    override suspend fun initialize() {
        // Native init is performed in this method body above, guarded with try/catch.
        // If libVlc is still null after this call, the backend is unavailable and
        // BackendSelector will fall back to Media3.
    }

    private fun startPositionPolling() {
        scope.launch {
            while (isActive) {
                mediaPlayer()?.let { mp ->
                    _currentPosition.value = mp.time.coerceAtLeast(0L)
                    _duration.value = (mp.length).coerceAtLeast(0L)
                    _bufferedPosition.value = _currentPosition.value
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
        val media = Media(libVlc, android.net.Uri.parse(uri))
        mediaPlayer()?.setMedia(media)
        media.release()
        mediaPlayer()?.let { mp ->
            mp.play()
            if (startPositionMs > 0) {
                mp.setTime(startPositionMs)
            }
        }
    }

    override fun play() {
        mediaPlayer()?.play()
    }

    override fun pause() {
        mediaPlayer()?.pause()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer()?.setTime(positionMs)
    }

    override fun seekForward() {
        mediaPlayer()?.let { mp ->
            val next = (mp.time + 10_000L).coerceAtMost(_duration.value)
            mp.setTime(next)
        }
    }

    override fun seekBack() {
        mediaPlayer()?.let { mp ->
            val prev = (mp.time - 10_000L).coerceAtLeast(0L)
            mp.setTime(prev)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaPlayer()?.rate = speed.toDouble()
    }

    fun setSurface(surface: Surface?) {
        val vout: IVLCVout? = mediaPlayer()?.vlcVout
        if (surface != null) {
            vout?.setVideoSurface(surface, null)
            vout?.attachViews()
        } else {
            vout?.detachViews()
        }
    }

    fun mediaPlayer(): MediaPlayer? = mediaPlayer

    override fun release() {
        scope.cancel()
        mediaPlayer()?.stop()
        mediaPlayer()?.release()
        libVlc?.release()
        mediaPlayer = null
        libVlc = null
        _currentPosition.value = 0L
        _duration.value = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.clear()
    }
}
