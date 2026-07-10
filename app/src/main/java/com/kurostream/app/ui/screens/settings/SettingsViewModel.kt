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

package com.kurostream.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun setSourceLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSourceLockEnabled(enabled)
            _uiState.update { it.copy(sourceLockEnabled = enabled) }
        }
    }

    fun setSourceLockFallbackMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setSourceLockFallbackMode(mode)
            _uiState.update { it.copy(sourceLockFallbackMode = mode) }
        }
    }

    fun setSourceLockMaxRetries(retries: Int) {
        viewModelScope.launch {
            settingsRepository.setSourceLockMaxRetries(retries)
            _uiState.update { it.copy(sourceLockMaxRetries = retries) }
        }
    }

    fun setSourceLockRetryDelayMs(delay: Long) {
        viewModelScope.launch {
            settingsRepository.setSourceLockRetryDelayMs(delay)
            _uiState.update { it.copy(sourceLockRetryDelayMs = delay) }
        }
    }

    fun setSourceLockPersist(persist: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSourceLockPersist(persist)
            _uiState.update { it.copy(sourceLockPersist = persist) }
        }
    }

    fun setSourceLockNotifyFallback(notify: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSourceLockNotifyFallback(notify)
            _uiState.update { it.copy(sourceLockNotifyFallback = notify) }
        }
    }

    fun clearAllSourceLocks() {
        viewModelScope.launch {
            settingsRepository.clearAllSourceLocks()
        }
    }

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayNextEnabled(enabled)
            _uiState.update { it.copy(autoPlayNextEnabled = enabled) }
        }
    }

    fun setSkipIntroEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSkipIntroEnabled(enabled)
            _uiState.update { it.copy(skipIntroEnabled = enabled) }
        }
    }

    fun setHardwareAccelerationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHardwareAccelerationEnabled(enabled)
            _uiState.update { it.copy(hardwareAccelerationEnabled = enabled) }
        }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackgroundPlaybackEnabled(enabled)
            _uiState.update { it.copy(backgroundPlaybackEnabled = enabled) }
        }
    }

    fun setCinematicModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCinematicModeEnabled(enabled)
            _uiState.update { it.copy(cinematicModeEnabled = enabled) }
        }
    }

    fun setAmbientModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAmbientModeEnabled(enabled)
            _uiState.update { it.copy(ambientModeEnabled = enabled) }
        }
    }

    fun setOfflineTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOfflineTranslationEnabled(enabled)
            _uiState.update { it.copy(offlineTranslationEnabled = enabled) }
        }
    }

    fun setPredictivePrecacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPredictivePrecacheEnabled(enabled)
            _uiState.update { it.copy(predictivePrecacheEnabled = enabled) }
        }
    }

    fun setAiUpscalingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiUpscalingEnabled(enabled)
            _uiState.update { it.copy(aiUpscalingEnabled = enabled) }
        }
    }

    fun setFrameInterpolationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFrameInterpolationEnabled(enabled)
            _uiState.update { it.copy(frameInterpolationEnabled = enabled) }
        }
    }

    fun setDiskBufferSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            settingsRepository.setDiskBufferSizeMb(sizeMb)
            _uiState.update { it.copy(diskBufferSizeMb = sizeMb) }
        }
    }

    fun setDiskBufferReadAheadMb(sizeMb: Int) {
        viewModelScope.launch {
            settingsRepository.setDiskBufferReadAheadMb(sizeMb)
            _uiState.update { it.copy(diskBufferReadAheadMb = sizeMb) }
        }
    }

    fun setDiskBufferLocation(location: String) {
        viewModelScope.launch {
            settingsRepository.setDiskBufferLocation(location)
            _uiState.update { it.copy(diskBufferLocation = location) }
        }
    }

    fun setDiskBufferDeleteOnShutdown(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDiskBufferDeleteOnShutdown(enabled)
            _uiState.update { it.copy(diskBufferDeleteOnShutdown = enabled) }
        }
    }

    // Torrent settings
    fun setSeedWhileIdleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSeedWhileIdleEnabled(enabled)
            _uiState.update { it.copy(seedWhileIdleEnabled = enabled) }
        }
    }

    fun setSequentialDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSequentialDownloadEnabled(enabled)
            _uiState.update { it.copy(sequentialDownloadEnabled = enabled) }
        }
    }

    fun setSeedRatioLimit(limit: Float) {
        viewModelScope.launch {
            settingsRepository.setSeedRatioLimit(limit)
            _uiState.update { it.copy(seedRatioLimit = limit) }
        }
    }

    fun setGlobalDownloadLimit(kbps: Long) {
        viewModelScope.launch {
            settingsRepository.setGlobalDownloadLimit(kbps)
            _uiState.update { it.copy(globalDownloadLimitKbps = kbps) }
        }
    }

    fun setGlobalUploadLimit(kbps: Long) {
        viewModelScope.launch {
            settingsRepository.setGlobalUploadLimit(kbps)
            _uiState.update { it.copy(globalUploadLimitKbps = kbps) }
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
    // Disk Buffer Settings
    val diskBufferSizeMb: Int = 200,
    val diskBufferReadAheadMb: Int = 4,
    val diskBufferLocation: String = "internal",
    val diskBufferDeleteOnShutdown: Boolean = false,
    // Torrent Settings
    val seedWhileIdleEnabled: Boolean = true,
    val sequentialDownloadEnabled: Boolean = true,
    val seedRatioLimit: Float = 2.0f,
    val globalDownloadLimitKbps: Long = -1,
    val globalUploadLimitKbps: Long = -1,
)