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

import okhttp3.*
import okio.ByteString
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * WebSocket signaling client for connecting to a watch party host.
 */
class SignalingClient(
    private val hostAddress: InetAddress,
    private val port: Int
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var messageHandler: ((SignalingMessage) -> Unit)? = null

    fun connect(onMessage: (SignalingMessage) -> Unit) {
        messageHandler = onMessage
        val request = Request.Builder()
            .url("ws://${hostAddress.hostAddress}:$port")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {}
            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = org.json.JSONObject(text)
                    val message = SignalingMessage(
                        peerId = json.getString("peer_id"),
                        type = json.getString("type"),
                        sdp = json.getString("sdp")
                    )
                    messageHandler?.invoke(message)
                } catch (e: Exception) {}
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {}
        })
    }

    fun send(message: SignalingMessage) {
        val json = org.json.JSONObject().apply {
            put("peer_id", message.peerId)
            put("type", message.type)
            put("sdp", message.sdp)
        }.toString()
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        client.dispatcher.executorService.shutdown()
    }
}
