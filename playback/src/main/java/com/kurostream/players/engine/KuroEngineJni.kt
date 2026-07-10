// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.players.engine

import android.content.Context
import android.os.Build
import android.view.Surface
import com.kurostream.players.engine.core.MediaItem
import com.kurostream.players.engine.core.SubtitleSource
import com.kurostream.players.engine.core.DrmConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * JNI bridge for KuroEngine native library.
 * All native methods must be implemented in kuroengine_jni.cpp
 */
class KuroEngineJni private constructor(private val context: Context) {

    companion object {
        private var instance: KuroEngineJni? = null
        private var loaded = false

        @JvmStatic
        fun getInstance(context: Context): KuroEngineJni {
            if (!loaded) {
                System.loadLibrary("kuroengine_jni")
                loaded = true
            }
            return instance ?: synchronized(this) {
                instance ?: KuroEngineJni(context.applicationContext).also { instance = it }
            }
        }
    }

    // ===== FFmpeg Core =====
    @JvmStatic external fun nativeInitFFmpeg(): Long
    @JvmStatic external fun nativeCleanup(handle: Long)
    @JvmStatic external fun nativeOpenInput(handle: Long, url: String): Int
    @JvmStatic external fun nativePlay(handle: Long)
    @JvmStatic external fun nativePause(handle: Long)
    @JvmStatic external fun nativeStop(handle: Long)
    @JvmStatic external fun nativeSeek(handle: Long, positionSeconds: Double)
    @JvmStatic external fun nativeSetSpeed(handle: Long, speed: Float)
    @JvmStatic external fun nativeSetHeaders(handle: Long, headersJson: String)
    @JvmStatic external fun nativeSetDecoderThreads(handle: Long, threads: Int)
    @JvmStatic external fun nativeEnableHardwareDecoding(handle: Long, enable: Boolean, decoderName: String)
    @JvmStatic external fun nativeFindBestVideoStream(handle: Long, hwCodecsJson: String): Int
    @JvmStatic external fun nativeFindBestAudioStream(handle: Long, preferredLanguage: String?): Int
    @JvmStatic external fun nativeSelectStream(handle: Long, streamIndex: Int)
    @JvmStatic external fun nativeGetVideoCodec(handle: Long): String
    @JvmStatic external fun nativeGetAudioCodec(handle: Long): String
    @JvmStatic external fun nativeGetVideoResolution(handle: Long): String
    @JvmStatic external fun nativeGetFrameRate(handle: Long): Float
    @JvmStatic external fun nativeGetBitrate(handle: Long): Long
    @JvmStatic external fun nativeGetBufferHealth(handle: Long): Int
    @JvmStatic external fun nativeGetDroppedFrames(handle: Long): Int

    // ===== Libplacebo =====
    @JvmStatic external fun nativeInitLibplacebo(fontsDir: String): Long
    @JvmStatic external fun nativeCleanupLibplacebo(handle: Long)
    @JvmStatic external fun nativeConfigureLibplacebo(handle: Long, surface: Surface, configJson: String)
    @JvmStatic external fun nativePlaceboRenderFrame(handle: Long)
    @JvmStatic external fun nativePlaceboSetUpscaling(handle: Long, enabled: Boolean, factor: Float)
    @JvmStatic external fun nativePlaceboSetToneMapping(handle: Long, algorithm: Int, paramsJson: String)
    @JvmStatic external fun nativePlaceboSetDebanding(handle: Long, enabled: Boolean, strength: Float)
    @JvmStatic external fun nativePlaceboSetFrameInterpolation(handle: Long, enabled: Boolean)

    // ===== Libass =====
    @JvmStatic external fun nativeInitLibass(fontsDir: String): Long
    @JvmStatic external fun nativeCleanupLibass(handle: Long)
    @JvmStatic external fun nativeAssSetStyle(handle: Long, styleJson: String)
    @JvmStatic external fun nativeAssRenderSubtitle(handle: Long, text: String, timestampMs: Long, width: Int, height: Int): ByteArray // RGBA bitmap
    @JvmStatic external fun nativeAssLoadExternal(handle: Long, path: String, language: String): Boolean
    @JvmStatic external fun nativeAssSelectTrack(handle: Long, trackIndex: Int)
    @JvmStatic external fun nativeAssGetTrackCount(handle: Long): Int
    @JvmStatic external fun nativeAssGetTrackInfo(handle: Long, index: Int): String // JSON

