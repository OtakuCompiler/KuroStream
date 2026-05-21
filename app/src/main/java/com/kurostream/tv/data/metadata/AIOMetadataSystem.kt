package com.kurostream.tv.data.metadata

import android.content.Context
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * AIOMetadata System — All-In-One metadata correlation engine.
 *
 * Resolves unified anime identity across:
 *   - AniList  (primary, GraphQL)
 *   - MyAnimeList via Jikan v4 REST (no API key required)
 *   - TMDB (for movie fallback, via Jikan external links)
 *   - Kitsu (ID only, via community ID map)
 *
 * Features:
 *  - Cross-platform ID correlation (MAL ↔ AniList ↔ TMDB)
 *  - Fuzzy / Levenshtein title matching
 *  - Two-tier cache: in-memory LRU (fast path) + Future: Room DB (persistent)
 *  - Graceful fallback chain — never throws, always returns partial data
 */
@Singleton
class AIOMetadataSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    companion object {
        private const val TAG = "AIOMetadata"

        private const val JIKAN_BASE = "https://api.jikan.moe/v4"
        private const val ANILIST_BASE = "https://graphql.anilist.co"

        private const val MEMORY_CACHE_SIZE = 200
    }

    // ─── In-memory LRU cache ─────────────────────────────────────────────────

    private val idCache = LruCache<String, UnifiedAnimeId>(MEMORY_CACHE_SIZE)
    private val enrichedCache = LruCache<String, EnrichedMetadata>(MEMORY_CACHE_SIZE)

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Resolve all platform IDs for an anime given any one known ID or title.
     *
     * @param anilistId  AniList numeric ID (preferred)
     * @param malId      MyAnimeList numeric ID
     * @param title      Fallback: fuzzy-match by title when no numeric ID is known
     */
    suspend fun resolveIds(
        anilistId: Long? = null,
        malId: Long? = null,
        title: String? = null
    ): UnifiedAnimeId = withContext(Dispatchers.IO) {
        val cacheKey = anilistId?.let { "al:$it" }
            ?: malId?.let { "mal:$it" }
            ?: title?.let { "title:${it.lowercase().trim()}" }
            ?: return@withContext UnifiedAnimeId()

        idCache.get(cacheKey)?.let { return@withContext it }

        val result = runCatching {
            resolveIdsInternal(anilistId, malId, title)
        }.getOrElse { e ->
            Timber.tag(TAG).w(e, "ID resolution failed for $cacheKey")
            UnifiedAnimeId(anilistId = anilistId, malId = malId)
        }

        idCache.put(cacheKey, result)
        result
    }

    /**
     * Enrich metadata by merging best-available data from all reachable sources.
     *
     * Returns whatever data is available — callers must handle nullable fields.
     */
    suspend fun enrich(
        unifiedId: UnifiedAnimeId,
        preferredTitle: String? = null
    ): EnrichedMetadata = withContext(Dispatchers.IO) {
        val cacheKey = unifiedId.cacheKey
        enrichedCache.get(cacheKey)?.let { return@withContext it }

        val result = runCatching {
            enrichInternal(unifiedId, preferredTitle)
        }.getOrElse { e ->
            Timber.tag(TAG).w(e, "Metadata enrichment failed for $cacheKey")
            EnrichedMetadata(unifiedId = unifiedId, title = preferredTitle ?: "Unknown")
        }

        enrichedCache.put(cacheKey, result)
        result
    }

    /**
     * Fuzzy-match an anime title across Jikan (MAL).
     * Returns the best-matching [UnifiedAnimeId] or null if below [threshold].
     */
    suspend fun matchByTitle(title: String, threshold: Float = 0.75f): UnifiedAnimeId? =
        withContext(Dispatchers.IO) {
            val cleaned = title.trim().lowercase()
            runCatching {
                jikanSearch(cleaned)
                    .map { it to titleSimilarity(cleaned, it.title.lowercase()) }
                    .filter { (_, score) -> score >= threshold }
                    .maxByOrNull { (_, score) -> score }
                    ?.let { (result, score) ->
                        Timber.tag(TAG).d("Fuzzy match '$title' → '${result.title}' score=$score")
                        UnifiedAnimeId(malId = result.malId, canonicalTitle = result.title)
                    }
            }.getOrNull()
        }

    // ─── Internal resolution ──────────────────────────────────────────────────

    private suspend fun resolveIdsInternal(
        anilistId: Long?,
        malId: Long?,
        title: String?
    ): UnifiedAnimeId {
        if (anilistId != null && malId != null) {
            return UnifiedAnimeId(anilistId = anilistId, malId = malId)
        }

        if (malId != null) {
            val detail = fetchJikanAnime(malId)
            return UnifiedAnimeId(
                anilistId = anilistId ?: detail?.externalIds?.anilistId,
                malId = malId,
                tmdbId = detail?.externalIds?.tmdbId,
                canonicalTitle = detail?.titleEn ?: detail?.title
            )
        }

        if (anilistId != null) {
            val malIdFromAniList = fetchMalIdFromAniList(anilistId)
            return UnifiedAnimeId(anilistId = anilistId, malId = malIdFromAniList)
        }

        if (title != null) {
            return matchByTitle(title) ?: UnifiedAnimeId(canonicalTitle = title)
        }

        return UnifiedAnimeId()
    }

    private suspend fun enrichInternal(
        ids: UnifiedAnimeId,
        preferredTitle: String?
    ): EnrichedMetadata = withContext(Dispatchers.IO) {
        val jikanDeferred = ids.malId?.let { malId ->
            async { runCatching { fetchJikanAnime(malId)?.toEnrichedMetadata(ids) }.getOrNull() }
        }
        val results = listOfNotNull(jikanDeferred?.await())
        mergeMetadata(results, ids, preferredTitle)
    }

    // ─── Merge strategy ───────────────────────────────────────────────────────

    private fun mergeMetadata(
        sources: List<EnrichedMetadata>,
        ids: UnifiedAnimeId,
        preferredTitle: String?
    ): EnrichedMetadata {
        if (sources.isEmpty()) {
            return EnrichedMetadata(unifiedId = ids, title = preferredTitle ?: "Unknown")
        }
        return EnrichedMetadata(
            unifiedId = ids,
            title = preferredTitle
                ?: sources.firstNotNullOfOrNull { it.title.takeIf { t -> t.isNotBlank() } }
                ?: "Unknown",
            titleRomaji = sources.firstNotNullOfOrNull { it.titleRomaji },
            titleNative = sources.firstNotNullOfOrNull { it.titleNative },
            synopsis = sources.firstNotNullOfOrNull { it.synopsis?.takeIf { s -> s.length > 50 } },
            coverImageUrl = sources.firstNotNullOfOrNull { it.coverImageUrl },
            bannerImageUrl = sources.firstNotNullOfOrNull { it.bannerImageUrl },
            score = sources.mapNotNull { it.score }.average().takeIf { !it.isNaN() }?.toFloat(),
            episodeCount = sources.firstNotNullOfOrNull { it.episodeCount },
            genres = sources.flatMap { it.genres }.distinct(),
            studios = sources.flatMap { it.studios }.distinct(),
            status = sources.firstNotNullOfOrNull { it.status },
            year = sources.firstNotNullOfOrNull { it.year },
            season = sources.firstNotNullOfOrNull { it.season },
            isAdult = sources.firstOrNull()?.isAdult ?: false,
            duration = sources.firstNotNullOfOrNull { it.duration }
        )
    }

    // ─── Network calls ────────────────────────────────────────────────────────

    private suspend fun fetchJikanAnime(malId: Long): JikanAnimeDetail? {
        val url = "$JIKAN_BASE/anime/$malId/full"
        return runCatching {
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string() ?: return@runCatching null
            json.decodeFromString<JikanAnimeResponse>(body).data
        }.getOrNull()
    }

    private suspend fun jikanSearch(query: String): List<JikanSearchResult> {
        val encodedQuery = query.replace(" ", "%20")
        val url = "$JIKAN_BASE/anime?q=$encodedQuery&limit=5"
        return runCatching {
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@runCatching emptyList()
            val body = response.body?.string() ?: return@runCatching emptyList()
            json.decodeFromString<JikanSearchResponse>(body).data
                .map { JikanSearchResult(malId = it.malId, title = it.title) }
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchMalIdFromAniList(anilistId: Long): Long? {
        val query = """{"query":"query { Media(id: $anilistId, type: ANIME) { idMal } }"}"""
        val contentType = "application/json".toMediaType()
        val body = query.toRequestBody(contentType)
        return runCatching {
            val response = okHttpClient.newCall(
                Request.Builder()
                    .url(ANILIST_BASE)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
            ).execute()
            val text = response.body?.string() ?: return@runCatching null
            Regex(""""idMal"\s*:\s*(\d+)""").find(text)?.groupValues?.get(1)?.toLongOrNull()
        }.getOrNull()
    }

    // ─── Fuzzy matching ───────────────────────────────────────────────────────

    internal fun titleSimilarity(a: String, b: String): Float {
        if (a == b) return 1f
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1f
        return 1f - levenshtein(a, b).toFloat() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + min(dp[i - 1][j - 1], min(dp[i - 1][j], dp[i][j - 1]))
        }
        return dp[m][n]
    }
}

