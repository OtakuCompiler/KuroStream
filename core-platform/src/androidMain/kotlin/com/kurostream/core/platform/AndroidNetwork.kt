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

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose

class AndroidNetwork(private val context: Context) : PlatformNetwork {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers).get().build()
        executeRequest(request)
    }
    
    override suspend fun post(url: String, body: String?, headers: Map<String, String>): HttpResponse = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers).post(toRequestBody(body)).build()
        executeRequest(request)
    }
    
    override suspend fun put(url: String, body: String?, headers: Map<String, String>): HttpResponse = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers).put(toRequestBody(body)).build()
        executeRequest(request)
    }
    
    override suspend fun delete(url: String, headers: Map<String, String>): HttpResponse = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers).delete().build()
        executeRequest(request)
    }
    
    private fun buildRequest(url: String, headers: Map<String, String>): Request.Builder {
        return Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
        }
    }
    
    private fun toRequestBody(body: String?): RequestBody {
        return (body ?: "").toRequestBody(mediaType)
    }
    
    private suspend fun executeRequest(request: Request): HttpResponse = suspendCancellableCoroutine { cont ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resume(HttpResponse(0, null, emptyMap(), false))
            }
            
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val responseHeaders = response.headers.toMultimap().mapValues { (_, v) -> v.joinToString(",") }
                cont.resume(HttpResponse(
                    statusCode = response.code,
                    body = body,
                    headers = responseHeaders,
                    isSuccessful = response.isSuccessful
                ))
                response.close()
            }
        })
    }
    
    override fun webSocket(url: String): Flow<WebSocketMessage> = webSocketWithHeaders(url, emptyMap())
    
    override fun webSocketWithHeaders(url: String, headers: Map<String, String>): Flow<WebSocketMessage> = callbackFlow {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
        }.build()
        
        val channel = Channel<WebSocketMessage>()
        var ws: WebSocket? = null
        
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                ws = webSocket
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                channel.trySend(WebSocketMessage.Text(text))
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                channel.trySend(WebSocketMessage.Binary(bytes.toByteArray()))
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                channel.trySend(WebSocketMessage.Closed)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                channel.trySend(WebSocketMessage.Error(t))
            }
        })
        
        awaitClose { ws?.close(1000, "Flow cancelled") }
    }
    
    override fun download(url: String, destinationPath: String, progress: Flow<Float>): Flow<DownloadResult> = callbackFlow {
        val request = Request.Builder().url(url).build()
        val file = File(destinationPath)
        file.parentFile?.mkdirs()
        
        val call = client.newCall(request)
        val response = call.execute()
        
        val totalBytes = response.body?.contentLength() ?: 0L
        var downloadedBytes = 0L
        
        try {
            val inputStream = response.body?.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8192)
            
            inputStream?.let { `in` ->
                while (true) {
                    val read = `in`.read(buffer)
                    if (read == -1) break
                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0) {
                        trySend(DownloadResult(true, destinationPath, null))
                    }
                }
            }
            
            outputStream.close()
            response.close()
            trySend(DownloadResult(true, destinationPath, null))
        } catch (e: Exception) {
            trySend(DownloadResult(false, null, e.message))
        } finally {
            call.cancel()
        }
    }
    
    override suspend fun ping(host: String): Boolean = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(host).isReachable(5000)
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getConnectionQuality(): ConnectionQuality = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val reachable = ping("8.8.8.8")
        val latency = System.currentTimeMillis() - start
        
        if (!reachable) return@withContext ConnectionQuality.OFFLINE
        
        return@withContext when {
            latency < 50 -> ConnectionQuality.EXCELLENT
            latency < 100 -> ConnectionQuality.GOOD
            latency < 200 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }
}