    // ===== Sonic Audio DSP =====
    @JvmStatic external fun nativeInitSonicDsp(sampleRate: Int, channels: Int): Long
    @JvmStatic external fun nativeCleanupSonicDsp(handle: Long)
    @JvmStatic external fun nativeSonicSetVolume(handle: Long, volume: Float)
    @JvmStatic external fun nativeSonicSetMute(handle: Long, mute: Boolean)
    @JvmStatic external fun nativeSonicSetEq(handle: Long, bandsJson: String)
    @JvmStatic external fun nativeSonicSetNightMode(handle: Long, enabled: Boolean, thresholdDb: Float, ratio: Float)
    @JvmStatic external fun nativeSonicSetLoudnessNorm(handle: Long, enabled: Boolean, targetLufs: Float)
    @JvmStatic external fun nativeSonicSetResampleQuality(handle: Long, quality: Int)
    @JvmStatic external fun nativeSonicProcessBuffer(handle: Long, input: ByteArray, output: ByteArray, frames: Int)

    // ===== Performance Metrics =====
    @JvmStatic external fun nativeGetPerformanceMetrics(handle: Long): String // JSON

    // ===== Codec Pack Loading =====
    @JvmStatic external fun nativeLoadCodecPack(handle: Long, packPath: String): Boolean
    @JvmStatic external fun nativeUnloadCodecPack(handle: Long, codecName: String)
    @JvmStatic external fun nativeGetLoadedCodecPacks(handle: Long): String // JSON array

    // ===== DRM =====
    @JvmStatic external fun nativeSetDrmConfig(handle: Long, drmConfigJson: String): Boolean
    @JvmStatic external fun nativeProcessDrmKeyRequest(handle: Long, request: ByteArray): ByteArray

    // ===== Surface =====
    @JvmStatic external fun nativeSetSurface(handle: Long, surface: Surface)
    @JvmStatic external fun nativeSetSurfaceSize(handle: Long, width: Int, height: Int)

    // ===== Subtitle Loading =====
    fun loadExternalSubtitle(path: String, language: String): Boolean {
        // Called from Kotlin, delegates to libass
        val kuroEngine = KuroEngine.getInstance(context) // assuming static access
        return if (kuroEngine.libassHandle != 0L) {
            nativeAssLoadExternal(kuroEngine.libassHandle, path, language)
        } else false
    }
}

/**
 * JNI data classes for configuration passing
 */
data class JniPlaceboConfig(
    val enableUpscaling: Boolean = false,
    val enableDebanding: Boolean = true,
    val enableToneMapping: Boolean = true,
    val toneMappingAlgorithm: Int = 0, // 0=BT2390, 1=Hable, 2=Reinhard
    val toneMappingParams: String = "{}",
    val enableFrameInterpolation: Boolean = false,
    val maxUpscaleFactor: Float = 2.0f,
    val debandingStrength: Float = 0.02f,
    val debandingRange: Float = 16f,
)

data class JniSonicDspConfig(
    val eqBands: FloatArray = FloatArray(10) { 0f },
    val nightModeEnabled: Boolean = false,
    val nightModeThresholdDb: Float = -20f,
    val nightModeRatio: Float = 4f,
    val loudnessNormEnabled: Boolean = true,
    val loudnessTargetLufs: Float = -14f,
    val resampleQuality: Int = 2, // 0=low, 1=med, 2=high, 3=very_high
)

data class JniDrmConfig(
    val schemeUuid: String = "",
    val licenseUri: String = "",
    val keyRequestProperties: String = "{}",
)

/**
 * Tone mapping algorithms (must match native enum)
 */
enum class ToneMappingAlgorithm(val value: Int) {
    BT2390(0),
    HABLE(1),
    REINHARD(2),
    MOBILITY(3),
    LINEAR(4),
}

/**
 * Resample quality levels
 */
enum class ResampleQuality(val value: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    VERY_HIGH(3),
}