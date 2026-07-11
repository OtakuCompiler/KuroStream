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

import kotlinx.coroutines.flow.*
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchPartySignalingClient @Inject constructor() {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val _messages = MutableSharedFlow<SignalingMessage>()
    val messages: SharedFlow<SignalingMessage> = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    fun connect(url: String, participantId: String) {
        val request = Request.Builder().url("$url?participantId=$participantId").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { _connectionState.value = true }
            override fun onMessage(webSocket: WebSocket, text: String) { _messages.tryEmit(parseMessage(text)) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { _connectionState.value = false }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { _connectionState.value = false }
        })
    }

    fun sendMessage(message: SignalingMessage) { webSocket?.send(serializeMessage(message)) }
    fun disconnect() { webSocket?.close(1000, "User disconnected"); webSocket = null; _connectionState.value = false }

    private fun parseMessage(json: String): SignalingMessage = SignalingMessage("unknown", "", null, null)
    private fun serializeMessage(message: SignalingMessage): String = "{}"
}
