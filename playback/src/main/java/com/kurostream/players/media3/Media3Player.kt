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

                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                        ) {
                            _currentPosition.value = newPosition.positionMs
                        }

                        override fun onEvents(player: Player, events: Player.Events) {
                            _currentPosition.value = player.currentPosition
                            _duration.value = player.duration.coerceAtLeast(0L)
                            _bufferedPosition.value = player.bufferedPosition
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
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setMediaCodecSelector(MediaCodecSelector.DEFAULT)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage("jpn")
                    .setPreferredTextLanguage("eng")
                    .setTunnelingEnabled(true)
            )
        }

        val isLowRam = LowRamDevice.isLowRamDevice
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                if (isLowRam) 30_000 else 50_000,
                2_500,
                5_000
            )
            .setTargetBufferBytes(if (isLowRam) 16 * 1024 * 1024 else 32 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            .build()
            .also { exo ->
                exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                Timber.d("Media3Player: created (lowRam=$isLowRam)")
            }
    }

    override fun addListener(listener: PlaybackEngine.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlaybackEngine.Listener) {
        listeners.remove(listener)
    }

    override fun setMedia(uri: String, title: String?, startPositionMs: Long) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri)
            .build()
        player?.setMediaItem(mediaItem, startPositionMs)
        player?.prepare()
        _playbackState.value = PlaybackEngine.PlaybackState.BUFFERING
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.BUFFERING) }
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
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

    override fun nativePlayer(): Any? = player

    override fun release() {
        player?.removeListener(object : Player.Listener {})
        player?.release()
        player = null
        listeners.clear()
        _currentPosition.value = 0L
        _duration.value = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value = false
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
    }
}
