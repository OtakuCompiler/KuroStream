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

package com.kurostream.players.advanced.watchparty

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight WebSocket signaling server for WebRTC peer discovery.
 * Runs on the host device for LAN-based watch parties.
 */
class SignalingServer(port: Int) : NanoWSD("0.0.0.0", port) {

    private val clients = ConcurrentHashMap<String, NanoWSD.WebSocket>()
    private val _messages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 128)
    val messages = _messages.asSharedFlow()

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return SignalingWebSocket(handshake)
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/health" -> newFixedLengthResponse("OK")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    fun broadcast(message: SignalingMessage) {
        val json = JSONObject().apply {
            put("peer_id", message.peerId)
            put("type", message.type)
            put("sdp", message.sdp)
        }.toString()
        clients.values.forEach { ws ->
            ws.send(json)
        }
    }

    inner class SignalingWebSocket(handshake: IHTTPSession) : WebSocket(handshake) {
        private var peerId: String? = null
        override fun onOpen() {}
        override fun onClose(code: WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean) {
            peerId?.let { clients.remove(it) }
        }
        override fun onMessage(message: WebSocketFrame) {
            try {
                val json = JSONObject(message.textPayload)
                val msg = SignalingMessage(
                    peerId = json.getString("peer_id"),
                    type = json.getString("type"),
                    sdp = json.getString("sdp")
                )
                peerId = msg.peerId
                clients[msg.peerId] = this
                _messages.tryEmit(msg)
            } catch (e: Exception) {
                // Invalid message
            }
        }
        override fun onPong(pong: WebSocketFrame) {}
        override fun onException(exception: Exception) {
            peerId?.let { clients.remove(it) }
        }
    }
}

data class SignalingMessage(
    val peerId: String,
    val type: String,
    val sdp: String
)
