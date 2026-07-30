// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream. If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setHighContrastEnabled(enabled)
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setReduceMotionEnabled(enabled)
                _uiState.update { it.copy(reduceMotionEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setFocusHighlightEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setFocusHighlightEnabled(enabled)
                _uiState.update { it.copy(focusHighlightEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSkinName(name: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setSkinName(name)
                _uiState.update { it.copy(skinName = name) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockEnabled(enabled)
                _uiState.update { it.copy(sourceLockEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockFallbackMode(mode: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockFallbackMode(mode)
                _uiState.update { it.copy(sourceLockFallbackMode = mode) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockMaxRetries(retries: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockMaxRetries(retries)
                _uiState.update { it.copy(sourceLockMaxRetries = retries) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockRetryDelayMs(delay: Long) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockRetryDelayMs(delay)
                _uiState.update { it.copy(sourceLockRetryDelayMs = delay) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockPersist(persist: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockPersist(persist)
                _uiState.update { it.copy(sourceLockPersist = persist) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSourceLockNotifyFallback(notify: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSourceLockNotifyFallback(notify)
                _uiState.update { it.copy(sourceLockNotifyFallback = notify) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun clearAllSourceLocks() {
        viewModelScope.launch {
            try {
                settingsRepository.clearAllSourceLocks()
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setAutoPlayNextEnabled(enabled)
                _uiState.update { it.copy(autoPlayNextEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setSkipIntroEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSkipIntroEnabled(enabled)
                _uiState.update { it.copy(skipIntroEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setHardwareAccelerationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setHardwareAccelerationEnabled(enabled)
                _uiState.update { it.copy(hardwareAccelerationEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setBackgroundPlaybackEnabled(enabled)
                _uiState.update { it.copy(backgroundPlaybackEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setCinematicModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(cinematicModeEnabled = enabled) }
    }

    fun setAmbientModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(ambientModeEnabled = enabled) }
    }

    fun setOfflineTranslationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(offlineTranslationEnabled = enabled) }
    }

    fun setPredictivePrecacheEnabled(enabled: Boolean) {
        _uiState.update { it.copy(predictivePrecacheEnabled = enabled) }
    }

    fun setAiUpscalingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setAiUpscalingEnabled(enabled)
                _uiState.update { it.copy(aiUpscalingEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setFrameInterpolationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setFrameInterpolationEnabled(enabled)
                _uiState.update { it.copy(frameInterpolationEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setLowLatencyUpscalingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setLowLatencyUpscalingEnabled(enabled)
                _uiState.update { it.copy(lowLatencyUpscalingEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setVodCacheCompressionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setVodCacheCompressionEnabled(enabled)
                _uiState.update { it.copy(vodCacheCompressionEnabled = enabled) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setDiskBufferSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setDiskBufferSizeMb(sizeMb)
                _uiState.update { it.copy(diskBufferSizeMb = sizeMb) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setDiskBufferReadAheadMb(sizeMb: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setDiskBufferReadAheadMb(sizeMb)
                _uiState.update { it.copy(diskBufferReadAheadMb = sizeMb) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setDiskBufferLocation(location: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setDiskBufferLocation(location)
                _uiState.update { it.copy(diskBufferLocation = location) }
            } catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }

    fun setDiskBufferDeleteOnShutdown(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setDiskBufferDeleteOnShutdown(enabled)
                _uiState.update { it.copy(diskBufferDeleteOnShutdown = enabled) }
            }             catch (e: Exception) {
                /* ignore settings error */
            }
        }
    }
}

data class SettingsUiState(
    val sourceLockEnabled: Boolean = true,
    val sourceLockFallbackMode: Int = 0,
    val sourceLockMaxRetries: Int = 2,
    val sourceLockRetryDelayMs: Long = 3000,
    val sourceLockPersist: Boolean = true,
    val sourceLockNotifyFallback: Boolean = true,
    val autoPlayNextEnabled: Boolean = true,
    val skipIntroEnabled: Boolean = true,
    val hardwareAccelerationEnabled: Boolean = true,
    val backgroundPlaybackEnabled: Boolean = false,
    val cinematicModeEnabled: Boolean = false,
    val ambientModeEnabled: Boolean = false,
    val offlineTranslationEnabled: Boolean = false,
    val predictivePrecacheEnabled: Boolean = true,
    val aiUpscalingEnabled: Boolean = false,
    val frameInterpolationEnabled: Boolean = false,
    val diskBufferSizeMb: Int = 200,
    val diskBufferReadAheadMb: Int = 4,
    val diskBufferLocation: String = "internal",
    val diskBufferDeleteOnShutdown: Boolean = false,
    val lowLatencyUpscalingEnabled: Boolean = false,
    val vodCacheCompressionEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false,
    val focusHighlightEnabled: Boolean = true,
    val skinName: String = "ARCTIC_FUSE",
)
