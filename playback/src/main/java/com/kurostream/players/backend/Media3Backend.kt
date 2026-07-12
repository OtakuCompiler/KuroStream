package com.kurostream.players.backend

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class Media3Backend(
    private val context: Context,
    private val trackSelector: DefaultTrackSelector = DefaultTrackSelector(context),
) : PlayerBackend {

    override val type: PlayerBackendType = PlayerBackendType.Media3

    private val exoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .build()

    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _error = MutableStateFlow<PlaybackException?>(null)

    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    override val duration: StateFlow<Long> = _duration.asStateFlow()
    override val error: StateFlow<PlaybackException?> = _error.asStateFlow()

    override val capabilities: PlaybackCapabilities
        get() {
            val codecs = listOf("avc1", "hev1", "hvc1", "vp9", "av01")
                .filter { isCodecSupported(it) }
                .toSet()
            
            val supportsHEVC = isCodecSupported("hev1") || isCodecSupported("hvc1")
            
            return PlaybackCapabilities(
                supportsHDR = supportsHEVC,
                supportsHDR10 = supportsHEVC,
                supportsDolbyVision = false,
                supportsCodec = codecs,
                supportsPassthrough = true,
                maxResolution = 3840 to 2160,
                supportedFrameRates = listOf(24f, 25f, 30f, 48f, 50f, 60f),
            )
        }

    private fun isCodecSupported(codec: String): Boolean {
        val mimeType = when (codec) {
            "avc1" -> C.MIME_TYPE_VIDEO_AVC
            "hev1", "hvc1" -> C.MIME_TYPE_VIDEO_HEVC
            "vp9" -> C.MIME_TYPE_VIDEO_VP9
            "av01" -> C.MIME_TYPE_VIDEO_AV1
            else -> return false
        }
        return try {
            androidx.media3.common.util.Util.checkVideoDecoderSupport(mimeType, false) ||
            androidx.media3.common.util.Util.checkVideoEncoderSupport(mimeType, false)
        } catch (_: Exception) {
            false
        }
    }

    override fun prepare(mediaItem: MediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        updateState()
    }

    override fun play() {
        exoPlayer.play()
        updateState()
    }

    override fun pause() {
        exoPlayer.pause()
        updateState()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.25f, 8f))
    }

    override fun setAudioTrack(trackId: String) {
        val parameters = trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(trackId)
        trackSelector.setParameters(parameters)
    }

    override fun setVideoTrack(trackId: String) {
        // ExoPlayer handles video track selection automatically
    }

    override fun setSubtitleTrack(trackId: String) {
        val parameters = trackSelector.buildUponParameters()
            .setPreferredTextLanguage(trackId)
        trackSelector.setParameters(parameters)
    }

    override fun release() {
        exoPlayer.release()
    }

    private fun updateState() {
        exoPlayer.let { player ->
            _isPlaying.value = player.isPlaying
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        }
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updateState()
                if (state == Player.STATE_ENDED) {
                    Timber.d("Playback ended")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.value = error
                Timber.e(error, "Playback error")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }
}
