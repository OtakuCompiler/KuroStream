package com.kurostream.players.media3

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.common.util.UnstableApi
import com.kurostream.common.memory.CodecCapabilityDetector
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.players.selector.PlaybackEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class Media3Player @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackEngine {

    private var player: ExoPlayer? = null
    private val listeners = mutableSetOf<PlaybackEngine.Listener>()

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

    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            if (player == null) {
                player = createExoPlayer().also { exo ->
                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            val mapped = when (state) {
                                Player.STATE_IDLE -> PlaybackEngine.PlaybackState.IDLE
                                Player.STATE_BUFFERING -> PlaybackEngine.PlaybackState.BUFFERING
                                Player.STATE_READY -> PlaybackEngine.PlaybackState.READY
                                Player.STATE_ENDED -> PlaybackEngine.PlaybackState.ENDED
                                else -> PlaybackEngine.PlaybackState.ERROR
                            }
                            _playbackState.value = mapped
                            listeners.forEach { it.onPlaybackStateChanged(mapped) }
                        }

                        override fun onIsPlayingChanged(playing: Boolean) {
                            _isPlaying.value = playing
                            listeners.forEach { it.onIsPlayingChanged(playing) }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Timber.e(error, "Media3 playback error")
                            _errorMessage.tryEmit(error.message)
                            _playbackState.value = PlaybackEngine.PlaybackState.ERROR
                            listeners.forEach { it.onError(error.message) }
                        }
                    })
                }
            }
        }
    }

    private fun createExoPlayer(): ExoPlayer {
        CodecCapabilityDetector.detect()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector(MediaCodecSelector.DEFAULT)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage("jpn")
                    .setPreferredTextLanguage("eng")
            )
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context)

        val isLowRam = LowRamDevice.isLowRamDevice()
        val MIN_BUFFER_MS = 15_000
        val MAX_BUFFER_MS = if (isLowRam) MIN_BUFFER_MS else 50_000
        val BUFFER_FOR_PLAYBACK_MS = 2_500
        val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setTargetBufferBytes(if (isLowRam) 2 * 1024 * 1024 else 5 * 1024 * 1024)
                    .setPriorizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build()
            .also { exo ->
                exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                Timber.d("Media3Player: created player (lowRam=$isLowRam)")
            }
    }

    override fun addListener(listener: PlaybackEngine.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlaybackEngine.Listener) {
        listeners.remove(listener)
    }

    override fun setMedia(uri: String, title: String?, startPositionMs: Long) {
        val exo = player ?: return
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(title ?: uri)
            .build()
        exo.setMediaItem(mediaItem, startPositionMs)
        exo.prepare()
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceIn(0, _duration.value.coerceAtLeast(0L)))
    }

    override fun seekForward() {
        player?.seekForward()
    }

    override fun seekBack() {
        player?.seekBack()
    }

    override fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    override fun release() {
        player?.release()
        player = null
        _currentPosition.value = 0L
        _duration.value = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.clear()
    }

    override fun nativePlayer(): Any? = player
}
