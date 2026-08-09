// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.aggregator

import com.kurostream.extensions.anilist.AniListTVAdapter
import com.kurostream.extensions.kitsu.KitsuAdapter
import com.kurostream.extensions.mal.MalAdapter
import com.kurostream.extensions.simkl.SimklAdapter
import com.kurostream.extensions.tmdb.TmdbAdapter
import com.kurostream.extensions.tvdb.TvdbAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native metadata fusion — queries multiple providers in parallel and
 * deduplicates by canonical title/year. Designed to be the home-screen
 * fallback when the local media DB is empty (no add-ons installed yet).
 *
 * Providers covered:
 *  - AniList  (anime, GraphQL, no auth)
 *  - MAL/JIKAN (anime/manga, public)
 *  - TMDB     (movies/TV, requires API key for data; poster URLs are free)
 *  - TVDB     (TV series, requires PIN)
 *  - Kitsu    (anime/manga, no auth)
 *  - Simkl    (movies/TV, requires client ID)
 */
@Singleton
class MetadataFusionService @Inject constructor(
    private val anilist: AniListTVAdapter,
    private val mal: MalAdapter,
    private val tmdb: TmdbAdapter,
    private val tvdb: TvdbAdapter,
    private val kitsu: KitsuAdapter,
    private val simkl: SimklAdapter,
) {

    enum class Provider(val label: String) {
        ANILIST("AniList"),
        MAL("MAL"),
        TMDB("TMDB"),
        TVDB("TVDB"),
        KITSU("Kitsu"),
        SIMKL("Simkl"),
    }

    data class MetadataRow(
        val provider: Provider,
        val externalId: String,
        val title: String,
        val year: Int? = null,
        val posterUrl: String? = null,
        val backdropUrl: String? = null,
        val overview: String? = null,
        val score: Float = 0f,
        val genres: List<String> = emptyList(),
        val type: String = "unknown",
    )

    data class FusedHomeFeed(
        val trending: List<MetadataRow> = emptyList(),
        val movies: List<MetadataRow> = emptyList(),
        val tv: List<MetadataRow> = emptyList(),
        val anime: List<MetadataRow> = emptyList(),
        val topRated: List<MetadataRow> = emptyList(),
        val providersQueried: List<Provider> = emptyList(),
        val providersFailed: List<Provider> = emptyList(),
    )

    /**
     * Query all metadata providers in parallel and return a fused feed.
     * Best-effort: providers that error or aren't configured are skipped.
     * Each provider has a 4s per-query budget so the home screen never
     * hangs on a slow public API.
     */
    suspend fun loadFusedFeed(): FusedHomeFeed = withContext(Dispatchers.IO) {
        coroutineScope {
            val aniTrending = async { safeAniList { anilist.getTrending(perPage = 15).getOrThrow() } }
            val aniCurrent  = async { safeAniList { anilist.getCurrentSeason(perPage = 15).getOrThrow() } }
            val aniTopRated = async { safeAniList { anilist.getTopRated(perPage = 15).getOrThrow() } }

            val malSeasonal = async { safeMal { mal.seasonalNow(limit = 15).getOrThrow() } }
            val malPopular  = async { safeMal { mal.popular(limit = 15).getOrThrow() } }
            val malTop      = async { safeMal { mal.topAnime(limit = 15).getOrThrow() } }

            val kitsuTrending = async { safeKitsu { kitsu.trending(limit = 15).getOrThrow() } }
            val kitsuSeasonal = async { safeKitsu { kitsu.currentSeason(limit = 15).getOrThrow() } }
            val kitsuUpcoming = async { safeKitsu { kitsu.upcoming(limit = 15).getOrThrow() } }

            val tmdbTrending  = async { safeTmdb {
                if (tmdb.isConfigured) tmdb.trending().getOrThrow().results.map { tmdb.tmdbToRow(it) }
                else emptyList()
            } }
            val tmdbTopMovies = async { safeTmdb {
                if (tmdb.isConfigured) tmdb.topRatedMovies().getOrThrow().results.map { tmdb.tmdbToRow(it) }
                else emptyList()
            } }
            val tmdbTopTv     = async { safeTmdb {
                if (tmdb.isConfigured) tmdb.topRatedTv().getOrThrow().results.map { tmdb.tmdbToRow(it) }
                else emptyList()
            } }

            val tvdbTrending = async { safeTvdb {
                if (tvdb.isConfigured) tvdb.trending().getOrThrow()
                else emptyList()
            } }

            val simklTrending = async { safeSimkl {
                if (simkl.isConfigured) simkl.search("").getOrThrow().map { it.simklToRow() }
                else emptyList()
            } }

            val all = listOf(aniTrending, aniCurrent, aniTopRated,
                             malSeasonal, malPopular, malTop,
                             kitsuTrending, kitsuSeasonal, kitsuUpcoming,
                             tmdbTrending, tmdbTopMovies, tmdbTopTv,
                             tvdbTrending, simklTrending)

            val rows = all.flatMap { it.await() }
            val ok = rows.map { it.provider }.distinct()
            val failed = Provider.values().filterNot { it in ok }

            val byKey = LinkedHashMap<String, MetadataRow>()
            rows.forEach { r ->
                val key = "${normalizeTitle(r.title)}|${r.year ?: 0}"
                val existing = byKey[key]
                byKey[key] = if (existing == null || r.score > existing.score) r else existing
            }
            val deduped = byKey.values.toList()

            FusedHomeFeed(
                trending = deduped.sortedByDescending { it.score }.take(15),
                movies = deduped.filter { it.type == "movie" }.take(15),
                tv = deduped.filter { it.type == "tv" || it.type == "series" }.take(15),
                anime = deduped.filter { it.type == "anime" }.take(15),
                topRated = deduped.sortedByDescending { it.score }.take(15),
                providersQueried = ok,
                providersFailed = failed,
            )
        }
    }

    private suspend fun safeAniList(block: suspend () -> List<com.kurostream.extensions.anilist.AniListMedia>) =
        safeList(Provider.ANILIST) { block().map { it.toRow() } }

    private suspend fun safeMal(block: suspend () -> List<com.kurostream.extensions.mal.MalAnime>) =
        safeList(Provider.MAL) { block().map { it.toRow() } }

    private suspend fun safeKitsu(block: suspend () -> List<com.kurostream.extensions.kitsu.KitsuAnime>) =
        safeList(Provider.KITSU) { block().map { it.toRow() } }

    private suspend fun safeTmdb(block: suspend () -> List<MetadataRow>) =
        safeList(Provider.TMDB, block)

    private suspend fun safeTvdb(block: suspend () -> List<com.kurostream.extensions.tvdb.TvdbResult>) =
        safeList(Provider.TVDB) { block().map { it.toRow() } }

    private suspend fun safeSimkl(block: suspend () -> List<MetadataRow>) =
        safeList(Provider.SIMKL, block)

    private suspend fun safeList(provider: Provider, block: suspend () -> List<MetadataRow>): List<MetadataRow> =
        runCatching {
            kotlinx.coroutines.withTimeout(4_000) { block() }
        }.onFailure { Timber.w(it, "MetadataFusionService: $provider failed") }
            .getOrDefault(emptyList())

    private fun normalizeTitle(t: String): String =
        t.lowercase().replace(Regex("[^a-z0-9]"), "").take(40)
}

