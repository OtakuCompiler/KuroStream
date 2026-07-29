package com.kurostream.app.player

import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import timber.log.Timber

/**
 * Detects hardware decoder capabilities at runtime.
 * Results are cached for the process lifetime — call [detect] once during init.
 */
object CodecCapabilityDetector {

    data class CodecInfo(
        val name: String,
        val isHardware: Boolean,
        val mimeType: String,
        val supportsHdr10: Boolean = false,
        val supportsHdr10Plus: Boolean = false,
        val supportsDolbyVision: Boolean = false,
        val supportsHlg: Boolean = false,
        val maxResolution: Int = 0,
    )

    private var _codecs: List<CodecInfo>? = null
    val codecs: List<CodecInfo> get() = _codecs ?: detect()

    var hasHardwareDecoder: Boolean = false
        private set
    var supportsHdr: Boolean = false
        private set
    var supportsAv1: Boolean = false
        private set
    var supportsHevc: Boolean = false
        private set
    var supportsDolbyVision: Boolean = false
        private set

    fun detect(): List<CodecInfo> {
        return _codecs ?: run {
            val results = mutableListOf<CodecInfo>()
            val codecList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            } else {
                @Suppress("DEPRECATION")
                MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            }

            for (codec in codecList) {
                if (!codec.isEncoder) {
                    val name = codec.name
                    val isHardware = isHardwareCodec(name)
                    val mimeTypes = codec.supportedTypes.toList()
                    val hasHdr10 = mimeTypes.any { it.contains("hevc") } &&
                        codec.getCapabilitiesForType("video/hevc")?.let { caps ->
                            caps.profileLevels.any { it.profile >= android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 }
                        } ?: false
                    val hasDolbyVision = mimeTypes.any { it.contains("dolby") }

                    mimeTypes.forEach { mime ->
                        results.add(
                            CodecInfo(
                                name = name,
                                isHardware = isHardware,
                                mimeType = mime,
                                supportsHdr10 = hasHdr10,
                                supportsDolbyVision = hasDolbyVision,
                            )
                        )
                    }

                    if (isHardware) hasHardwareDecoder = true
                    if (hasHdr10) supportsHdr = true
                    if (hasDolbyVision) supportsDolbyVision = true
                    if (mimeTypes.any { it == "video/av01" || it == "video/avc1" }) supportsAv1 = true
                    if (mimeTypes.any { it == "video/hevc" || it == "video/hevc1" }) supportsHevc = true
                }
            }

            _codecs = results
            Timber.d("CodecCapability: HW=$hasHardwareDecoder HDR=$supportsHdr AV1=$supportsAv1 HEVC=$supportsHevc DV=$supportsDolbyVision (${results.size} codec entries)")
            results
        }
    }

    /** Prefer software codecs for known hardware prefixes to actively avoid them. */
    private fun isHardwareCodec(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("omx.") && (
            lower.contains("qcom") ||
            lower.contains("nvidia") ||
            lower.contains("exynos") ||
            lower.contains("ti") ||
            lower.contains("intel") ||
            lower.contains("hisi") ||
            lower.contains("mediatek") ||
            lower.contains("samsung") ||
            lower.contains("amlogic") ||
            lower.contains("rockchip")
        )
    }

    fun preferHardwareDecoderFor(mimeType: String): Boolean {
        if (!hasHardwareDecoder) return false
        return codecs.any { it.isHardware && it.mimeType == mimeType }
    }
}
