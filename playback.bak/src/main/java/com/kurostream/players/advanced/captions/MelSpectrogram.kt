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

package com.kurostream.players.advanced.captions

import kotlin.math.*

/**
 * Optimized mel spectrogram computation for Whisper model input.
 * Pre-computes filter banks for efficiency.
 */
class MelSpectrogram(
    private val sampleRate: Int = 16000,
    private val nFft: Int = 400,
    private val nMels: Int = 80,
    private val hopLength: Int = 160,
    private val maxDuration: Int = 30  // seconds
) {
    private val melFilters: Array<FloatArray> = computeMelFilters()
    private val window: FloatArray = FloatArray(nFft) { i ->
        0.54f - 0.46f * cos(2 * PI * i / (nFft - 1)).toFloat()
    }

    private val maxSamples = sampleRate * maxDuration
    private val maxFrames = maxSamples / hopLength

    /**
     * Compute mel spectrogram from raw audio samples.
     * Returns flattened array suitable for Tensor input.
     */
    fun compute(samples: FloatArray): FloatArray {
        // Pad or truncate to max duration
        val paddedSamples = when {
            samples.size < maxSamples -> samples.copyOf(maxSamples)
            samples.size > maxSamples -> samples.copyOfRange(0, maxSamples)
            else -> samples
        }

        val nFrames = min(paddedSamples.size / hopLength, maxFrames)
        val melSpec = FloatArray(nMels * maxFrames) // Fixed size for model input

        for (t in 0 until nFrames) {
            val frameStart = t * hopLength
            val frame = FloatArray(nFft)

            // Apply window and zero-pad
            for (i in 0 until nFft) {
                val sampleIdx = frameStart + i - nFft / 2
                frame[i] = when {
                    sampleIdx < 0 || sampleIdx >= paddedSamples.size -> 0f
                    else -> paddedSamples[sampleIdx] * window[i]
                }
            }

            // Compute FFT magnitude
            val fftMagnitude = computeFFT(frame)

            // Apply mel filter banks
            for (m in 0 until nMels) {
                var melEnergy = 0f
                for (f in 0 until nFft / 2 + 1) {
                    melEnergy += fftMagnitude[f] * melFilters[m][f]
                }
                // Log scale with clamping
                melSpec[m * maxFrames + t] = ln(max(melEnergy, 1e-10f))
            }
        }

        return melSpec
    }

    private fun computeFFT(frame: FloatArray): FloatArray {
        val n = frame.size
        val result = FloatArray(n / 2 + 1)

        // Simple DFT for small frames (n=400)
        // For production, use FFTW or similar optimized library
        for (k in 0..n / 2) {
            var real = 0f
            var imag = 0f
            for (t in 0 until n) {
                val angle = -2 * PI * k * t / n
                real += frame[t] * cos(angle).toFloat()
                imag += frame[t] * sin(angle).toFloat()
            }
            result[k] = sqrt(real * real + imag * imag)
        }

        return result
    }

    private fun computeMelFilters(): Array<FloatArray> {
        val fMin = 0f
        val fMax = sampleRate / 2f

        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        val melPoints = FloatArray(nMels + 2) { i ->
            melMin + (melMax - melMin) * i / (nMels + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }.toFloatArray()
        val fftFreqs = FloatArray(nFft / 2 + 1) { i -> sampleRate * i.toFloat() / nFft }

        val filters = Array(nMels) { FloatArray(nFft / 2 + 1) }

        for (m in 1..nMels) {
            val fLeft = hzPoints[m - 1]
            val fCenter = hzPoints[m]
            val fRight = hzPoints[m + 1]

            for (f in 0 until nFft / 2 + 1) {
                val freq = fftFreqs[f]
                filters[m - 1][f] = when {
                    freq <= fLeft || freq >= fRight -> 0f
                    freq < fCenter -> (freq - fLeft) / (fCenter - fLeft)
                    else -> (fRight - freq) / (fRight - fCenter)
                }
            }
        }

        return filters
    }

    private fun hzToMel(hz: Float): Float {
        return 2595f * log10(1 + hz / 700f)
    }

    private fun melToHz(mel: Float): Float {
        return 700f * (10f.pow(mel / 2595f) - 1f)
    }
}
