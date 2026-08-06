// This file is part of KuroStream.
//
// TorrentStreamServer — lightweight HTTP server (via NanoHTTPD) that exposes
// a local progressive-stream URL for Media3/ExoPlayer to consume.
//
// URL scheme: GET http://127.0.0.1:8090/stream/<torrent_name>
//
// Piece-priority management for sequential playback is handled by
// OptimizedTorrentEngine.addStreamingTorrent. This server only turns the
// resulting on-disk file into a progressive HTTP response.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.torrent.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileInputStream

class TorrentStreamServer(private val port: Int = 8090) : NanoHTTPD(port) {

    private val _progress = MutableSharedFlow<TorrentProgress>()
    val progress = _progress.asSharedFlow()

    private val registry = mutableMapOf<String, File>()

    fun register(name: String, file: File) {
        registry[name] = file
    }

    fun unregister(name: String) {
        registry.remove(name)
    }

    fun startSafe() {
        try {
            start()
            Log.i("TorrentStreamServer", "Listening on port $port")
        } catch (e: Exception) {
            Log.w("TorrentStreamServer", "Port $port busy; server not started", e)
        }
    }

    fun stopSafe() {
        try {
            stop()
        } catch (e: Exception) {
            Log.w("TorrentStreamServer", "Stop failed", e)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trimStart('/')
        val parts = uri.split("/", limit = 2)

        if (parts.isEmpty() || parts[0] != "stream") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        val torrentName = parts.getOrNull(1)?.substringBefore("/") ?: ""
        val file = registry[torrentName]

        if (file == null || !file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Torrent not ready")
        }

        return try {
            FileInputStream(file).use { fis ->
                newChunkedResponse(Response.Status.OK, "video/mp4", fis)
            }
        } catch (e: Exception) {
            Log.w("TorrentStreamServer", "Serve failed for $torrentName", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "IO error")
        }
    }
}
