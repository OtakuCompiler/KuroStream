package com.kurostream.legacyui.anistream.ui.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.addons.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonManagerViewModel @Inject constructor(
    private val addonRepository: AddonRepository
) : ViewModel() {

    private val _addons = MutableStateFlow<List<Addon>>(emptyList())
    val addons: StateFlow<List<Addon>> = _addons.asStateFlow()

    private val _events = MutableSharedFlow<AddonEvent>()
    val events: SharedFlow<AddonEvent> = _events.asSharedFlow()

    init {
        loadAddons()
    }

    private fun loadAddons() {
        viewModelScope.launch {
            addonRepository.getInstalledAddons()
                .collectLatest { _addons.value = it }
        }
    }

    fun toggleAddon(addonId: String) {
        viewModelScope.launch {
            try {
                addonRepository.toggleEnabled(addonId)
                val addon = _addons.value.find { it.id == addonId }
                val status = if (addon?.isEnabled == true) "disabled" else "enabled"
                _events.emit(AddonEvent.ShowMessage("Extension $status"))
            } catch (e: Exception) {
                _events.emit(AddonEvent.ShowError("Failed to toggle extension"))
            }
        }
    }

    fun uninstallAddon(addonId: String) {
        viewModelScope.launch {
            try {
                addonRepository.uninstallAddon(addonId)
                _events.emit(AddonEvent.ShowMessage("Extension uninstalled"))
            } catch (e: Exception) {
                _events.emit(AddonEvent.ShowError("Failed to uninstall extension"))
            }
        }
    }
}

sealed class AddonEvent {
    data class ShowError(val message: String) : AddonEvent()
    data class ShowMessage(val message: String) : AddonEvent()
}
