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

package com.kurostream.torrent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.common.result.Result
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.repository.TorrentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sharingStarted
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val repository: TorrentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorrentUiState())
    val uiState = _uiState

    private val _torrents = repository.observeTorrents()
        .stateIn(viewModelScope.coroutineContext, sharingStarted.WhileSubscribed(), emptyList())

    private val _globalStats = repository.observeGlobalStats()
        .stateIn(viewModelScope.coroutineContext, sharingStarted.WhileSubscribed(), GlobalStats(0L, 0L, 0, 0, 0, 0))

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(_torrents, _globalStats) { torrents, stats ->
                _uiState.update { it.copy(torrents = torrents, globalStats = stats) }
            }.collect()
        }
    }

    fun addMagnet(magnetUri: String, sequentialDownload: Boolean = false) {
        viewModelScope.launch {
            val result = repository.addTorrent(magnetUri, getDefaultSavePath(), sequentialDownload)
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }

    fun addTorrentFile(filePath: String, sequentialDownload: Boolean = false) {
        viewModelScope.launch {
            val result = repository.addTorrentFile(filePath, getDefaultSavePath(), sequentialDownload)
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        viewModelScope.launch {
            repository.removeTorrent(infoHash, deleteFiles)
        }
    }

    fun pauseTorrent(infoHash: String) {
        viewModelScope.launch {
            repository.pauseTorrent(infoHash)
        }
    }

    fun resumeTorrent(infoHash: String) {
        viewModelScope.launch {
            repository.resumeTorrent(infoHash)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            repository.pauseAll()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            repository.resumeAll()
        }
    }

    fun setFilePriorities(infoHash: String, priorities: Map<String, FilePriority>) {
        viewModelScope.launch {
            repository.setFilePriorities(infoHash, priorities)
        }
    }

    fun setSequentialDownload(infoHash: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSequentialDownload(infoHash, enabled)
        }
    }

    fun setSeedLimits(infoHash: String, ratioLimit: Float, timeLimitMinutes: Long) {
        viewModelScope.launch {
            repository.setSeedLimits(infoHash, ratioLimit, timeLimitMinutes)
        }
    }

    fun setGlobalSpeedLimits(downloadKbps: Long, uploadKbps: Long) {
        viewModelScope.launch {
            repository.setGlobalSpeedLimits(downloadKbps, uploadKbps)
        }
    }

    fun moveTorrentData(infoHash: String, newPath: String) {
        viewModelScope.launch {
            val result = repository.moveTorrentData(infoHash, newPath)
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }

    fun reannounceTorrent(infoHash: String) {
        viewModelScope.launch {
            repository.reannounceTorrent(infoHash)
        }
    }

    fun scrapeTracker(infoHash: String) {
        viewModelScope.launch {
            repository.scrapeTracker(infoHash)
        }
    }

    fun getTorrentStreamUrl(infoHash: String, fileIndex: Int): String? {
        return repository.getTorrentStreamUrl(infoHash, fileIndex)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getDefaultSavePath(): String {
        return _uiState.value.defaultSavePath
    }
}

data class TorrentUiState(
    val torrents: List<TorrentInfo> = emptyList(),
    val globalStats: GlobalStats = GlobalStats(0L, 0L, 0, 0, 0, 0),
    val errorMessage: String? = null,
    val defaultSavePath: String = "",
    val isLoading: Boolean = false,
)