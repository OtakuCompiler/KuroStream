package com.kurostream.legacyui.anistream.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<List<SettingItem>>(emptyList())
    val settings: StateFlow<List<SettingItem>> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val items = buildSettingsList()
            _settings.value = items
        }
    }

    private suspend fun buildSettingsList(): List<SettingItem> {
        val currentSettings = settingsRepository.getAllSettings()

        return listOf(
            SettingItem.Header("Appearance"),
            SettingItem.ThemeSelector(
                key = "theme",
                title = "Theme",
                subtitle = currentSettings["theme"] ?: "Dark",
                currentTheme = currentSettings["theme"] ?: "dark"
            ),
            SettingItem.Toggle(
                key = "animations",
                title = "Enable Animations",
                subtitle = "Smooth transitions and effects",
                isChecked = currentSettings["animations"]?.toBoolean() ?: true
            ),

            SettingItem.Header("Subtitles"),
            SettingItem.Navigate(
                key = "subtitle_settings",
                title = "Subtitle Preferences",
                subtitle = "Font, size, color, language"
            ),

            SettingItem.Header("Playback"),
            SettingItem.Navigate(
                key = "playback_defaults",
                title = "Playback Defaults",
                subtitle = "Auto-play, skip intro, quality"
            ),
            SettingItem.Toggle(
                key = "auto_play_next",
                title = "Auto-play Next Episode",
                subtitle = "Automatically play the next episode",
                isChecked = currentSettings["auto_play_next"]?.toBoolean() ?: true
            ),
            SettingItem.Toggle(
                key = "skip_intro",
                title = "Skip Intro",
                subtitle = "Automatically skip opening sequences",
                isChecked = currentSettings["skip_intro"]?.toBoolean() ?: true
            ),

            SettingItem.Header("Sync"),
            SettingItem.Navigate(
                key = "sync_settings",
                title = "Sync Settings",
                subtitle = "MAL, AniList, cloud backup"
            ),

            SettingItem.Header("Data"),
            SettingItem.Navigate(
                key = "backup_restore",
                title = "Backup & Restore",
                subtitle = "Export or import your data"
            ),
            SettingItem.Action(
                key = "clear_cache",
                title = "Clear Cache",
                subtitle = "Free up storage space"
            ),

            SettingItem.Header("System"),
            SettingItem.Navigate(
                key = "performance",
                title = "Performance",
                subtitle = "Buffer, decoder, cache settings"
            ),
            SettingItem.Action(
                key = "check_updates",
                title = "Check for Updates",
                subtitle = "Current version: 1.0.0"
            ),
            SettingItem.Navigate(
                key = "about",
                title = "About",
                subtitle = "Version, licenses, credits"
            )
        )
    }

    fun updateToggle(key: String, value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSetting(key, value.toString())
            loadSettings()
        }
    }

    fun updateValue(key: String, value: String) {
        viewModelScope.launch {
            settingsRepository.setSetting(key, value)
            loadSettings()
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setSetting("theme", theme)
            loadSettings()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            settingsRepository.clearCache()
            loadSettings()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            // Check for app updates
        }
    }
}
