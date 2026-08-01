package com.kurostream.app.ui.screens.extensions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.domain.extension.ExtensionRepository
import com.kurostream.domain.extension.UnifiedExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ExtensionsViewModel @Inject constructor(
    private val extensionRepository: ExtensionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtensionsUiState())
    val uiState: StateFlow<ExtensionsUiState> = _uiState.asStateFlow()

    init {
        loadExtensions()
    }

    private fun loadExtensions() {
        viewModelScope.launch {
            extensionRepository.observeAll().collect { extensions ->
                _uiState.update { state ->
                    state.copy(
                        extensions = extensions,
                        installedCount = extensions.count { it.isInstalled && it.isEnabled },
                    )
                }
            }
        }
    }

    fun toggleExtension(extensionId: String) {
        viewModelScope.launch {
            val ext = extensionRepository.getExtension(extensionId)
            if (ext?.isEnabled == true) {
                extensionRepository.disable(extensionId)
            } else {
                extensionRepository.enable(extensionId)
            }
        }
    }

    fun uninstallExtension(extensionId: String) {
        viewModelScope.launch {
            extensionRepository.uninstall(extensionId)
        }
    }

    fun configureExtension(extensionId: String) {
        // Navigate to config screen
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdates = true) }
            try {
                val updates = extensionRepository.checkForUpdates()
                _uiState.update { it.copy(availableUpdates = updates.size, isCheckingUpdates = false) }
            } catch (e: Exception) {
                Timber.e(e, "Update check failed")
                _uiState.update { it.copy(isCheckingUpdates = false) }
            }
        }
    }
}

@Immutable
data class ExtensionsUiState(
    val extensions: List<UnifiedExtension> = emptyList(),
    val installedCount: Int = 0,
    val availableUpdates: Int = 0,
    val isCheckingUpdates: Boolean = false,
)