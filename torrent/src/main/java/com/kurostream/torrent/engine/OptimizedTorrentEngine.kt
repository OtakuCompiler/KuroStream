// This file is part of KuroStream.
//
// OptimizedTorrentEngine — production-grade torrent streaming with
// DHT/PEX/LSD/UDP optimizations and RAM-conscious piece caching.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.torrent.engine

import android.util.Log
import com.frostwire.jlibtorrent.AddTorrentParams
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SessionSettings
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.swig.add_torrent_params_flags_t

class OptimizedTorrentEngine {

    private val session = SessionManager()

    fun start() {
        val sp = SessionParams()
        val settings = sp.settings()

        settings.setMaxPeers(500)
        settings.setMaxPeerlistSize(1000)
        settings.setConnectionSpeed(50)
        settings.setEnableDht(true)
        settings.setEnableLsd(true)
        settings.setEnableUtp(true)
        settings.setTorrentConnectBoost(50)
        settings.setRateLimitIpOverhead(true)

        settings.setCacheSize(2048)
        settings.setActiveDownloads(3)
        settings.setActiveSeeds(5)
        settings.setActiveLimit(8)
        settings.setActiveDhtLimit(200)

        settings.setDiskIoWriteMode(SessionSettings.io_buffer_mode_t.enable_os_cache)
        settings.setDiskIoReadMode(SessionSettings.io_buffer_mode_t.enable_os_cache)
        settings.setGuidedReadCache(true)
        settings.setVolatileReadCache(true)

        settings.setSuggestMode(true)
        settings.setSeedingOutgoingConnections(false)

        sp.setSettings(settings)
        session.start(sp)
    }

    fun addStreamingTorrent(magnet: String, savePath: String): TorrentHandle {
        val addParams = AddTorrentParams.parseMagnetUri(magnet)
        addParams.savePath(savePath)
        addParams.flags = addParams.flags.and(add_torrent_params_flags_t.seed_mode.swigValue().inv())

        val th = session.addTorrent(addParams)
        th.setSequentialDownload(true)
        th.setPriority(7)

        val pieceLength = th.torrentFile().swig().pieceLength()
        val piecesToBuffer = (5 * 1024 * 1024) / pieceLength
        val numPieces = th.torrentFile().numPieces()
        for (i in 0 until piecesToBuffer.coerceAtMost(numPieces)) {
            th.setPieceDeadline(i, 100)
        }

        return th
    }

    fun stop() = session.stop()
}
