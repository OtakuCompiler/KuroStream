package com.kurostream.app.player

import android.content.Context
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.VideoScalingMode
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import timber.log.Timber

object PlayerConfig {

    private const val MIN_BUFFER_MS = 2_000
    private const val MAX_BUFFER_MS = 8_000
    private const val BUFFER_FOR_PLAYBACK_MS = 1_000
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000

    fun create(context: Context, lowRam: Boolean = true): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                codecs.sortedWith(compareByDescending {
                    when {
                        it.name.contains("ac3", true) -> 3
                        it.name.contains("eac3", true) -> 3
                        it.name.contains("dts", true) -> 2
                        it.name.contains("omx", true) && !it.name.contains("sw", true) -> 1
                        else -> 0
                    }
                })
            }

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage("jpn")
                    .setPreferredTextLanguage("eng")
                    .setTunnelingEnabled(true)
                    .clearMaxVideoSizeConstraints()
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setMaxVideoBitrate(8000000)
                    .setMinVideoBitrate(2000000)
                    .setViewportSizeToPhysicalDisplaySize(context, true)
            )
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioCapabilities(
                androidx.media3.exoplayer.audio.AudioCapabilities.getCapabilities(context)
            )
            .setEnableFloatOutput(false)
            .setOffloadMode(DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS)
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setAudioSink(audioSink)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setTargetBufferBytes(4 * 1024 * 1024)
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .build()
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .also { player ->
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    player.setTunnelingEnabled(true)
                }
                Timber.d("PlayerConfig: created 4K/HDR-ready player (lowRam=$lowRam, buffer=4MB)")
            }
    }
}
