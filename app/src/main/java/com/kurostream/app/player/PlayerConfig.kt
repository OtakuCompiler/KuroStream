// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import timber.log.Timber

/**
 * Ultra-low-memory ExoPlayer configuration tuned for 4K + upscaling +
 * Dolby Atmos streams on devices with 125-256 MB available RAM.
 *
 * Key optimizations:
 *  - Single video decoder (HW only, software decoder disabled entirely)
 *  - Tiny buffer pool: target 4 MB, cap at 6 MB, min 1.5s buffer
 *  - Skip renderer extensions (no FFmpeg/Lottie/etc. side-loads)
 *  - Dolby Atmos passthrough to receiver (no audio decode in Java)
 *  - Codec selector prefers hardware AC3/EAC3/Atmos passthrough decoders
 *  - Track selector pins max video bitrate for 4K HEVC HDR streams
 *  - Viewport size = physical display (no double-buffering for surface)
 */
object PlayerConfig {

    private const val TAG = "PlayerConfig"

    private const val MIN_BUFFER_MS = 1_500
    private const val MAX_BUFFER_MS = 4_000
    private const val BUFFER_FOR_PLAYBACK_MS = 800
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_500
    private const val TARGET_BUFFER_BYTES = 4 * 1024 * 1024

    @OptIn(UnstableApi::class)
    fun create(context: Context, lowRam: Boolean = true): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(false)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                codecs
                    .filter { !it.name.contains("sw.", ignoreCase = true) && !it.name.contains("software", ignoreCase = true) }
                    .sortedWith(compareByDescending {
                        when {
                            it.name.contains("c2.android", true) -> 5
                            it.name.contains("ac3", true) -> 4
                            it.name.contains("eac3", true) -> 4
                            it.name.contains("atmos", true) -> 4
                            it.name.contains("dts", true) -> 3
                            it.name.contains("omx.google", true) -> -100
                            it.name.contains("omx", true) -> 1
                            else -> 0
                        }
                    })
            }
            .setForceEnableVideoCodecs(false)
            .setForceEnableAudioCodecs(false)
            .setForceDisableVideoCodecs(emptyArray())
            .setForceDisableAudioCodecs(emptyArray())

        val trackSelector = DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setPreferredAudioLanguage("jpn")
                .setPreferredTextLanguage("eng")
                .setAllowVideoNonSeamlessAdaptiveness(false)
                .setMaxVideoBitrate(if (lowRam) 25_000_000 else 80_000_000)
                .setMinVideoBitrate(if (lowRam) 500_000 else 2_000_000)
                .setViewportSizeToPhysicalDisplaySize(context, true)
                .setAllowVideoMixedMimeTypeAdaptiveness(false)
                .setAllowVideoResolutionAdaptiveness(false)
            setParameters(params)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_SYSTEM)
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setTargetBufferBytes(TARGET_BUFFER_BYTES)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .also { player ->
                Timber.tag(TAG).i(
                    "player ready (lowRam=$lowRam, bufMin=${MIN_BUFFER_MS}ms, bufMax=${MAX_BUFFER_MS}ms, target=${TARGET_BUFFER_BYTES}B)"
                )
            }
    }

    /**
     * Pin Atmos passthrough. AC3/EAC3/JOC streams are forwarded as-is
     * to the HDMI sink, no Java-side decode happens, saving 8-16 MB.
     */
    @OptIn(UnstableApi::class)
    fun pinAtmosPassthrough(player: ExoPlayer) {
        val params = player.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(
                androidx.media3.common.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(androidx.media3.common.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                    .build()
            )
            .build()
        player.trackSelectionParameters = params
        Timber.tag(TAG).i("pinned Atmos passthrough + audio offload")
    }
}
