package com.kurostream.legacyui.anistream.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.backup.BackupManager
import com.kurostream.data.anistream.backup.BackupSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackup(destinationUri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Exporting
            val result = backupManager.exportBackup(destinationUri, password)
            _uiState.value = result.fold(
                onSuccess = { uri -> BackupUiState.ExportSuccess(uri) },
                onFailure = { e -> BackupUiState.Error(e.message ?: "Export failed") }
            )
        }
    }

    fun importBackup(sourceUri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Importing
            val result = backupManager.importBackup(sourceUri, password)
            _uiState.value = result.fold(
                onSuccess = { summary -> BackupUiState.ImportSuccess(summary) },
                onFailure = { e -> BackupUiState.Error(e.message ?: "Import failed") }
            )
        }
    }
}

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Exporting : BackupUiState()
    object Importing : BackupUiState()
    data class ExportSuccess(val uri: String) : BackupUiState()
    data class ImportSuccess(val summary: BackupSummary) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}
