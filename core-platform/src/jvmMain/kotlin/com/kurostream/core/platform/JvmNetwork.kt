package com.kurostream.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JvmNetwork : PlatformNetwork {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("JvmNetwork not implemented for JVM.")
    }
    override suspend fun post(url: String, body: String?, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("JvmNetwork not implemented for JVM.")
    }
    override suspend fun put(url: String, body: String?, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("JvmNetwork not implemented for JVM.")
    }
    override suspend fun delete(url: String, headers: Map<String, String>): HttpResponse {
        throw UnsupportedOperationException("JvmNetwork not implemented for JVM.")
    }
    override fun webSocket(url: String): Flow<WebSocketMessage> = flow { }
    override fun webSocketWithHeaders(url: String, headers: Map<String, String>): Flow<WebSocketMessage> = flow { }
    override fun download(url: String, destinationPath: String, progress: Flow<Float>): Flow<DownloadResult> = flow { }
    override suspend fun ping(host: String): Boolean = false
    override suspend fun getConnectionQuality(): ConnectionQuality = ConnectionQuality.OFFLINE
}
