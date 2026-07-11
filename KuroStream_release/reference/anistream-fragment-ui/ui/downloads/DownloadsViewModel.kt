package com.kurostream.legacyui.anistream.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.downloads.DownloadItem
import com.kurostream.data.anistream.downloads.DownloadManager
import com.kurostream.data.anistream.downloads.DownloadProgress
import com.kurostream.data.anistream.downloads.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _activeProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeProgress: StateFlow<Map<String, DownloadProgress>> = _activeProgress.asStateFlow()

    private val _events = MutableSharedFlow<DownloadsEvent>()
    val events: SharedFlow<DownloadsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            downloadManager.downloadQueue.collectLatest { items ->
                _downloads.value = items.sortedByDescending {
                    when (it.status) {
                        DownloadStatus.DOWNLOADING -> 4
                        DownloadStatus.QUEUED -> 3
                        DownloadStatus.PAUSED -> 2
                        DownloadStatus.FAILED -> 1
                        DownloadStatus.COMPLETED -> 0
                        DownloadStatus.CANCELLED -> -1
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadManager.activeDownloads.collectLatest { progress ->
                _activeProgress.value = progress
            }
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(downloadId)
            _events.emit(DownloadsEvent.ShowMessage("Download paused"))
        }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(downloadId)
            _events.emit(DownloadsEvent.ShowMessage("Download resumed"))
        }
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(downloadId)
            _events.emit(DownloadsEvent.ShowMessage("Download cancelled"))
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.retryDownload(downloadId)
            _events.emit(DownloadsEvent.ShowMessage("Retrying download..."))
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.removeDownload(downloadId)
            _events.emit(DownloadsEvent.ShowMessage("Download deleted"))
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            downloadManager.pauseAll()
            _events.emit(DownloadsEvent.ShowMessage("All downloads paused"))
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            downloadManager.resumeAll()
            _events.emit(DownloadsEvent.ShowMessage("All downloads resumed"))
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val completed = _downloads.value.filter { it.status == DownloadStatus.COMPLETED }
            completed.forEach { downloadManager.removeDownload(it.id) }
            _events.emit(DownloadsEvent.ShowMessage("Completed downloads cleared"))
        }
    }

    fun onItemFocused(item: DownloadItem) {
        // Could preload poster
    }
}

sealed class DownloadsEvent {
    data class ShowError(val message: String) : DownloadsEvent()
    data class ShowMessage(val message: String) : DownloadsEvent()
}
