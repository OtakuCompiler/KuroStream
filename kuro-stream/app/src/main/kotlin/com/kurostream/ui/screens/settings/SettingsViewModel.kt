package com.kurostream.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.player.MemoryManager
import com.kurostream.core.plugin.PluginManager
import com.kurostream.data.model.Plugin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val plugins: List<Plugin> = emptyList(),
    val defaultResolution: String = "1080p",
    val memoryStatus: String = "Unknown",
    val addError: String? = null,
    val isAddingPlugin: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val memoryManager: MemoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val resolutionCycle = listOf("Auto", "4K (2160p)", "1080p", "720p", "480p")
    private var resolutionIndex = 2 // default: 1080p

    init {
        observePlugins()
        updateMemoryStatus()
    }

    private fun observePlugins() {
        viewModelScope.launch {
            pluginManager.activePlugins.collect { plugins ->
                _uiState.update { it.copy(plugins = plugins) }
            }
        }
    }

    private fun updateMemoryStatus() {
        val pressure = memoryManager.memoryPressureLevel
        val available = memoryManager.availableRamMb
        _uiState.update {
            it.copy(memoryStatus = "${available}MB free | $pressure")
        }
    }

    fun addPlugin(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingPlugin = true, addError = null) }
            val result = pluginManager.addPlugin(url)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isAddingPlugin = false, addError = null) }
                    onResult(true)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isAddingPlugin = false, addError = "Failed: ${e.message}")
                    }
                    onResult(false)
                }
            )
        }
    }

    fun removePlugin(pluginId: String) {
        viewModelScope.launch {
            pluginManager.removePlugin(pluginId)
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            pluginManager.togglePlugin(pluginId, enabled)
        }
    }

    fun cycleResolution() {
        resolutionIndex = (resolutionIndex + 1) % resolutionCycle.size
        _uiState.update { it.copy(defaultResolution = resolutionCycle[resolutionIndex]) }
    }
}
