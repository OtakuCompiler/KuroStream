// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.player

import android.content.Context
import android.os.Build
import android.view.Display
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

/**
 * Detects HDR display capabilities and configures the media player accordingly.
 *
 * On non-HDR panels we offer a Fake HDR mode via [KuroVision] that simulates
 * HDR contrast on SDR displays — handled at the shader level in
 * [EnhancedUpscaleEngine].
 */
object HdrDetector {

    data class HdrInfo(
        val isHdrCapable:         Boolean = false,
        val supportsHdr10:        Boolean = false,
        val supportsHdr10Plus:    Boolean = false,
        val supportsDolbyVision:  Boolean = false,
        val supportsHlg:          Boolean = false,
        val maxLuminance:         Float   = 0f,   // cd/m² — 0 = unknown
        val minLuminance:         Float   = 0f,
    ) {
        val isTrueHdr: Boolean get() = supportsHdr10 || supportsDolbyVision || supportsHdr10Plus
    }

    private var _cached: HdrInfo? = null

    fun detect(context: Context): HdrInfo {
        _cached?.let { return it }

        val info: HdrInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay
            }
            val hdr = display?.hdrCapabilities
            if (hdr != null) {
                val types = hdr.supportedHdrTypes.toSet()
                HdrInfo(
                    isHdrCapable        = types.isNotEmpty(),
                    supportsHdr10       = Display.HdrCapabilities.HDR_TYPE_HDR10 in types,
                    supportsHdr10Plus   = Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in types,
                    supportsDolbyVision = Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION in types,
                    supportsHlg         = Display.HdrCapabilities.HDR_TYPE_HLG in types,
                    maxLuminance        = runCatching { hdr.desiredMaxAverageLuminance }.getOrDefault(0f),
                    minLuminance        = runCatching { hdr.desiredMinLuminance }.getOrDefault(0f),
                )
            } else HdrInfo()
        } else HdrInfo()

        _cached = info
        Timber.d("HdrDetector: $info")
        return info
    }

    fun invalidateCache() { _cached = null }

    /** Apply HDR track-selection and tunneling hints to an ExoPlayer instance. */
    @OptIn(UnstableApi::class)
    fun configurePlayerForHdr(player: Player, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val info = detect(context)
        if (player !is ExoPlayer) return

        // Tunneled rendering is required for Dolby Vision and HDR10+ passthrough
        // on Android TV — it bypasses the compositor and sends the frame directly
        // to the display pipeline.
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredVideoRoleFlags(C.ROLE_FLAG_MAIN)
            .also { builder ->
                if (info.isTrueHdr) {
                    // Allow HDR tracks — ExoPlayer will prefer them automatically
                    // when tunneling is enabled (set in Media3Player's TrackSelector).
                    builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    builder.setMaxVideoBitrate(Int.MAX_VALUE)
                }
            }
            .build()

        Timber.d("HdrDetector: player configured for HDR (hdrCapable=${info.isHdrCapable}, dv=${info.supportsDolbyVision})")
    }

    fun supportsDolbyVision(context: Context): Boolean = detect(context).supportsDolbyVision
    fun supportsHdr10(context: Context): Boolean       = detect(context).supportsHdr10
    fun supportsHdr10Plus(context: Context): Boolean   = detect(context).supportsHdr10Plus
    fun isHdrCapable(context: Context): Boolean        = detect(context).isHdrCapable
}
