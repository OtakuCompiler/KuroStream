package com.kurostream.app.ui.screens.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.domain.extension.ExtensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionConfigViewModel @Inject constructor(
    private val extensionRepository: ExtensionRepository,
) : ViewModel() {

    private val _configStates = mutableMapOf<String, MutableStateFlow<ExtensionConfigUiState>>()

    fun getConfigState(extensionId: String): StateFlow<ExtensionConfigUiState> {
        return _configStates.getOrPut(extensionId) {
            MutableStateFlow(ExtensionConfigUiState()).also { loadConfig(extensionId, it) }
        }
    }

    private fun loadConfig(extensionId: String, stateFlow: MutableStateFlow<ExtensionConfigUiState>) {
        viewModelScope.launch {
            val ext = extensionRepository.getExtension(extensionId)
            val config = extensionRepository.getConfig(extensionId)
            stateFlow.update {
                ExtensionConfigUiState(
                    extensionName = ext?.name ?: "",
                    fields = ext?.configSchema ?: emptyList(),
                    currentValues = config,
                )
            }
        }
    }

    fun saveConfig(extensionId: String, values: Map<String, String>) {
        viewModelScope.launch {
            extensionRepository.setConfig(extensionId, values)
        }
    }
}
