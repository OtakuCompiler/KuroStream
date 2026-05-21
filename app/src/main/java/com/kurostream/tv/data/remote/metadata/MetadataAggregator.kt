package com.kurostream.tv.data.remote.metadata

import timber.log.Timber

/**
 * Aggregates anime metadata from multiple sources (Kitsu + Cinemeta).
 *
 * Instantiated manually in [com.kurostream.tv.di.RepositoryModule] so that
 * Hilt has a single, unambiguous binding — do NOT add @Inject or @Singleton
 * here or it will create a duplicate binding and fail at compile time.
 *
 * Strategy:
 *  1. Kitsu is queried first — it has richer anime-specific data (ratings,
 *     episode counts, seasonal info).
 *  2. Cinemeta fills in the gap for titles that have an IMDB ID but may not
 *     be in Kitsu's anime-only catalog (e.g. live-action adaptations, movies).
 *  3. Results are de-duplicated by title similarity before being returned.
 */
class MetadataAggregator(
    private val cinemetaService: CinemetaService,
    private val kitsuService: KitsuService
) {
    companion object {
        private const val TAG = "MetadataAggregator"
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchAnime(query: String, limit: Int = 20): Result<List<AnimeMetadata>> {
        return try {
            val kitsuResults = kitsuService.searchAnime(query, limit)
            val cinemetaResults = if (kitsuResults.size < limit / 2) {
                cinemetaService.searchSeries(query)
            } else {
                emptyList()
            }
            val merged = mergeAndDeduplicate(kitsuResults, cinemetaResults, limit)
            Result.success(merged)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Search failed for '$query'")
            Result.failure(e)
        }
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    suspend fun getTrendingAnime(limit: Int = 20): Result<List<AnimeMetadata>> {
        return try {
            Result.success(kitsuService.getTrendingAnime(limit))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Trending fetch failed")
            Result.failure(e)
        }
    }

    suspend fun getTopRatedAnime(limit: Int = 20): Result<List<AnimeMetadata>> {
        return try {
            Result.success(kitsuService.getTopRated(limit))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Top-rated fetch failed")
            Result.failure(e)
        }
    }

    suspend fun getSeasonalAnime(
        year: Int,
        season: String,
        limit: Int = 20
    ): Result<List<AnimeMetadata>> {
        return try {
            Result.success(kitsuService.getSeasonalAnime(year, season, limit))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Seasonal fetch failed ($year $season)")
            Result.failure(e)
        }
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    suspend fun getAnimeMetadata(
        kitsuId: String? = null,
        imdbId: String? = null,
        malId: String? = null
    ): Result<AnimeMetadata?> {
        return try {
            val result = when {
                kitsuId != null -> kitsuService.getAnime(kitsuId)
                imdbId  != null -> cinemetaService.getSeriesMetadata(imdbId)
                else            -> null
            }
            Result.success(result)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Metadata detail fetch failed")
            Result.failure(e)
        }
    }

    suspend fun getEpisodes(kitsuId: String): Result<List<EpisodeMetadata>> {
        return try {
            Result.success(kitsuService.getEpisodes(kitsuId))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Episode fetch failed for kitsuId=$kitsuId")
            Result.failure(e)
        }
    }

    // ── Merge / de-duplicate ──────────────────────────────────────────────────

    private fun mergeAndDeduplicate(
        primary: List<AnimeMetadata>,
        secondary: List<AnimeMetadata>,
        limit: Int
    ): List<AnimeMetadata> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<AnimeMetadata>()

        for (item in primary) {
            val key = normaliseTitle(item.title)
            if (seen.add(key)) merged.add(item)
        }

        for (item in secondary) {
            val key = normaliseTitle(item.title)
            if (seen.add(key) && merged.size < limit) merged.add(item)
        }

        return merged.take(limit)
    }

    private fun normaliseTitle(title: String): String =
        title.lowercase().replace(Regex("[^a-z0-9]"), "").take(30)
}
