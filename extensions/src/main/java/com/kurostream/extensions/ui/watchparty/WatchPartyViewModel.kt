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

package com.kurostream.extensions.ui.watchparty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.extensions.watchparty.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchPartyViewModel @Inject constructor(
    private val syncManager: PlaybackSyncManager,
    private val webRtcManager: WebRtcManager,
    private val signalingClient: WatchPartySignalingClient
) : ViewModel() {

    private val _sessionState = MutableStateFlow<WatchPartySession?>(null)
    val sessionState: StateFlow<WatchPartySession?> = _sessionState.asStateFlow()

    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants.asStateFlow()

    val isHost: StateFlow<Boolean> = syncManager.isHost
    val connectionState: StateFlow<Boolean> = signalingClient.connectionState

    private val _showInviteDialog = MutableStateFlow(false)
    val showInviteDialog: StateFlow<Boolean> = _showInviteDialog.asStateFlow()

    init {
        viewModelScope.launch { syncManager.remoteStates.collect {} }
    }

    fun createSession(mediaId: String, mediaUrl: String) {
        viewModelScope.launch {
            val session = WatchPartySession(
                id = generateSessionId(), hostId = "local", mediaId = mediaId, mediaUrl = mediaUrl,
                createdAt = System.currentTimeMillis(),
                participants = listOf(Participant("local", "You", true, true, System.currentTimeMillis()))
            )
            _sessionState.value = session
            syncManager.initializeAsHost(mediaId, mediaUrl)
        }
    }

    fun joinSession(sessionId: String, hostId: String) {
        viewModelScope.launch { syncManager.joinAsParticipant(sessionId, hostId) }
    }

    fun connectSignaling(url: String, participantId: String) { signalingClient.connect(url, participantId) }
    fun broadcastPlay() { _sessionState.value?.let { _sessionState.value = it.copy(isPlaying = true) }; syncManager.onLocalPlay(getCurrentPosition()) }
    fun broadcastPause() { _sessionState.value?.let { _sessionState.value = it.copy(isPlaying = false) }; syncManager.onLocalPause(getCurrentPosition()) }
    fun broadcastSeek(positionMs: Long) { syncManager.onLocalSeek(positionMs) }
    fun showInviteDialog() { _showInviteDialog.value = true }
    fun dismissInviteDialog() { _showInviteDialog.value = false }
    private fun getCurrentPosition(): Long = 0L
    private fun generateSessionId(): String = "wp-${System.currentTimeMillis()}-${(1000..9999).random()}"

    override fun onCleared() {
        super.onCleared()
        syncManager.dispose()
        signalingClient.disconnect()
        webRtcManager.dispose()
    }
}
