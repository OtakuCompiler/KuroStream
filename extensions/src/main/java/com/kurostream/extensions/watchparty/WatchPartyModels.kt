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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
data class WatchPartySession(
    @Json(name = "id") val id: String,
    @Json(name = "hostId") val hostId: String,
    @Json(name = "mediaId") val mediaId: String,
    @Json(name = "mediaUrl") val mediaUrl: String,
    @Json(name = "createdAt") val createdAt: Long,
    @Json(name = "participants") val participants: List<Participant> = emptyList(),
    @Json(name = "isPlaying") val isPlaying: Boolean = false,
    @Json(name = "currentTime") val currentTime: Long = 0,
    @Json(name = "playbackRate") val playbackRate: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class Participant(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "isHost") val isHost: Boolean = false,
    @Json(name = "isConnected") val isConnected: Boolean = true,
    @Json(name = "joinedAt") val joinedAt: Long
)

@JsonClass(generateAdapter = true)
data class SignalingMessage(
    @Json(name = "type") val type: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String? = null,
    @Json(name = "payload") val payload: Map<String, Any>? = null,
    @Json(name = "sessionId") val sessionId: String? = null
)

@Serializable
@JsonClass(generateAdapter = true)
data class PlaybackSyncMessage(
    @Json(name = "action") val action: SyncAction,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "positionMs") val positionMs: Long,
    @Json(name = "senderId") val senderId: String
)

enum class SyncAction { PLAY, PAUSE, SEEK, BUFFERING, READY, PING, PONG }

data class WebRtcConfig(val iceServers: List<IceServer> = listOf(
    IceServer("stun:stun.l.google.com:19302"),
    IceServer("stun:stun1.l.google.com:19302")
))

data class IceServer(val urls: String, val username: String? = null, val credential: String? = null)

@Serializable
data class PlaybackState(val isPlaying: Boolean = false, val positionMs: Long = 0, val playbackRate: Float = 1.0f, val timestamp: Long = System.currentTimeMillis())