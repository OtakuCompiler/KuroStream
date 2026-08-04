// This file is part of KuroStream.
//
// TorrentsViewModel — drives the torrent streaming screen backed by the
// :torrent module's OptimizedTorrentEngine (jlibtorrent session).
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.torrents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostwire.jlibtorrent.TorrentHandle
import com.kurostream.torrent.engine.OptimizedTorrentEngine
import com.kurostream.ui.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentsViewModel @Inject constructor(
    private val engine: OptimizedTorrentEngine,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _torrents = MutableStateFlow<List<TorrentUi>>(emptyList())
    val torrents: StateFlow<List<TorrentUi>> = _torrents.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val handles = LinkedHashMap<String, TorrentHandle>()
    private var pollJob: Job? = null

    init {
        startEngine()
    }

    fun startEngine() {
        if (_isStarted.value) return
        viewModelScope.launch {
            try {
                engine.start()
                _isStarted.value = true
                Log.i("Torrents", "Torrent session started")
                pollJob = viewModelScope.launch { pollProgress() }
            } catch (t: Throwable) {
                _error.value = "Failed to start torrent engine: ${t.message}"
                Log.e("Torrents", "Engine start failed", t)
            }
        }
    }

    fun addMagnet(magnet: String) {
        val trimmed = magnet.trim()
        if (trimmed.isEmpty()) return
        if (handles.containsKey(trimmed)) {
            _error.value = "Torrent already added"
            return
        }
        viewModelScope.launch {
            try {
                val savePath = context.filesDir.absolutePath
                val handle = engine.addStreamingTorrent(trimmed, savePath)
                handles[trimmed] = handle
                _torrents.value = _torrents.value + TorrentUi(trimmed, handle.name())
                _error.value = null
                Log.i("Torrents", "Added torrent ${handle.name()}")
            } catch (t: Throwable) {
                _error.value = "Failed to add torrent: ${t.message}"
                Log.e("Torrents", "Add torrent failed", t)
            }
        }
    }

    fun removeTorrent(magnet: String) {
        val handle = handles.remove(magnet) ?: return
        try {
            handle.pause()
        } catch (t: Throwable) {
            Log.w("Torrents", "Pause failed", t)
        }
        _torrents.value = _torrents.value.filterNot { it.magnet == magnet }
    }

    private suspend fun pollProgress() {
        while (isActive) {
            _torrents.value = _torrents.value.map { ui ->
                val handle = handles[ui.magnet]
                if (handle != null) {
                    val status = try {
                        handle.status()
                    } catch (t: Throwable) {
                        null
                    }
                    val progress = status?.progress()?.times(100f)?.toInt() ?: ui.progress
                    ui.copy(
                        progress = progress,
                        state = status?.state()?.toString() ?: ui.state,
                    )
                } else {
                    ui
                }
            }
            delay(1000)
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        try {
            engine.stop()
        } catch (t: Throwable) {
            Log.w("Torrents", "Engine stop failed", t)
        }
        super.onCleared()
    }
}

data class TorrentUi(
    val magnet: String,
    val name: String,
    val progress: Int = 0,
    val state: String = "queued",
)
