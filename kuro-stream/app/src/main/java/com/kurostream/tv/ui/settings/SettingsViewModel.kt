package com.kurostream.tv.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.BuildConfig
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.domain.model.StreamQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Subtitle size options.
 */
enum class SubtitleSize(val displayName: String, val scaleFactor: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f),
    EXTRA_LARGE("Extra Large", 1.5f)
}

/**
 * Video quality with display name.
 */
val StreamQuality.displayName: String
    get() = when (this) {
        StreamQuality.AUTO -> "Auto"
        StreamQuality.P360 -> "360p"
        StreamQuality.P480 -> "480p"
        StreamQuality.P720 -> "720p (Recommended)"
        StreamQuality.P1080 -> "1080p"
        StreamQuality.P1440 -> "1440p"
        StreamQuality.P2160 -> "4K"
    }

/**
 * UI State for Settings Screen.
 */
data class SettingsUiState(
    // Playback
    val preferredQuality: StreamQuality = StreamQuality.P720,
    val autoPlayNext: Boolean = true,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    
    // Subtitles
    val preferredSubtitleLanguage: String = "English",
    val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    val subtitleBackground: Boolean = true,
    
    // Appearance
    val preferEnglishTitles: Boolean = true,
    
    // Security
    val pinEnabled: Boolean = false,
    
    // Sync
    val anilistUsername: String? = null,
    val autoSyncEnabled: Boolean = false,
    
    // Storage
    val imageCacheSize: String = "Calculating...",
    val videoCacheSize: String = "Calculating...",
    
    // About
    val appVersion: String = BuildConfig.VERSION_NAME,
    val buildNumber: String = BuildConfig.VERSION_CODE.toString()
)

/**
 * ViewModel for Settings Screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Preference keys
    private object PreferenceKeys {
        val PREFERRED_QUALITY = intPreferencesKey("preferred_quality")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val AUTO_SKIP_OUTRO = booleanPreferencesKey("auto_skip_outro")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_SIZE = intPreferencesKey("subtitle_size")
        val SUBTITLE_BACKGROUND = booleanPreferencesKey("subtitle_background")
        val PREFER_ENGLISH_TITLES = booleanPreferencesKey("prefer_english_titles")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val ANILIST_USERNAME = stringPreferencesKey("anilist_username")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    }

    init {
        loadSettings()
        calculateCacheSizes()
    }

    private fun loadSettings() {
        viewModelScope.launch(ioDispatcher) {
            dataStore.data.collect { preferences ->
                _uiState.update { state ->
                    state.copy(
                        preferredQuality = StreamQuality.entries.getOrElse(
                            preferences[PreferenceKeys.PREFERRED_QUALITY] ?: 2
                        ) { StreamQuality.P720 },
                        autoPlayNext = preferences[PreferenceKeys.AUTO_PLAY_NEXT] ?: true,
                        autoSkipIntro = preferences[PreferenceKeys.AUTO_SKIP_INTRO] ?: false,
                        autoSkipOutro = preferences[PreferenceKeys.AUTO_SKIP_OUTRO] ?: false,
                        preferredSubtitleLanguage = preferences[PreferenceKeys.SUBTITLE_LANGUAGE] ?: "English",
                        subtitleSize = SubtitleSize.entries.getOrElse(
                            preferences[PreferenceKeys.SUBTITLE_SIZE] ?: 1
                        ) { SubtitleSize.MEDIUM },
                        subtitleBackground = preferences[PreferenceKeys.SUBTITLE_BACKGROUND] ?: true,
                        preferEnglishTitles = preferences[PreferenceKeys.PREFER_ENGLISH_TITLES] ?: true,
                        pinEnabled = preferences[PreferenceKeys.PIN_ENABLED] ?: false,
                        anilistUsername = preferences[PreferenceKeys.ANILIST_USERNAME],
                        autoSyncEnabled = preferences[PreferenceKeys.AUTO_SYNC_ENABLED] ?: false
                    )
                }
            }
        }
    }

    private fun calculateCacheSizes() {
        viewModelScope.launch(ioDispatcher) {
            // TODO: Calculate actual cache sizes
            _uiState.update { 
                it.copy(
                    imageCacheSize = "50 MB",
                    videoCacheSize = "120 MB"
                )
            }
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.AUTO_PLAY_NEXT] = enabled
            }
        }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.AUTO_SKIP_INTRO] = enabled
            }
        }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.AUTO_SKIP_OUTRO] = enabled
            }
        }
    }

    fun setSubtitleBackground(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.SUBTITLE_BACKGROUND] = enabled
            }
        }
    }

    fun setPreferEnglishTitles(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.PREFER_ENGLISH_TITLES] = enabled
            }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.AUTO_SYNC_ENABLED] = enabled
            }
        }
    }

    fun showQualityPicker() {
        // TODO: Show quality picker dialog
    }

    fun showSubtitleLanguagePicker() {
        // TODO: Show subtitle language picker dialog
    }

    fun showSubtitleSizePicker() {
        // TODO: Show subtitle size picker dialog
    }

    fun showAniListLogin() {
        // TODO: Start AniList OAuth flow
    }

    fun clearCache() {
        viewModelScope.launch(ioDispatcher) {
            // TODO: Clear image and video cache
            calculateCacheSizes()
        }
    }
}
