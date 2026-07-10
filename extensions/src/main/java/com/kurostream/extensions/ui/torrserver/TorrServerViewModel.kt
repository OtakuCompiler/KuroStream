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
import com.kurostream.extensions.torrserver.TorrServerRepository
import com.kurostream.extensions.torrserver.TorrServerTorrent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrServerViewModel @Inject constructor(
    private val repository: TorrServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TorrServerUiState>(TorrServerUiState.Loading)
    val uiState: StateFlow<TorrServerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = TorrServerUiState.Loading
            repository.listTorrents().collect { result ->
                _uiState.value = when {
                    result.isSuccess -> {
                        val torrents = result.getOrNull() ?: emptyList()
                        if (torrents.isEmpty()) TorrServerUiState.Empty
                        else TorrServerUiState.Success(torrents)
                    }
                    else -> TorrServerUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            }
        }
    }

    fun addTorrent(link: String, title: String?) {
        viewModelScope.launch {
            _uiState.value = TorrServerUiState.Loading
            val result = repository.addTorrent(link, title)
            if (result.isSuccess) {
                refresh()
            } else {
                _uiState.value = TorrServerUiState.Error(result.exceptionOrNull()?.message ?: "Failed to add torrent")
            }
        }
    }

    fun removeTorrent(hash: String) {
        viewModelScope.launch {
            repository.removeTorrent(hash)
            refresh()
        }
    }

    fun dropTorrent(hash: String) {
        viewModelScope.launch {
            repository.dropTorrent(hash)
        }
    }
}

sealed class TorrServerUiState {
    object Loading : TorrServerUiState()
    object Empty : TorrServerUiState()
    data class Success(val torrents: List<TorrServerTorrent>) : TorrServerUiState()
    data class Error(val message: String) : TorrServerUiState()
}
