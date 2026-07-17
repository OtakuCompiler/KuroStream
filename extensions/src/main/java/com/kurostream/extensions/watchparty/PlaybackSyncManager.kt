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

package com.kurostream.extensions.watchparty

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSyncManager @Inject constructor(
    private val webRtcManager: WebRtcManager,
    private val signalingClient: WatchPartySignalingClient
) {
    private val _localState = MutableStateFlow(PlaybackState())
    val localState: StateFlow<PlaybackState> = _localState.asStateFlow()

    private val _remoteStates = MutableStateFlow<Map<String, PlaybackState>>(emptyMap())
    val remoteStates: StateFlow<Map<String, PlaybackState>> = _remoteStates.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val pingIntervalMs = 5000L
    private val syncThresholdMs = 2000L

    fun initializeAsHost(mediaId: String, mediaUrl: String) {
        _isHost.value = true
        startPeriodicSync()
    }

    fun joinAsParticipant(sessionId: String, hostId: String) {
        _isHost.value = false
        listenForSyncMessages()
    }

    fun updateLocalState(isPlaying: Boolean, positionMs: Long, playbackRate: Float = 1.0f) {
        _localState.value = PlaybackState(isPlaying, positionMs, playbackRate, System.currentTimeMillis())
        broadcastState()
    }

    fun onLocalPlay(positionMs: Long) { updateLocalState(true, positionMs); broadcastAction(SyncAction.PLAY, positionMs) }
    fun onLocalPause(positionMs: Long) { updateLocalState(false, positionMs); broadcastAction(SyncAction.PAUSE, positionMs) }
    fun onLocalSeek(positionMs: Long) { updateLocalState(_localState.value.isPlaying, positionMs); broadcastAction(SyncAction.SEEK, positionMs) }

    private fun broadcastState() {
        val state = _localState.value
        val message = PlaybackSyncMessage(if (state.isPlaying) SyncAction.PLAY else SyncAction.PAUSE, System.currentTimeMillis(), state.positionMs, "local")
        broadcastMessage(message)
    }

    private fun broadcastAction(action: SyncAction, positionMs: Long) {
        val message = PlaybackSyncMessage(action, System.currentTimeMillis(), positionMs, "local")
        broadcastMessage(message)
    }

    private fun broadcastMessage(message: PlaybackSyncMessage) {
        val json = serializeSyncMessage(message)
        webRtcManager.broadcastMessage(json)
    }

    private fun listenForSyncMessages() {
        syncScope.launch {
            webRtcManager.incomingMessages.collect { json ->
                val message = deserializeSyncMessage(json)
                handleRemoteMessage(message)
            }
        }
    }

    private fun handleRemoteMessage(message: PlaybackSyncMessage) {
        when (message.action) {
            SyncAction.PLAY -> _remoteStates.value = _remoteStates.value + (message.senderId to PlaybackState(true, message.positionMs, 1.0f, message.timestamp))
            SyncAction.PAUSE -> _remoteStates.value = _remoteStates.value + (message.senderId to PlaybackState(false, message.positionMs, 1.0f, message.timestamp))
            SyncAction.SEEK -> {}
            SyncAction.PING -> {}
            SyncAction.PONG -> {}
            else -> {}
        }
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = syncScope.launch {
            while (isActive) {
                delay(pingIntervalMs)
                val ping = PlaybackSyncMessage(SyncAction.PING, System.currentTimeMillis(), _localState.value.positionMs, "local")
                broadcastMessage(ping)
            }
        }
    }

    fun calculateSyncedPosition(): Long {
        if (_isHost.value) return _localState.value.positionMs
        val remote = _remoteStates.value.values.firstOrNull() ?: return _localState.value.positionMs
        return remote.positionMs + estimateLatency()
    }

    private fun estimateLatency(): Long = 100L

    fun shouldSync(): Boolean {
        if (_isHost.value) return false
        return kotlin.math.abs(calculateSyncedPosition() - _localState.value.positionMs) > syncThresholdMs
    }

    fun dispose() { syncJob?.cancel(); syncScope.cancel() }

    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    private fun serializeSyncMessage(message: PlaybackSyncMessage): String {
        return jsonSerializer.encodeToString(message)
    }

    private fun deserializeSyncMessage(jsonString: String): PlaybackSyncMessage {
        return jsonSerializer.decodeFromString<PlaybackSyncMessage>(jsonString)
    }
}