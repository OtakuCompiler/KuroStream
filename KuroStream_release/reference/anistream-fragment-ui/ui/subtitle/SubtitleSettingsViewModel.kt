package com.kurostream.legacyui.anistream.ui.subtitle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun setFontSize(sizeSp: Int) {
        viewModelScope.launch {
            settingsRepository.setInt("subtitle_font_size", sizeSp)
        }
    }

    fun setBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.setSetting("subtitle_bg_opacity", opacity.toString())
        }
    }

    fun setAutoLoad(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean("subtitle_auto_load", enabled)
        }
    }

    fun setPreferredLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setSetting("subtitle_language", language)
        }
    }
}
