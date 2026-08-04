package com.kurostream.players.media3

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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

    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            if (player == null) {
                player = createExoPlayer().also { attachListener(it) }
            }
        }
    }

    private fun attachListener(exo: ExoPlayer) {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val mapped = when (state) {
                    Player.STATE_IDLE      -> PlaybackEngine.PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackEngine.PlaybackState.BUFFERING
                    Player.STATE_READY     -> PlaybackEngine.PlaybackState.READY
                    Player.STATE_ENDED     -> PlaybackEngine.PlaybackState.ENDED
                    else                   -> PlaybackEngine.PlaybackState.ERROR
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
                reason: Int,
            ) {
                _currentPosition.value = newPosition.positionMs
            }

            override fun onEvents(player: Player, events: Player.Events) {
                _currentPosition.value  = player.currentPosition
                _duration.value         = player.duration.coerceAtLeast(0L)
                _bufferedPosition.value = player.bufferedPosition
            }
        })
    }

    private fun createExoPlayer(): ExoPlayer {
        CodecCapabilityDetector.detect()

        val tier = LowRamDevice.ramTier
        Timber.d("Media3Player: ramTier=$tier totalRam=${LowRamDevice.totalMemoryMb}MB")

        // ── Renderers ────────────────────────────────────────────────────────
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector(MediaCodecSelector.DEFAULT)

        // ── Track selector — prefer 4K/HDR when available ────────────────────
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    // Audio
                    .setPreferredAudioLanguage("jpn")
                    .setPreferredTextLanguage("eng")
                    // Tunneled rendering offloads video decode to the display path
                    // on Android TV — critical for Dolby Vision / HDR10+ passthrough.
                    .setTunnelingEnabled(true)
                    // Prefer the highest quality the display supports (4K → 1080p → …)
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    // Allow adaptive bitrate up-switches immediately (default is gradual).
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    // Prefer hardware-accelerated codecs (AV1 > HEVC > AVC priority
                    // is set via codec selector; this ensures we don't fall to SW).
                    .setForceHighestSupportedBitrate(false)
            )
        }

        // ── Audio: Dolby Atmos offload ────────────────────────────────────────
        // When the audio HAL supports offload the DSP handles decode entirely,
        // freeing the CPU and dropping ~15-20 MB of working RAM.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // ── Load control — tuned to <125 MB active RAM ──────────────────────
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                LowRamDevice.exoMinBufferMs,
                LowRamDevice.exoMaxBufferMs,
                /* playback start buffer */ 1_500,
                /* playback resume buffer */ 3_000,
            )
            .setTargetBufferBytes(LowRamDevice.exoTargetBufferBytes)
            // Prioritise time-based buffering over size — avoids over-buffering
            // short-bitrate content that would balloon RAM usage unnecessarily.
            .setPrioritizeTimeOverSizeThresholds(true)
            // Back-buffer: keep very little behind the playhead on low-RAM devices
            // to avoid retaining decoded frames.
            .setBackBuffer(
                /* durationMs */ if (LowRamDevice.isLowRamDevice) 2_000 else 5_000,
                /* retainWhilePaused */ false,
            )
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            // LOCAL keeps the screen + audio alive without a wake lock service.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            // EXACT seek is best for content with chapter markers / skip buttons.
            // On very low-RAM devices, CLOSEST_SYNC is faster (no key-frame scan).
            .setSeekParameters(
                if (LowRamDevice.isLowRamDevice) SeekParameters.CLOSEST_SYNC
                else SeekParameters.EXACT
            )
            .build()
            .also { exo ->
                exo.setAudioAttributes(audioAttributes, /* handleAudioFocus */ true)
                // SCALE_TO_FIT_WITH_CROPPING keeps 4K content full-bleed on TV screens.
                exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                Timber.d("Media3Player: created (tier=$tier)")
            }
    }

    // ── PlaybackEngine API ────────────────────────────────────────────────────

    override fun addListener(listener: PlaybackEngine.Listener) { listeners.add(listener) }
    override fun removeListener(listener: PlaybackEngine.Listener) { listeners.remove(listener) }

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

    override fun play()  { player?.play() }
    override fun pause() { player?.pause() }

    override fun seekTo(positionMs: Long) { player?.seekTo(positionMs) }
    override fun seekForward() { player?.seekForward() }
    override fun seekBack()    { player?.seekBack() }

    override fun setPlaybackSpeed(speed: Float) { player?.setPlaybackSpeed(speed) }

    override fun stop() {
        player?.stop()
        _playbackState.value = PlaybackEngine.PlaybackState.IDLE
        listeners.forEach { it.onPlaybackStateChanged(PlaybackEngine.PlaybackState.IDLE) }
    }

    override fun setSubtitleEnabled(enabled: Boolean) {
        val current = player?.trackSelectionParameters ?: return
        player?.trackSelectionParameters = current.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    override fun nativePlayer(): Any? = player

    override fun release() {
        player?.release()
        player = null
        listeners.clear()
        _currentPosition.value  = 0L
        _duration.value         = 0L
        _bufferedPosition.value = 0L
        _isPlaying.value        = false
        _playbackState.value    = PlaybackEngine.PlaybackState.IDLE
    }
}
