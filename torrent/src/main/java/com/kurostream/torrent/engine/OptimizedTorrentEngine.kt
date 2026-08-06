// This file is part of KuroStream.
//
// OptimizedTorrentEngine — production-grade torrent streaming with
// DHT/PEX/LSD/UDP optimizations and RAM-conscious piece caching.
//
// Fixes in this pass:
//   - addStreamingTorrent now returns TorrentStream with a local HTTP URL
//     served by TorrentStreamServer (NanoHTTPD), replacing the bare
//     TorrentHandle return type.
//   - A progress Flow is exposed so the UI can show download speed/status.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.torrent.engine

import android.util.Log
import com.frostwire.jlibtorrent.AddTorrentParams
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.swig.settings_pack
import com.frostwire.jlibtorrent.swig.settings_pack.int_types
import com.frostwire.jlibtorrent.swig.settings_pack.bool_types
import com.frostwire.jlibtorrent.swig.settings_pack.io_buffer_mode_t
import com.frostwire.jlibtorrent.swig.settings_pack.suggest_mode_t
import com.kurostream.torrent.server.TorrentStreamServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizedTorrentEngine @Inject constructor() {

    private val session = SessionManager()
    private val httpServer = TorrentStreamServer(port = 8090)

    fun start() {
        val sp = SessionParams(SettingsPack())
        val settings = sp.settings()

        settings.connectionsLimit(500)
        settings.maxPeerlistSize(1000)
        settings.setInteger(int_types.connection_speed.swigValue(), 50)
        settings.enableDht(true)
        settings.setBoolean(bool_types.enable_lsd.swigValue(), true)
        settings.setBoolean(bool_types.enable_outgoing_utp.swigValue(), true)
        settings.setBoolean(bool_types.enable_incoming_utp.swigValue(), true)
        settings.setInteger(int_types.torrent_connect_boost.swigValue(), 50)
        settings.setBoolean(bool_types.rate_limit_ip_overhead.swigValue(), true)

        settings.cacheSize(2048)
        settings.activeDownloads(3)
        settings.activeSeeds(5)
        settings.activeLimit(8)
        settings.activeDhtLimit(200)

        settings.setInteger(int_types.disk_io_write_mode.swigValue(), io_buffer_mode_t.enable_os_cache.swigValue())
        settings.setInteger(int_types.disk_io_read_mode.swigValue(), io_buffer_mode_t.enable_os_cache.swigValue())
        settings.setBoolean(bool_types.use_read_cache.swigValue(), true)
        settings.setBoolean(bool_types.volatile_read_cache.swigValue(), true)

        settings.setInteger(int_types.suggest_mode.swigValue(), suggest_mode_t.suggest_read_cache.swigValue())
        settings.seedingOutgoingConnections(false)

        session.start(sp)
        httpServer.startSafe()
    }

    fun addStreamingTorrent(magnet: String, savePath: String): TorrentStream {
        val addParams = AddTorrentParams.parseMagnetUri(magnet)
        val infoHash = addParams.infoHash()

        session.download(magnet, File(savePath))

        val th = session.find(infoHash)
            ?: throw IllegalStateException("Torrent not found after adding: $magnet")

        val pieceLength = th.torrentFile().pieceLength()
        val piecesToBuffer = (5 * 1024 * 1024) / pieceLength
        val numPieces = th.torrentFile().numPieces()
        for (i in 0 until piecesToBuffer.coerceAtMost(numPieces)) {
            th.setPieceDeadline(i, 100)
        }

        val torrentName = th.name() ?: infoHash.toString()
        val targetFile = File(savePath, torrentName)

        httpServer.register(torrentName, targetFile)

        return TorrentStream(
            url = "http://127.0.0.1:8090/stream/$torrentName",
            progressFlow = createProgressFlow(th),
            subtitleTracks = emptyList(),
        )
    }

    private fun createProgressFlow(handle: TorrentHandle): Flow<TorrentProgress> = flow {
        while (true) {
            val status = handle.status()
            emit(
                TorrentProgress(
                    downloadRateBps = status.downloadRate(),
                    progress = status.progress(),
                    seeds = status.numSeeds(),
                    peers = status.numPeers(),
                )
            )
            kotlinx.coroutines.delay(1000)
        }
    }.catch { e ->
        Log.w("OptimizedTorrentEngine", "Progress flow error", e)
    }

    fun stop() {
        httpServer.stopSafe()
        session.stop()
    }
}