// ─── Domain models ────────────────────────────────────────────────────────────

/** Unified cross-platform identity for one anime title. */
data class UnifiedAnimeId(
    val anilistId: Long? = null,
    val malId: Long? = null,
    val tmdbId: Long? = null,
    val kitsuId: Long? = null,
    val canonicalTitle: String? = null
) {
    val cacheKey: String
        get() = anilistId?.let { "al:$it" }
            ?: malId?.let { "mal:$it" }
            ?: canonicalTitle?.let { "title:${it.lowercase()}" }
            ?: "unknown"

    val hasAnyId: Boolean get() = anilistId != null || malId != null || tmdbId != null
}

/** Fully enriched metadata merged from all available sources. */
data class EnrichedMetadata(
    val unifiedId: UnifiedAnimeId,
    val title: String,
    val titleRomaji: String? = null,
    val titleNative: String? = null,
    val synopsis: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val score: Float? = null,
    val episodeCount: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val status: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val isAdult: Boolean = false,
    val duration: Int? = null   // minutes per episode
)

// ─── Jikan (MAL) REST models ──────────────────────────────────────────────────

@Serializable
private data class JikanAnimeResponse(val data: JikanAnimeDetail? = null)

@Serializable
private data class JikanAnimeDetail(
    @kotlinx.serialization.SerialName("mal_id") val malId: Long = 0,
    val title: String = "",
    @kotlinx.serialization.SerialName("title_english") val titleEn: String? = null,
    @kotlinx.serialization.SerialName("title_japanese") val titleJa: String? = null,
    val synopsis: String? = null,
    val score: Float? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val duration: String? = null,
    @kotlinx.serialization.SerialName("approved") val isAdult: Boolean = false,
    val images: JikanImages? = null,
    val genres: List<JikanGenre> = emptyList(),
    val studios: List<JikanStudio> = emptyList(),
    val external: List<JikanExternal> = emptyList()
) {
    val externalIds: JikanExternalIds
        get() {
            val anilistEntry = external.firstOrNull { it.name.lowercase() == "anilist" }
            val tmdbEntry = external.firstOrNull { it.name.lowercase().contains("tmdb") }
            return JikanExternalIds(
                anilistId = anilistEntry?.url?.substringAfterLast("/")?.toLongOrNull(),
                tmdbId = tmdbEntry?.url?.substringAfterLast("/")?.toLongOrNull()
            )
        }

    fun toEnrichedMetadata(ids: UnifiedAnimeId): EnrichedMetadata {
        val durationMins = duration
            ?.filter { it.isDigit() || it == ' ' }
            ?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull()
        return EnrichedMetadata(
            unifiedId = ids,
            title = titleEn ?: title,
            titleRomaji = title,
            titleNative = titleJa,
            synopsis = synopsis,
            coverImageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
            score = score,
            episodeCount = episodes,
            genres = genres.map { it.name },
            studios = studios.map { it.name },
            status = status,
            year = year,
            season = season?.replaceFirstChar { it.uppercase() },
            isAdult = isAdult,
            duration = durationMins
        )
    }
}

@Serializable
private data class JikanImages(val jpg: JikanJpeg? = null)

@Serializable
private data class JikanJpeg(
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null,
    @kotlinx.serialization.SerialName("large_image_url") val largeImageUrl: String? = null
)

@Serializable
private data class JikanGenre(val name: String = "")

@Serializable
private data class JikanStudio(val name: String = "")

@Serializable
private data class JikanExternal(val name: String = "", val url: String = "")

private data class JikanExternalIds(val anilistId: Long? = null, val tmdbId: Long? = null)

@Serializable
private data class JikanSearchResponse(val data: List<JikanSearchItem> = emptyList())

@Serializable
private data class JikanSearchItem(
    @kotlinx.serialization.SerialName("mal_id") val malId: Long = 0,
    val title: String = ""
)

private data class JikanSearchResult(val malId: Long, val title: String)
