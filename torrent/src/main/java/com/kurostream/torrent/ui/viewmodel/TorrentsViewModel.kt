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

package com.kurostream.torrent.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.common.result.Result
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.repository.TorrentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sharingStarted
import javax.inject.Inject

@HiltViewModel
class TorrentsViewModel @Inject constructor(
    private val repository: TorrentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorrentsUiState())
    val uiState = _uiState.asStateFlow()

    private val _sessionSettings = MutableStateFlow<TorrentSessionSettings?>(null)
    val sessionSettings = _sessionSettings.asStateFlow()

    init {
        loadSessionSettings()
        observeTorrents()
    }

    private fun loadSessionSettings() {
        viewModelScope.launch {
            _sessionSettings.value = repository.getSessionSettings()
        }
    }

    private fun observeTorrents() {
        viewModelScope.launch {
            combine(
                repository.observeTorrents(),
                repository.observeGlobalStats()
            ) { torrents, stats ->
                _uiState.update { it.copy(torrents = torrents, globalStats = stats) }
            }.stateIn(viewModelScope.coroutineContext, sharingStarted.WhileSubscribed(), Pair(emptyList(), GlobalStats(0L, 0L, 0, 0, 0, 0)))
        }
    }

    fun addMagnet(magnetUri: String, downloadDir: String, sequential: Boolean = false) {
        viewModelScope.launch {
            val result = repository.addTorrent(magnetUri, downloadDir, sequential)
            when (result) {
                is TorrentResult.Success -> {
                    dismissAddTorrentDialog()
                }
                is TorrentResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun addTorrentFile(filePath: String, downloadDir: String, sequential: Boolean = false) {
        viewModelScope.launch {
            val result = repository.addTorrentFile(filePath, downloadDir, sequential)
            when (result) {
                is TorrentResult.Success -> {
                    dismissAddTorrentDialog()
                }
                is TorrentResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun pauseTorrent(infoHash: String) {
        viewModelScope.launch { repository.pauseTorrent(infoHash) }
    }

    fun resumeTorrent(infoHash: String) {
        viewModelScope.launch { repository.resumeTorrent(infoHash) }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        viewModelScope.launch { repository.removeTorrent(infoHash, deleteFiles) }
    }

    fun setTorrentPriority(infoHash: String, priority: Int) {
        viewModelScope.launch { repository.setTorrentPriority(infoHash, priority) }
    }

    fun setFilePriority(infoHash: String, filePath: String, priority: FilePriority) {
        viewModelScope.launch {
            val priorities = mapOf(filePath to priority)
            repository.setFilePriorities(infoHash, priorities)
        }
    }

    fun setSequentialDownload(infoHash: String, enabled: Boolean) {
        viewModelScope.launch { repository.setSequentialDownload(infoHash, enabled) }
    }

    fun setTorrentSpeedLimits(infoHash: String, downloadLimit: Long, uploadLimit: Long) {
        viewModelScope.launch {
            // TODO: Implement per-torrent speed limits
        }
    }

    fun showAddTorrentDialog() {
        _uiState.update { it.copy(showAddTorrentDialog = true) }
    }

    fun dismissAddTorrentDialog() {
        _uiState.update { it.copy(showAddTorrentDialog = false, errorMessage = null) }
    }

    fun showSettingsDialog() {
        viewModelScope.launch {
            _sessionSettings.value = repository.getSessionSettings()
            _uiState.update { it.copy(showSettingsDialog = true) }
        }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun updateSessionSettings(settings: TorrentSessionSettings) {
        viewModelScope.launch {
            repository.setSessionSettings(settings)
            _sessionSettings.value = settings
        }
    }

    fun showFilesDialog(torrent: TorrentInfo) {
        _uiState.update { it.copy(filesDialogTorrent = torrent) }
    }

    fun dismissFilesDialog() {
        _uiState.update { it.copy(filesDialogTorrent = null) }
    }

    fun showSpeedLimitsDialog(torrent: TorrentInfo) {
        _uiState.update { it.copy(speedLimitsTorrent = torrent) }
    }

    fun dismissSpeedLimitsDialog() {
        _uiState.update { it.copy(speedLimitsTorrent = null) }
    }

    fun openStreamPlayer(torrent: TorrentInfo) {
        _uiState.update { it.copy(streamTorrent = torrent) }
    }

    fun dismissStreamPlayer() {
        _uiState.update { it.copy(streamTorrent = null) }
    }
}

data class TorrentsUiState(
    val torrents: List<TorrentInfo> = emptyList(),
    val globalStats: GlobalStats? = null,
    val sessionSettings: TorrentSessionSettings? = null,
    val showAddTorrentDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val filesDialogTorrent: TorrentInfo? = null,
    val speedLimitsTorrent: TorrentInfo? = null,
    val streamTorrent: TorrentInfo? = null,
    val errorMessage: String? = null,
)