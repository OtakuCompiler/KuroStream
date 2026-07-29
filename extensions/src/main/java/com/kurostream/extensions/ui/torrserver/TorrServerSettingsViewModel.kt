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

package com.kurostream.extensions.ui.torrserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.extensions.torrserver.TorrServerConfig
import com.kurostream.extensions.torrserver.TorrServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrServerSettingsViewModel @Inject constructor(
    private val torrServerConfig: TorrServerConfig,
    private val repository: TorrServerRepository
) : ViewModel() {

    private val _config = MutableStateFlow(ConfigUiState())
    val config: StateFlow<ConfigUiState> = _config.asStateFlow()

    init {
        _config.value = ConfigUiState(
            serverUrl = torrServerConfig.serverUrl,
            cacheSize = torrServerConfig.cacheSize,
            preloadBuffer = torrServerConfig.preloadBuffer,
            connectionsLimit = torrServerConfig.connectionsLimit,
            useDiskCache = torrServerConfig.useDiskCache,
            removeAfterStop = torrServerConfig.removeAfterStop
        )
    }

    fun updateServerUrl(url: String) {
        _config.value = _config.value.copy(serverUrl = url)
    }

    fun updateCacheSize(size: Int) {
        _config.value = _config.value.copy(cacheSize = size)
    }

    fun updatePreloadBuffer(buffer: Int) {
        _config.value = _config.value.copy(preloadBuffer = buffer)
    }

    fun updateConnectionsLimit(limit: Int) {
        _config.value = _config.value.copy(connectionsLimit = limit)
    }

    fun updateUseDiskCache(use: Boolean) {
        _config.value = _config.value.copy(useDiskCache = use)
    }

    fun updateRemoveAfterStop(remove: Boolean) {
        _config.value = _config.value.copy(removeAfterStop = remove)
    }

    fun saveSettings() {
        val state = _config.value
        torrServerConfig.serverUrl = state.serverUrl
        torrServerConfig.cacheSize = state.cacheSize
        torrServerConfig.preloadBuffer = state.preloadBuffer
        torrServerConfig.connectionsLimit = state.connectionsLimit
        torrServerConfig.useDiskCache = state.useDiskCache
        torrServerConfig.removeAfterStop = state.removeAfterStop
        viewModelScope.launch {
            repository.updateSettings()
        }
    }

    data class ConfigUiState(
        val serverUrl: String = "",
        val cacheSize: Int = 200,
        val preloadBuffer: Int = 32,
        val connectionsLimit: Int = 50,
        val useDiskCache: Boolean = true,
        val removeAfterStop: Boolean = false
    )
}