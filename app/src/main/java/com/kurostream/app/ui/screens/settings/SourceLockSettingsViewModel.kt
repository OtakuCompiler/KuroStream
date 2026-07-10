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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.domain.model.SourceLock
import com.kurostream.app.repository.SourceLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourceLockSettingsViewModel @Inject constructor(
    application: Application,
    private val sourceLockRepository: SourceLockRepository
) : AndroidViewModel(application) {

    private val _locks = mutableStateFlow<List<SourceLock>>(emptyList())
    val locks: Flow<List<SourceLock>> = _locks.asStateFlow()

    private val _isLoading = mutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLocks()
    }

    fun loadLocks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locks.value = sourceLockRepository.observeAllActive()
                    .filter { it.isActive }
                    .toList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLock(seriesId: String) {
        viewModelScope.launch {
            sourceLockRepository.clearLock(seriesId)
            loadLocks()
        }
    }

    fun clearAllLocks() {
        viewModelScope.launch {
            sourceLockRepository.clearAllLocks()
            _locks.value = emptyList()
        }
    }
}