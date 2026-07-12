package com.kurostream.players.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioProfileManager @Inject constructor() {
    private val _currentProfile = MutableStateFlow<AudioProfile?>(null)
    val currentProfile: StateFlow<AudioProfile?> = _currentProfile.asStateFlow()

    private val availableProfiles = listOf(
        AudioProfile("default", "Default", FloatArray(10), 0, false, 0f),
        AudioProfile("night", "Night Mode", floatArrayOf(2f, 1f, 0f, -1f, -2f, -2f, -1f, 0f, 1f, 2f), 0, true, 0.8f),
        AudioProfile("cinema", "Cinema", floatArrayOf(3f, 2f, 1f, 0f, -1f, -1f, 0f, 1f, 2f, 3f), 20, false, 0.3f),
        AudioProfile("music", "Music", floatArrayOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f), 0, false, 0.1f),
        AudioProfile("dialog", "Dialog Enhancement", floatArrayOf(-2f, -1f, 1f, 3f, 4f, 4f, 3f, 1f, -1f, -2f), 0, false, 0.5f),
    )

    fun getAvailableProfiles(): List<AudioProfile> = availableProfiles

    fun setProfile(profileId: String) {
        _currentProfile.value = availableProfiles.find { it.id == profileId }
    }

    fun setCustomAudioDelay(delayMs: Int) {
        _currentProfile.update { it?.copy(audioDelayMs = delayMs) }
    }

    fun setNightMode(enabled: Boolean) {
        _currentProfile.update { it?.copy(nightMode = enabled, drcStrength = if (enabled) 0.8f else 0f) }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        _currentProfile.update { old ->
            old?.let {
                val newGains = it.bandGains.copyOf()
                newGains[bandIndex.coerceIn(0, 9)] = gainDb.coerceIn(-10f, 10f)
                it.copy(bandGains = newGains)
            }
        }
    }
}

data class AudioProfile(
    val id: String,
    val name: String,
    val bandGains: FloatArray,
    val audioDelayMs: Int,
    val nightMode: Boolean,
    val drcStrength: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioProfile) return false
        return id == other.id && name == other.name && audioDelayMs == other.audioDelayMs &&
            nightMode == other.nightMode && drcStrength == other.drcStrength &&
            bandGains.contentEquals(other.bandGains)
    }

    override fun hashCode(): Int {
        return listOf(id, name, bandGains.contentHashCode(), audioDelayMs, nightMode, drcStrength).hashCode()
    }
}