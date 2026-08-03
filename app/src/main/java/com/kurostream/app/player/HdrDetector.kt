package com.kurostream.app.player

import android.content.Context
import android.os.Build
import android.view.Display
import androidx.media3.common.Player
import timber.log.Timber

/**
 * Detects HDR display capabilities on Android TV / phone.
 * Uses Display.HdrCapabilities on API 24+.
 */
object HdrDetector {

    data class HdrInfo(
        val isHdrCapable: Boolean = false,
        val supportsHdr10: Boolean = false,
        val supportsHdr10Plus: Boolean = false,
        val supportsDolbyVision: Boolean = false,
        val supportsHlg: Boolean = false,
    )

    private var _cachedInfo: HdrInfo? = null
    private var context: Context? = null

    fun detect(context: Context): HdrInfo {
        this.context = context
        _cachedInfo?.let { return it }

        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay
            }
            val hdrCapabilities = display?.hdrCapabilities
            if (hdrCapabilities != null) {
                @Suppress("DEPRECATION")
                val supportedTypes = hdrCapabilities.supportedHdrTypes.toSet()
                HdrInfo(
                    isHdrCapable = supportedTypes.isNotEmpty(),
                    supportsHdr10 = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10),
                    supportsHdr10Plus = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS),
                    supportsDolbyVision = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION),
                    supportsHlg = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG),
                )
            } else {
                HdrInfo()
            }
        } else {
            HdrInfo()
        }

        _cachedInfo = info
        Timber.d("HdrDetector: $info")
        return info
    }

    fun configurePlayerForHdr(player: androidx.media3.common.Player) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val ctx = context ?: return
            val info = detect(ctx)
            if (info.supportsDolbyVision || info.supportsHdr10) {
            }
        }
    }

    fun supportsDolbyVision(): Boolean = context?.let { detect(it).supportsDolbyVision } ?: false
    fun supportsHdr10(): Boolean = context?.let { detect(it).supportsHdr10 } ?: false
    fun supportsHdr10Plus(): Boolean = context?.let { detect(it).supportsHdr10Plus } ?: false
}
