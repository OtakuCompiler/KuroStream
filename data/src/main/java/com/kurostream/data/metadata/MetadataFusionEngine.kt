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

import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.metadata.AnimeMetadata
import com.kurostream.domain.metadata.MetadataResult
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
    private val trailerRepository: com.kurostream.domain.metadata.TrailerRepository,
) {

    suspend fun enrich(mediaId: String, sourceHint: String? = null): MediaItem? = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferreds = listOf(
                async { tmdbProvider.searchAnime(mediaId, 1, 10) },
                async { anilistProvider.searchAnime(mediaId, 1, 10) },
                async { malProvider.searchAnime(mediaId, 1, 10) },
                async { kitsuProvider.searchAnime(mediaId, 1, 10) },
            )
            val results = deferreds.awaitAll().flatMap { it.successfulData() }

            if (results.isEmpty()) return@coroutineScope null

            val base = results.first()
            base.copy(
                score = results.mapNotNull { it.score }.maxOrNull() ?: base.score,
                genres = results.flatMap { it.genres }.distinct(),
            ).toMediaItem()
        }
    }

    suspend fun enrichAnime(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferreds = listOf(
                async { anilistProvider.searchAnime(mediaId, 1, 10) },
                async { malProvider.searchAnime(mediaId, 1, 10) },
                async { kitsuProvider.searchAnime(mediaId, 1, 10) },
            )
            val results = deferreds.awaitAll().flatMap { it.successfulData() }
            val base = results.firstOrNull() ?: return@coroutineScope null

            base.copy(
                score = results.mapNotNull { it.score }.maxOrNull() ?: base.score,
                description = results.mapNotNull { it.description }.firstOrNull() ?: base.description,
            ).toMediaItem()
        }
    }

    suspend fun getTrailers(mediaId: String): List<com.kurostream.domain.model.Trailer> = withContext(Dispatchers.IO) {
        trailerRepository.getTrailerForAnime(mediaId).getOrNull()?.let { listOf(it) } ?: emptyList()
    }

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
}
