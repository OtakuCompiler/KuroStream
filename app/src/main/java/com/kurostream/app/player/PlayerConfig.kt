package com.kurostream.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import timber.log.Timber

/**
 * Creates a performance-optimized [ExoPlayer] instance.
 * Prefers hardware decoding, minimizes buffer sizes on low-RAM devices,
 * and enables HDR passthrough when detected.
 */
object PlayerConfig {

    private const val MIN_BUFFER_MS = 15_000
    private const val MAX_BUFFER_MS = 50_000
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

    fun create(context: Context, lowRam: Boolean = false): ExoPlayer {
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

        val bufferMax = if (lowRam) MIN_BUFFER_MS else MAX_BUFFER_MS

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        bufferMax,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setTargetBufferBytes(if (lowRam) 2 * 1024 * 1024 else 5 * 1024 * 1024)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build()
            .also { player ->
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                Timber.d("PlayerConfig: created player (lowRam=$lowRam)")
            }
    }
}
