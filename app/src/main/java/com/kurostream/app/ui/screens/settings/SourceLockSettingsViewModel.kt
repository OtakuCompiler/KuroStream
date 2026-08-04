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

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SourceLockSettingsViewModel @Inject constructor(
) : ViewModel() {

    private val _locks = MutableStateFlow<List<SourceLock>>(emptyList())
    val locks: StateFlow<List<SourceLock>> = _locks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLocks()
    }

    fun loadLocks() {
        // Source locks are managed in-memory for now
        _isLoading.value = false
        _locks.value = emptyList()
    }

    fun deleteLock(seriesId: String) {
        _locks.value = _locks.value.filter { it.seriesId != seriesId }
    }

    fun clearAllLocks() {
        _locks.value = emptyList()
    }
}
