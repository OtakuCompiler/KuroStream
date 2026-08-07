package com.kurostream.desktop.playback

/**
 * Default playback knobs for the desktop build. On desktop we don't have
 * the tight RAM constraints of webOS/Tizen, so we keep the full pipeline
 * (Dolby Atmos passthrough, frame interpolation, AI upscaling) on by
 * default for capable GPUs. Users can disable individual features in
 * Settings if their hardware struggles.
 */
object DesktopPlaybackSettings {
    const val DEFAULT_BACKEND = "auto"
    const val DEFAULT_QUALITY = "1080p"
    const val DOLBY_ATMOS_PASSTHROUGH_DEFAULT = true
    const val FRAME_INTERPOLATION_DEFAULT = false
    const val AI_UPSCALING_DEFAULT = false
    const val HARDWARE_DECODER_DEFAULT = true
}
