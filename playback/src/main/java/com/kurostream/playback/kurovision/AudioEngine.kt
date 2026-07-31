// This file is part of KuroStream.
//
// KuroAudioEngine — audio passthrough and enhancement.
// Wraps Media3/VLC audio output with passthrough negotiation and simple
// enhancement (dialogue boost, night mode, volume normalization).
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.media.AudioFormat
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroAudioEngine @Inject constructor(
    private val settings: KuroVisionSettings,
) {
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    suspend fun initialize(audioSessionId: Int) {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
                setTargetGain(0)
            }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
                setTargetGain(0)
            }
            Log.i(TAG, "KuroAudioEngine initialized session=$audioSessionId")
        } catch (t: Throwable) {
            Log.w(TAG, "Audio init failed: ${t.message}")
        }
    }

    fun applyDialogueBoost(level: Float) {
        try {
            equalizer?.let {
                val midBand = (it.numberOfBands / 2).coerceAtLeast(0)
                val boost = (level * 500).toInt().toShort().coerceIn(-1000, 1000)
                it.setBandLevel(midBand.toShort(), boost)
            }
        } catch (_: Exception) { }
    }

    fun applyNightMode(enable: Boolean) {
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(if (enable) 600 else 0)
            }
        } catch (_: Exception) { }
    }

    fun release() {
        try {
            equalizer?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) { }
        equalizer = null
        loudnessEnhancer = null
    }

    data class AudioState(
        val dialogueBoost: Float = 0f,
        val nightMode: Boolean = false,
        val volumeNormalization: Boolean = false,
        val audioDelayMs: Int = 0,
    )

    companion object {
        private const val TAG = "KuroAudioEngine"
    }
}
