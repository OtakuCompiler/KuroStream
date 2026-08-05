package com.kurostream.players.upscaling

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Surface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpscalingManager @Inject constructor(
    context: Context
) {

    private val prefs = context.getSharedPreferences("upscaling", Context.MODE_PRIVATE)

    private var attachedSurface: Surface? = null
    private var attachedWidth: Int = 0
    private var attachedHeight: Int = 0

    var isAiUpscalingEnabled: Boolean
        get() = prefs.getBoolean("ai_upscaling", false)
        set(value) = prefs.edit().putBoolean("ai_upscaling", value).apply()

    var isFrameInterpolationEnabled: Boolean
        get() = prefs.getBoolean("frame_interpolation", false)
        set(value) = prefs.edit().putBoolean("frame_interpolation", value).apply()

    var targetResolution: String
        get() = prefs.getString("target_resolution", "4K") ?: "4K"
        set(value) = prefs.edit().putString("target_resolution", value).apply()

    fun isDeviceSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { codec ->
                !codec.isEncoder && codec.name.contains("qti", ignoreCase = true) &&
                (codec.name.contains("av1", ignoreCase = true) || codec.name.contains("hevc", ignoreCase = true))
            }
        } catch (e: Exception) {
            false
        }
    }

    fun applyToExoPlayer(player: androidx.media3.exoplayer.ExoPlayer) {
        if (!isAiUpscalingEnabled) return
        player.setVideoScalingMode(androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
    }

    fun attach(surface: Surface, width: Int, height: Int) {
        attachedSurface = surface
        attachedWidth = width
        attachedHeight = height
    }

    fun release() {
        attachedSurface = null
        attachedWidth = 0
        attachedHeight = 0
    }
}
