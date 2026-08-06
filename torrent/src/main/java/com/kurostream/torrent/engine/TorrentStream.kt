// This file is part of KuroStream.
//
// TorrentStream — result of successfully adding a streaming torrent.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.torrent.engine

import kotlinx.coroutines.flow.Flow

data class TorrentStream(
    val url: String,
    val progressFlow: Flow<TorrentProgress>,
    val subtitleTracks: List<EmbeddedSubtitle> = emptyList(),
)

data class TorrentProgress(
    val downloadRateBps: Long = 0,
    val progress: Float = 0f,
    val seeds: Int = 0,
    val peers: Int = 0,
)

data class EmbeddedSubtitle(
    val language: String,
    val fileName: String,
    val format: String,
)
