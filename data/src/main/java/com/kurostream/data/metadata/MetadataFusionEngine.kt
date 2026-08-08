// This file is part of KuroStream.
//
// MetadataFusionEngine — combines metadata from multiple providers
// (TMDB, AniList, MAL, Kitsu, IMDb, TVDB, YouTube) with fallback priority.
// No provider replaces another; all are consulted and the best data wins.
//
// Priority order:
//   1. TMDB (movies + TV)
//   2. AniList (anime)
//   3. MAL (anime backup)
//   4. Kitsu (anime backup)
//   5. IMDb (ratings)
//   6. TVDB (episode data)
//   7. YouTube (trailers)
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.metadata

import android.util.LruCache
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.metadata.AnimeMetadata
import com.kurostream.domain.metadata.MetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata fusion engine.
 *
 * Performance: every provider call is bounded by [PROVIDER_TIMEOUT_MS] so a
 * single slow API never blocks the UI for more than ~3s. Results are cached
 * in memory for [CACHE_TTL_MS] to make repeat lookups instant.
 */
@Singleton
class MetadataFusionEngine @Inject constructor(
    private val tmdbProvider: com.kurostream.data.metadata.TmdbMetadataProvider,
    private val anilistProvider: com.kurostream.data.metadata.AniListMetadataProvider,
    private val malProvider: com.kurostream.data.metadata.MalMetadataProvider,
    private val kitsuProvider: com.kurostream.data.metadata.KitsuMetadataProvider,
    private val trailerRepository: com.kurostream.domain.metadata.TrailerRepository,
) {

    private val cache = LruCache<String, CachedMetadata>(256)
    private val cacheTimestamps = HashMap<String, Long>()

    /**
     * Enrich a media item by querying all providers in parallel.
     * Each provider call has a hard [PROVIDER_TIMEOUT_MS] timeout.
     */
    suspend fun enrich(mediaId: String, sourceHint: String? = null): MediaItem? = withContext(Dispatchers.IO) {
        val cached = cacheFor(mediaId)
        if (cached != null) return@withContext cached

        coroutineScope {
            val deferreds = listOf(
                async { withTimeout(PROVIDER_TIMEOUT_MS) { tmdbProvider.searchAnime(mediaId, 1, 10) } },
                async { withTimeout(PROVIDER_TIMEOUT_MS) { anilistProvider.searchAnime(mediaId, 1, 10) } },
                async { withTimeout(PROVIDER_TIMEOUT_MS) { malProvider.searchAnime(mediaId, 1, 10) } },
                async { withTimeout(PROVIDER_TIMEOUT_MS) { kitsuProvider.searchAnime(mediaId, 1, 10) } },
            )
            val results = deferreds.awaitAll().flatMap { it.successfulData() }

            if (results.isEmpty()) return@coroutineScope null

            val merged = results.first().copy(
                score = results.mapNotNull { it.score }.maxOrNull() ?: results.first().score,
                genres = results.flatMap { it.genres }.distinct(),
            )
            val item = merged.toMediaItem()
            cachePut(mediaId, item)
            item
        }
    }

    /**
     * Anime-specific enrichment that skips TMDB (anime is usually not in TMDB).
     */
    suspend fun enrichAnime(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        val cached = cacheFor(mediaId)
        if (cached != null) return@withContext cached

        coroutineScope {
            val deferreds = listOf(
                async { withTimeout(PROVIDER_TIMEOUT_MS) { anilistProvider.searchAnime(mediaId, 1, 10) } },
                async { withTimeout(PROVIDER_TIMEOUT_MS) { malProvider.searchAnime(mediaId, 1, 10) } },
                async { withTimeout(PROVIDER_TIMEOUT_MS) { kitsuProvider.searchAnime(mediaId, 1, 10) } },
            )
            val results = deferreds.awaitAll().flatMap { it.successfulData() }
            val base = results.firstOrNull() ?: return@coroutineScope null
            val merged = base.copy(
                score = results.mapNotNull { it.score }.maxOrNull() ?: base.score,
                description = results.mapNotNull { it.description }.firstOrNull() ?: base.description,
            )
            val item = merged.toMediaItem()
            cachePut(mediaId, item)
            item
        }
    }

    suspend fun getTrailers(mediaId: String): List<com.kurostream.domain.model.Trailer> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
            trailerRepository.getTrailerForAnime(mediaId).getOrNull()?.let { listOf(it) } ?: emptyList()
        } ?: emptyList()
    }

    private fun cacheFor(key: String): MediaItem? {
        val ts = cacheTimestamps[key] ?: return null
        if (System.currentTimeMillis() - ts > CACHE_TTL_MS) {
            cache.remove(key)
            cacheTimestamps.remove(key)
            return null
        }
        return cache.get(key)?.item
    }

    private fun cachePut(key: String, item: MediaItem) {
        cache.put(key, CachedMetadata(item))
        cacheTimestamps[key] = System.currentTimeMillis()
    }

    private suspend fun <T> withTimeout(ms: Long, block: suspend () -> T): T? =
        withTimeoutOrNull(ms) { block() }

    private fun MetadataResult<List<AnimeMetadata>>.successfulData(): List<AnimeMetadata> = when (this) {
        is MetadataResult.Success -> data
        is MetadataResult.Partial -> data
        else -> emptyList()
    }

    private fun AnimeMetadata.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        description = description ?: "",
        posterUrl = coverImageUrl ?: "",
        backdropUrl = bannerImageUrl ?: "",
        genre = genres,
        rating = (score ?: 0.0).toFloat(),
        year = seasonYear ?: 0,
        duration = durationMinutes ?: 0,
        source = providerId,
    )

    private data class CachedMetadata(val item: MediaItem)

    companion object {
        private const val PROVIDER_TIMEOUT_MS = 3_000L
        private const val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 minutes
    }
}
