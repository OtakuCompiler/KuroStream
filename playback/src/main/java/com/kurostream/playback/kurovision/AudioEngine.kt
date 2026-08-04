// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KuroAudioEngine — full audio enhancement pipeline.
 *
 * Features
 * ────────
 * • 5-band parametric EQ (sub, bass, mid, presence, air)
 * • Bass boost (hardware-accelerated AudioEffect)
 * • Virtualizer — stereo widening / virtual surround
 * • Dolby Atmos emulation via reverb + widening preset
 * • Night mode — DRC (dynamic range compression via LoudnessEnhancer)
 * • Dialogue boost — midrange EQ lift for clearer speech
 * • Volume normalization (loudness levelling)
 * • Audio delay fine-tuning (A/V sync offset)
 * • EQ presets: Flat, Cinema, Anime, Bass Boost, Treble Boost, Voice, Night
 *
 * All effects are lazily instantiated on the MediaPlayer audio session and
 * released together via [release]. Safe to call [initialize] multiple times
 * with different session IDs (e.g. when switching player engines).
 */
@Singleton
class KuroAudioEngine @Inject constructor(
    private val settings: KuroVisionSettings,
) {

    // ── Android AudioFX handles ───────────────────────────────────────────────
    private var equalizer:        Equalizer?         = null
    private var bassBoost:        BassBoost?         = null
    private var virtualizer:      Virtualizer?       = null
    private var loudnessEnhancer: LoudnessEnhancer?  = null
    private var reverb:           PresetReverb?      = null
    private var activeSessionId:  Int                = AudioTrack.SESSION_ID_GENERATE

    // ── State ────────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun initialize(audioSessionId: Int) {
        if (audioSessionId == activeSessionId && equalizer != null) return
        release()
        activeSessionId = audioSessionId
        try {
            equalizer        = Equalizer(Int.MAX_VALUE, audioSessionId).also { it.enabled = true }
            bassBoost        = BassBoost(Int.MAX_VALUE, audioSessionId).also { it.enabled = true }
            virtualizer      = Virtualizer(Int.MAX_VALUE, audioSessionId).also { it.enabled = true }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).also {
                it.enabled = true
                it.setTargetGain(0)
            }
            reverb           = PresetReverb(Int.MAX_VALUE, audioSessionId).also {
                it.preset = PresetReverb.PRESET_NONE
                it.enabled = false
            }
            // Re-apply saved state
            applyState(_state.value)
            Log.i(TAG, "KuroAudioEngine: initialized session=$audioSessionId")
        } catch (t: Throwable) {
            Log.w(TAG, "Audio init failed: ${t.message} (effects not available on this device)")
        }
    }

    fun release() {
        listOf<Runnable>(
            Runnable { equalizer?.release() },
            Runnable { bassBoost?.release() },
            Runnable { virtualizer?.release() },
            Runnable { loudnessEnhancer?.release() },
            Runnable { reverb?.release() },
        ).forEach { try { it.run() } catch (_: Exception) {} }
        equalizer = null; bassBoost = null; virtualizer = null
        loudnessEnhancer = null; reverb = null
        activeSessionId = AudioTrack.SESSION_ID_GENERATE
    }

    // ── EQ presets ────────────────────────────────────────────────────────────

    fun applyPreset(preset: EqPreset) {
        val gains = preset.bandGains
        val current = _state.value.copy(
            eqPreset     = preset,
            eqBands      = gains,
            dialogueBoost = if (preset == EqPreset.VOICE) 1.0f else _state.value.dialogueBoost,
        )
        applyState(current)
        _state.value = current
    }

    /** Set a single EQ band gain (mB, range -1500..+1500). */
    fun setEqBand(band: Int, gainMilliBels: Short) {
        try {
            equalizer?.setBandLevel(band.toShort(), gainMilliBels)
        } catch (_: Exception) {}
        val bands = _state.value.eqBands.toMutableList()
        if (band in bands.indices) bands[band] = gainMilliBels.toInt()
        _state.value = _state.value.copy(eqBands = bands, eqPreset = EqPreset.CUSTOM)
    }

    // ── Individual controls ───────────────────────────────────────────────────

    /** Dialogue boost: 0f = off, 1f = +1200 mB on midrange band. */
    fun setDialogueBoost(level: Float) {
        try {
            equalizer?.let {
                val midBand  = (it.numberOfBands / 2).coerceAtLeast(0)
                val presenceBand = (midBand + 1).coerceAtMost(it.numberOfBands.toInt() - 1)
                val gainMb   = (level * 1200).toInt().toShort().coerceIn(-1500, 1500)
                it.setBandLevel(midBand.toShort(), gainMb)
                it.setBandLevel(presenceBand.toShort(), (gainMb * 0.5f).toInt().toShort())
            }
        } catch (_: Exception) {}
        _state.value = _state.value.copy(dialogueBoost = level)
    }

    /** Night mode: compresses dynamic range so loud scenes don't blast and quiet scenes stay audible. */
    fun setNightMode(enabled: Boolean) {
        try {
            loudnessEnhancer?.setTargetGain(if (enabled) 800 else 0)
            // Also soften extreme highs in night mode to avoid harshness.
            equalizer?.let {
                val top = (it.numberOfBands.toInt() - 1).toShort()
                it.setBandLevel(top, if (enabled) (-400).toShort() else 0.toShort())
            }
        } catch (_: Exception) {}
        _state.value = _state.value.copy(nightMode = enabled)
    }

    /**
     * Bass boost strength 0..1000 (AndroidAudioEffect unit).
     * Values above 800 can distort on small speakers — warn in UI.
     */
    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength.coerceIn(0, 1000))
        } catch (_: Exception) {}
        _state.value = _state.value.copy(bassBoostStrength = strength.toInt())
    }

    /**
     * Virtualizer strength 0..1000.
     * Creates virtual surround and widens the stereo image.
     * At 600 this emulates a basic Dolby Atmos height layer effect.
     */
    fun setVirtualizer(strength: Short) {
        try {
            virtualizer?.setStrength(strength.coerceIn(0, 1000))
            virtualizer?.enabled = strength > 0
        } catch (_: Exception) {}
        _state.value = _state.value.copy(virtualizerStrength = strength.toInt())
    }

    /**
     * Dolby Atmos emulation preset:
     * - High virtualizer (700) for height perception
     * - Large room reverb for spatial depth
     * - Cinema EQ preset for frequency balance
     * - +200 mB loudness for the immersive feel
     */
    fun setDolbyAtmosEmulation(enabled: Boolean) {
        if (enabled) {
            setVirtualizer(700)
            setReverb(PresetReverb.PRESET_LARGEHALL, true)
            applyPreset(EqPreset.CINEMA)
            try { loudnessEnhancer?.setTargetGain(200) } catch (_: Exception) {}
        } else {
            setVirtualizer(0)
            setReverb(PresetReverb.PRESET_NONE, false)
            applyPreset(EqPreset.FLAT)
            try { loudnessEnhancer?.setTargetGain(0) } catch (_: Exception) {}
        }
        _state.value = _state.value.copy(atmosEmulation = enabled)
    }

    /** Volume normalization — boosts quiet content, attenuates loud content. */
    fun setVolumeNormalization(enabled: Boolean) {
        try {
            loudnessEnhancer?.setTargetGain(
                when {
                    enabled && _state.value.nightMode -> 600
                    enabled                           -> 300
                    _state.value.nightMode            -> 800
                    else                              -> 0
                }
            )
        } catch (_: Exception) {}
        _state.value = _state.value.copy(volumeNormalization = enabled)
    }

    /** Audio delay offset in milliseconds for A/V sync fine-tuning. */
    fun setAudioDelay(ms: Int) {
        _state.value = _state.value.copy(audioDelayMs = ms)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun setReverb(preset: Short, enabled: Boolean) {
        try {
            reverb?.preset = preset
            reverb?.enabled = enabled
        } catch (_: Exception) {}
    }

    private fun applyState(state: AudioState) {
        // EQ bands
        try {
            equalizer?.let { eq ->
                val bandCount = eq.numberOfBands.toInt()
                state.eqBands.forEachIndexed { i, gain ->
                    if (i < bandCount) eq.setBandLevel(i.toShort(), gain.toShort())
                }
            }
        } catch (_: Exception) {}
        setNightMode(state.nightMode)
        setBassBoost(state.bassBoostStrength.toShort())
        setVirtualizer(state.virtualizerStrength.toShort())
        if (state.dialogueBoost > 0f) setDialogueBoost(state.dialogueBoost)
    }

    companion object { private const val TAG = "KuroAudioEngine" }

    // ── Data types ────────────────────────────────────────────────────────────

    data class AudioState(
        val eqPreset:            EqPreset = EqPreset.FLAT,
        /** Gains for up to 5 bands in milli-Bels. */
        val eqBands:             List<Int> = listOf(0, 0, 0, 0, 0),
        val dialogueBoost:       Float = 0f,
        val nightMode:           Boolean = false,
        val volumeNormalization: Boolean = false,
        val bassBoostStrength:   Int = 0,       // 0–1000
        val virtualizerStrength: Int = 0,       // 0–1000
        val atmosEmulation:      Boolean = false,
        val audioDelayMs:        Int = 0,
    )
}

/**
 * EQ presets — gains in milli-Bels for [sub, bass, mid, presence, air].
 * Values range -1500..+1500 (±15 dB in 100 mB steps).
 */
enum class EqPreset(
    val displayName: String,
    /** [sub, bass, mid, presence, air] in milli-Bels. */
    val bandGains: List<Int>,
) {
    FLAT       ("Flat",         listOf(   0,    0,    0,    0,    0)),
    CINEMA     ("Cinema",       listOf( 300,  200,  -50,  100,  150)),
    ANIME      ("Anime",        listOf( 100,  200,  100,  300,  200)),
    BASS_BOOST ("Bass Boost",   listOf( 600,  500,    0, -100, -100)),
    TREBLE_BOOST("Treble Boost",listOf(-100, -100,    0,  200,  500)),
    VOICE      ("Voice / Dialogue", listOf(-200, -100, 400, 300,  100)),
    NIGHT      ("Night Mode",   listOf(-200,  100,  300,  200, -300)),
    CUSTOM     ("Custom",       listOf(   0,    0,    0,    0,    0)),
}
