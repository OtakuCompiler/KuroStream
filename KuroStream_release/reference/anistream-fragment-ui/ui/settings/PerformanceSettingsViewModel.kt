package com.kurostream.legacyui.anistream.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(PerformanceSettings())
    val settings: StateFlow<PerformanceSettings> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _settings.value = PerformanceSettings(
                bufferSizeIndex = settingsRepository.getInt("buffer_size", 0),
                decoderPreference = DecoderPreference.valueOf(
                    settingsRepository.getSetting("decoder") ?: "AUTO"
                ),
                coilCacheIndex = settingsRepository.getInt("coil_cache", 1),
                animationSpeedIndex = settingsRepository.getInt("animation_speed", 2)
            )
        }
    }

    fun setBufferSize(index: Int) {
        viewModelScope.launch {
            settingsRepository.setInt("buffer_size", index)
            _settings.value = _settings.value.copy(bufferSizeIndex = index)
        }
    }

    fun setDecoderPreference(preference: DecoderPreference) {
        viewModelScope.launch {
            settingsRepository.setSetting("decoder", preference.name)
            _settings.value = _settings.value.copy(decoderPreference = preference)
        }
    }

    fun setCoilCacheSize(index: Int) {
        viewModelScope.launch {
            settingsRepository.setInt("coil_cache", index)
            _settings.value = _settings.value.copy(coilCacheIndex = index)
        }
    }

    fun setAnimationSpeed(index: Int) {
        viewModelScope.launch {
            settingsRepository.setInt("animation_speed", index)
            _settings.value = _settings.value.copy(animationSpeedIndex = index)
        }
    }
}

data class PerformanceSettings(
    val bufferSizeIndex: Int = 0,
    val decoderPreference: DecoderPreference = DecoderPreference.AUTO,
    val coilCacheIndex: Int = 1,
    val animationSpeedIndex: Int = 2
)

enum class DecoderPreference {
    AUTO, HARDWARE, SOFTWARE
}
