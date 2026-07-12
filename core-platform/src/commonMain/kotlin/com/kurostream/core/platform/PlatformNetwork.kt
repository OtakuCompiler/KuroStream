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

package com.kurostream.core.platform

import kotlinx.coroutines.flow.Flow

interface PlatformNetwork {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
    suspend fun post(url: String, body: String?, headers: Map<String, String> = emptyMap()): HttpResponse
    suspend fun put(url: String, body: String?, headers: Map<String, String> = emptyMap()): HttpResponse
    suspend fun delete(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
    
    fun webSocket(url: String): Flow<WebSocketMessage>
    fun webSocketWithHeaders(url: String, headers: Map<String, String>): Flow<WebSocketMessage>
    
    fun download(url: String, destinationPath: String, progress: Flow<Float>): Flow<DownloadResult>
    
    suspend fun ping(host: String): Boolean
    suspend fun getConnectionQuality(): ConnectionQuality
}

data class HttpResponse(
    val statusCode: Int,
    val body: String?,
    val headers: Map<String, String>,
    val isSuccessful: Boolean
)

sealed interface WebSocketMessage {
    data class Text(val text: String) : WebSocketMessage
    data class Binary(val data: ByteArray) : WebSocketMessage
    data class Error(val throwable: Throwable) : WebSocketMessage
    object Closed : WebSocketMessage
}

data class DownloadResult(
    val success: Boolean,
    val filePath: String?,
    val error: String?
)

enum class ConnectionQuality {
    OFFLINE,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}