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

package com.kurostream.players.advanced.audio

import android.content.Context
import androidx.annotation.Keep
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Advanced audio DSP with Oboe/Superpowered integration.
 * Features: night mode, loudness normalization, 10-band EQ.
 */
@Keep
class AudioDSPManager(
    private val context: Context
) {
    companion object {
        const val SAMPLE_RATE = 48000
        const val CHANNEL_COUNT = 2
        const val EQ_BANDS = 10
        val EQ_FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        const val LOUDNESS_TARGET_LUFS = -14f
        const val NIGHT_MODE_MAX_GAIN = 0.3f
    }

    private val dspScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isInitialized = AtomicBoolean(false)
    private val isNightMode = AtomicBoolean(false)
    private val isLoudnessNormalization = AtomicBoolean(true)

    private var nativeEngine: Long = 0
    private val eqGains = FloatArray(EQ_BANDS) { 0f }
    private val _eqSettingsFlow = MutableStateFlow(eqGains.copyOf())
    val eqSettingsFlow: StateFlow<FloatArray> = _eqSettingsFlow.asStateFlow()

    private val _loudnessFlow = MutableStateFlow(0f)
    val loudnessFlow: StateFlow<Float> = _loudnessFlow.asStateFlow()

    data class NightModeSettings(
        val compressRange: Boolean = true,
        val reduceBass: Boolean = true,
        val maxVolume: Float = NIGHT_MODE_MAX_GAIN,
        val voiceBoost: Boolean = true
    )
    private val nightModeSettings = AtomicReference(NightModeSettings())

    init {
        System.loadLibrary("kurostream_dsp")
        nativeEngine = nativeCreateEngine(SAMPLE_RATE, CHANNEL_COUNT)
        isInitialized.set(true)
    }

    @Keep
    private external fun nativeCreateEngine(sampleRate: Int, channels: Int): Long
    @Keep
    private external fun nativeDestroyEngine(engine: Long)
    @Keep
    private external fun nativeProcessBuffer(engine: Long, input: ByteArray, output: ByteArray, frames: Int)
    @Keep
    private external fun nativeSetEQGain(engine: Long, band: Int, gainDb: Float)
    @Keep
    private external fun nativeSetNightMode(engine: Long, enabled: Boolean, settings: NightModeSettings)
    @Keep
    private external fun nativeSetLoudnessNormalization(engine: Long, targetLufs: Float, enabled: Boolean)
    @Keep
    private external fun nativeGetLoudnessMeasurement(engine: Long): Float
    @Keep
    private external fun nativeResetState(engine: Long)

    fun processAudio(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer, frames: Int) {
        if (!isInitialized.get()) return
        val inputArray = ByteArray(frames * CHANNEL_COUNT * 2)
        val outputArray = ByteArray(frames * CHANNEL_COUNT * 2)
        inputBuffer.get(inputArray)
        nativeProcessBuffer(nativeEngine, inputArray, outputArray, frames)
        outputBuffer.put(outputArray)
        if (frames % 1024 == 0) {
            val loudness = nativeGetLoudnessMeasurement(nativeEngine)
            _loudnessFlow.tryEmit(loudness)
        }
    }

    fun setEQBand(band: Int, gainDb: Float) {
        require(band in 0 until EQ_BANDS) { "Invalid band index: $band" }
        require(gainDb in -12f..12f) { "Gain out of range: $gainDb" }
        eqGains[band] = gainDb
        _eqSettingsFlow.value = eqGains.copyOf()
        nativeSetEQGain(nativeEngine, band, gainDb)
    }

    fun applyEQPreset(preset: EQPreset) {
        val gains = when (preset) {
            EQPreset.FLAT -> FloatArray(EQ_BANDS) { 0f }
            EQPreset.BASS_BOOST -> floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)
            EQPreset.VOCAL_BOOST -> floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f)
            EQPreset.TREBLE_BOOST -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f)
            EQPreset.NIGHT_MODE -> floatArrayOf(-6f, -4f, -2f, 0f, 2f, 2f, 0f, -2f, -4f, -6f)
            EQPreset.LOUDNESS -> floatArrayOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f)
        }
        gains.forEachIndexed { index, gain -> setEQBand(index, gain) }
    }

    enum class EQPreset { FLAT, BASS_BOOST, VOCAL_BOOST, TREBLE_BOOST, NIGHT_MODE, LOUDNESS }

    fun setNightMode(enabled: Boolean, settings: NightModeSettings = NightModeSettings()) {
        isNightMode.set(enabled)
        nightModeSettings.set(settings)
        nativeSetNightMode(nativeEngine, enabled, settings)
    }

    fun setLoudnessNormalization(enabled: Boolean, targetLufs: Float = LOUDNESS_TARGET_LUFS) {
        isLoudnessNormalization.set(enabled)
        nativeSetLoudnessNormalization(nativeEngine, targetLufs, enabled)
    }

    fun reset() {
        nativeResetState(nativeEngine)
        eqGains.fill(0f)
        _eqSettingsFlow.value = eqGains.copyOf()
    }

    fun release() {
        isInitialized.set(false)
        nativeDestroyEngine(nativeEngine)
        nativeEngine = 0
        dspScope.cancel()
    }
}
