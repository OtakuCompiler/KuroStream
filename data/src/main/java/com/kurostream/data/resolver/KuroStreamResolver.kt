// This file is part of KuroStream.
//
// KuroStreamResolver — unified source discovery and ranking.
// Merges results from:
//   - Cloudstream extensions
//   - Stremio addons
//   - Nuvio extensions
//   - Torrent providers
//   - HTTP providers
//   - Local media
//   - Debrid services
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.resolver

import com.kurostream.domain.resolver.SourceHealth
import com.kurostream.domain.resolver.SourceType
import com.kurostream.domain.resolver.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroStreamResolver @Inject constructor(
    private val healthManager: SourceHealthManager,
) {

    suspend fun resolve(query: String): List<StreamSource> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferred = mutableListOf<Deferred<List<StreamSource>>>()
            deferred += async { resolveFromExtensions(query) }
            deferred += async { resolveFromTorrents(query) }
            deferred += async { resolveFromHttp(query) }
            deferred += async { resolveFromLocal(query) }
            val all = deferred.awaitAll().flatten()
            rank(all)
        }
    }

    private suspend fun resolveFromExtensions(query: String): List<StreamSource> = emptyList()
    private suspend fun resolveFromTorrents(query: String): List<StreamSource> = emptyList()
    private suspend fun resolveFromHttp(query: String): List<StreamSource> = emptyList()
    private suspend fun resolveFromLocal(query: String): List<StreamSource> = emptyList()

    private fun rank(sources: List<StreamSource>): List<StreamSource> {
        val healthOrder = listOf(SourceHealth.EXCELLENT, SourceHealth.GOOD, SourceHealth.DEGRADED, SourceHealth.POOR, SourceHealth.UNKNOWN)
        return sources.sortedWith { a, b ->
            val ha = healthOrder.indexOf(a.health).let { if (it == -1) Int.MAX_VALUE else it }
            val hb = healthOrder.indexOf(b.health).let { if (it == -1) Int.MAX_VALUE else it }
            val hc = ha.compareTo(hb)
            if (hc != 0) return@sortedWith hc

            val qa = qualityScore(a)
            val qb = qualityScore(b)
            qb.compareTo(qa)
        }
    }

    private fun qualityScore(source: StreamSource): Int = when {
        source.isDolbyVision -> 100
        source.isHdr -> 80
        source.quality.contains("4K", true) || source.quality.contains("2160", true) -> 70
        source.quality.contains("1080", true) || source.quality.contains("FHD", true) -> 60
        source.quality.contains("720", true) || source.quality.contains("HD", true) -> 50
        source.quality.contains("480", true) -> 40
        else -> 30
    }
}

typealias Deferred<T> = kotlinx.coroutines.Deferred<T>
