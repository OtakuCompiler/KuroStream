package com.kurostream.torrent.network

import timber.log.Timber
import timber.log.Timber
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortMappingMonitor @Inject constructor() {

    private val TAG = "PortMappingMonitor"

    data class PortMapping(
        val externalPort: Int,
        val protocol: String,
        val isSuccessful: Boolean,
        val errorMessage: String? = null,
    )

    private val _mappingState = MutableStateFlow<PortMapping?>(null)
    val mappingState: StateFlow<PortMapping?> = _mappingState.asStateFlow()

    private val _isUpnpEnabled = MutableStateFlow(true)
    val isUpnpEnabled: StateFlow<Boolean> = _isUpnpEnabled.asStateFlow()

    private val _isNatpmpEnabled = MutableStateFlow(true)
    val isNatpmpEnabled: StateFlow<Boolean> = _isNatpmpEnabled.asStateFlow()

    fun onPortMappingAlert(externalPort: Int, protocol: String, errorMessage: String?) {
        val mapping = PortMapping(
            externalPort = externalPort,
            protocol = protocol,
            isSuccessful = errorMessage == null,
            errorMessage = errorMessage,
        )
        _mappingState.value = mapping
        if (errorMessage == null) {
            Timber.tag(TAG).i(, "Port mapping successful: $protocol:$externalPort")
        } else {
            Timber.tag(TAG).w(, "Port mapping failed: $protocol - $errorMessage")
        }
    }

    fun onMapPortAlert(success: Boolean, port: Int) {
        if (success) {
            _mappingState.value = PortMapping(port, "TCP/UDP", true)
            Timber.tag(TAG).i(, "UPnP/NAT-PMP port mapping successful on port $port")
        } else {
            Timber.tag(TAG).w(, "UPnP/NAT-PMP port mapping failed on port $port")
        }
    }

    fun getMappingStatusSummary(): String {
        val state = _mappingState.value
        return when {
            state == null -> "Not mapped"
            state.isSuccessful -> "Mapped: ${state.protocol}:${state.externalPort}"
            else -> "Failed: ${state.errorMessage ?: "Unknown error"}"
        }
    }
}
