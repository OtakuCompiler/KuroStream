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

package com.kurostream.backup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.backup.domain.*
import com.kurostream.backup.repository.BackupRepository
import com.kurostream.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sharingStarted
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    private val _authState = repository.observeAuthState()
        .stateIn(viewModelScope.coroutineContext, sharingStarted.WhileSubscribed(), GitHubAuthState())

    private val _backupConfig = repository.observeBackupConfig()
        .stateIn(viewModelScope.coroutineContext, sharingStarted.WhileSubscribed(), BackupConfig())

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(_authState, _backupConfig) { auth, config ->
                _uiState.update { it.copy(authState = auth, backupConfig = config) }
            }.collect()
        }
    }

    fun authenticate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, errorMessage = null) }
            val result = repository.authenticate()
            _uiState.update { it.copy(isAuthenticating = false) }
            when (result) {
                is Result.Success -> {
                    // Auth state will update via flow
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val result = repository.logout()
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.exception.message) }
            }
        }
    }

    fun createBackup(password: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBackup = true, errorMessage = null) }
            val config = _uiState.value.backupConfig
            val result = repository.createBackup(config, password)
            _uiState.update { it.copy(isCreatingBackup = false) }
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(lastBackupMetadata = result.data, showBackupSuccess = true) }
                    refreshBackups()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
            }
        }
    }

    fun refreshBackups() {
        viewModelScope.launch {
            val config = _uiState.value.backupConfig
            val result = repository.listBackups(config)
            if (result is Result.Success) {
                _uiState.update { it.copy(availableBackups = result.data) }
            }
        }
    }

    fun restoreBackup(metadata: BackupMetadata, password: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, errorMessage = null) }
            val config = _uiState.value.backupConfig
            val result = repository.restoreBackup(config, metadata, password)
            _uiState.update { it.copy(isRestoring = false) }
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(showRestoreSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
            }
        }
    }

    fun deleteBackup(metadata: BackupMetadata) {
        viewModelScope.launch {
            val config = _uiState.value.backupConfig
            val result = repository.deleteBackup(config, metadata)
            if (result is Result.Success) {
                refreshBackups()
            } else if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.exception.message) }
            }
        }
    }

    fun updateBackupConfig(config: BackupConfig) {
        viewModelScope.launch {
            repository.updateBackupConfig(config)
        }
    }

    fun showCreateBackupDialog() {
        _uiState.update { it.copy(showCreateBackupDialog = true) }
    }

    fun dismissCreateBackupDialog() {
        _uiState.update { it.copy(showCreateBackupDialog = false) }
    }

    fun showManageBackupsDialog() {
        refreshBackups()
        _uiState.update { it.copy(showManageBackupsDialog = true) }
    }

    fun dismissManageBackupsDialog() {
        _uiState.update { it.copy(showManageBackupsDialog = false) }
    }

    fun showConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true) }
    }

    fun dismissConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class BackupUiState(
    val authState: GitHubAuthState = GitHubAuthState(),
    val backupConfig: BackupConfig = BackupConfig(),
    val availableBackups: List<BackupMetadata> = emptyList(),
    val lastBackupMetadata: BackupMetadata? = null,
    val isAuthenticating: Boolean = false,
    val isCreatingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
    val showCreateBackupDialog: Boolean = false,
    val showManageBackupsDialog: Boolean = false,
    val showConfigDialog: Boolean = false,
    val showBackupSuccess: Boolean = false,
    val showRestoreSuccess: Boolean = false,
)