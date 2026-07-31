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

import com.kurostream.domain.model.MediaItem
import com.kurostream.domain.model.Trailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataFusionEngine @Inject constructor(
    private val tmdbProvider: com.kurostream.data.metadata.TmdbMetadataProvider,
    private val anilistProvider: com.kurostream.data.metadata.AniListMetadataProvider,
    private val malProvider: com.kurostream.data.metadata.MalMetadataProvider,
    private val kitsuProvider: com.kurostream.data.metadata.KitsuMetadataProvider,
    private val imdbProvider: com.kurostream.data.metadata.ImdbMetadataProvider,
    private val tvdbProvider: com.kurostream.data.metadata.TvdbMetadataProvider,
    private val trailerRepository: com.kurostream.domain.metadata.TrailerRepository,
) {

    suspend fun enrich(mediaId: String, sourceHint: String? = null): MediaItem? = withContext(Dispatchers.IO) {
        val base = tmdbProvider.search(mediaId) ?: anilistProvider.search(mediaId)
        if (base == null) {
            val candidates = mutableListOf<MediaItem>()
            if (sourceHint?.contains("anime", true) == true) {
                malProvider.search(mediaId)?.let { candidates += it }
                kitsuProvider.search(mediaId)?.let { candidates += it }
            }
            imdbProvider.search(mediaId)?.let { candidates += it }
            tvdbProvider.search(mediaId)?.let { candidates += it }
            return@withContext candidates.firstOrNull()
        }
        val enriched = base.copy(
            rating = bestRating(base, imdbProvider.search(mediaId)),
            trailer = bestTrailer(base, trailerRepository.getTrailer(mediaId)),
        )
        enriched
    }

    suspend fun enrichAnime(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        val anilist = anilistProvider.search(mediaId)
        val mal = malProvider.search(mediaId)
        val kitsu = kitsuProvider.search(mediaId)
        val base = anilist ?: mal ?: kitsu ?: return@withContext null
        base.copy(
            rating = bestRating(base, listOfNotNull(mal, kitsu)),
            description = base.description ?: mal?.description ?: kitsu?.description,
        )
    }

    private fun bestRating(base: MediaItem, others: List<MediaItem?>): Float {
        val candidates = others.mapNotNull { it?.rating }.filter { it > 0f }
        return if (candidates.isNotEmpty() && base.rating > 0f) {
            (base.rating + candidates.max()) / 2
        } else base.rating
    }

    private fun bestTrailer(base: MediaItem, trailer: Trailer?): String? =
        trailer?.url ?: base.trailer

    suspend fun getTrailers(mediaId: String): List<Trailer> = withContext(Dispatchers.IO) {
        listOfNotNull(trailerRepository.getTrailer(mediaId))
    }
}