// ── Provider → MetadataRow converters (no app-level imports — sanitization
//    happens at the HomeViewModel boundary instead) ─────────────────────────────

fun com.kurostream.extensions.tmdb.TmdbAdapter.tmdbToRow(r: com.kurostream.extensions.tmdb.TmdbResult): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.TMDB,
        externalId = "tmdb:${r.id}",
        title = r.displayTitle,
        year = r.displayDate.take(4).toIntOrNull(),
        posterUrl = posterUrl(r.posterPath, "w500"),
        backdropUrl = backdropUrl(r.backdropPath, "w1280"),
        overview = r.overview,
        score = r.voteAverage,
        genres = emptyList(),
        type = r.mediaType ?: if (r.firstAirDate != null) "tv" else "movie",
    )

private fun com.kurostream.extensions.anilist.AniListMedia.toRow(): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.ANILIST,
        externalId = "anilist:$id",
        title = displayTitle,
        year = seasonYear,
        posterUrl = posterUrl,
        backdropUrl = bannerImage,
        overview = description,
        score = scoreFloat,
        genres = genres,
        type = "anime",
    )

private fun com.kurostream.extensions.mal.MalAnime.toRow(): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.MAL,
        externalId = "mal:$malId",
        title = displayTitle,
        year = year,
        posterUrl = images.jpg.largeImageUrl ?: images.jpg.imageUrl,
        backdropUrl = images.jpg.largeImageUrl,
        overview = synopsis,
        score = score ?: 0f,
        genres = genres.map { it.name },
        type = "anime",
    )

private fun com.kurostream.extensions.kitsu.KitsuAnime.toRow(): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.KITSU,
        externalId = "kitsu:$id",
        title = attributes.displayTitle,
        year = attributes.startDate?.take(4)?.toIntOrNull(),
        posterUrl = attributes.posterImage?.large ?: attributes.posterImage?.medium,
        backdropUrl = attributes.coverImage?.large ?: attributes.coverImage?.original,
        overview = attributes.synopsis,
        score = attributes.scoreFloat,
        genres = emptyList(),
        type = "anime",
    )

private fun com.kurostream.extensions.tvdb.TvdbResult.toRow(): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.TVDB,
        externalId = "tvdb:$id",
        title = name,
        year = year?.toIntOrNull(),
        posterUrl = image,
        backdropUrl = image,
        overview = overview,
        score = score,
        genres = genres,
        type = "tv",
    )

fun com.kurostream.extensions.simkl.SimklSearchResult.simklToRow(): MetadataFusionService.MetadataRow =
    MetadataFusionService.MetadataRow(
        provider = MetadataFusionService.Provider.SIMKL,
        externalId = "simkl:${imdbId ?: title}",
        title = title,
        year = year,
        posterUrl = poster,
        backdropUrl = fanart,
        overview = null,
        score = ratings?.simkl?.rating ?: 0f,
        genres = emptyList(),
        type = if (type.equals("movie", ignoreCase = true)) "movie" else "tv",
    )
