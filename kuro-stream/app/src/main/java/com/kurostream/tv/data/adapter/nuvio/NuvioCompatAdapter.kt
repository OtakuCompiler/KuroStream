package com.kurostream.tv.data.adapter.nuvio

import com.kurostream.tv.data.metadata.AIOMetadataSystem
import com.kurostream.tv.data.metadata.EnrichedMetadata
import com.kurostream.tv.data.metadata.UnifiedAnimeId
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeStatus
import com.kurostream.tv.domain.provider.ProviderStream
import com.kurostream.tv.domain.provider.StreamMetadata
import com.kurostream.tv.domain.provider.StreamQuality
import com.kurostream.tv.domain.provider.StreamType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NuvioTV compatibility adapter.
 *
 * Translates NuvioTV `MetaPreview` / `MetaDetail` data structures (Cinemeta /
 * Stremio JSON format) into Kuro Stream's internal [Anime] domain model, and
 * converts NuvioTV stream objects into [ProviderStream]s.
 *
 * Enriches missing metadata fields via [AIOMetadataSystem] (MAL / AniList / TMDB)
 * when a cross-platform ID can be extracted from the Nuvio link list.
 */
@Singleton
class NuvioCompatAdapter @Inject constructor(
    private val aioMetadata: AIOMetadataSystem
) {
    companion object {
        private const val PROVIDER_ID   = "nuvio"
        private const val PROVIDER_NAME = "NuvioTV"
    }

    // ─── MetaPreview → Anime ──────────────────────────────────────────────────

    /**
     * Convert a lightweight NuvioTV catalog card into a Kuro Stream [Anime].
     * Only fields that are always present in preview blobs are mapped; richer
     * data is available via [metaDetailToAnime].
     */
    fun metaPreviewToAnime(preview: NuvioMetaPreview): Anime = Anime(
        id            = preview.id,
        title         = preview.name,
        coverImage    = preview.poster ?: preview.background ?: "",
        bannerImage   = preview.background ?: preview.poster ?: "",
        synopsis      = preview.description,
        genres        = preview.genres ?: emptyList(),
        status        = AnimeStatus.UNKNOWN,
        totalEpisodes = null,
        rating        = preview.imdbRating?.replace(",", ".")?.toFloatOrNull(),
        popularity    = null,
        season        = null,
        year          = preview.year?.toIntOrNull(),
        isAdult       = false,
        studios       = emptyList()
    )

    /**
     * Convert a full NuvioTV detail meta into [Anime], enriched with AIO metadata.
     */
    suspend fun metaDetailToAnime(detail: NuvioMetaDetail): Anime {
        val ids = resolveIds(detail)
        val enriched: EnrichedMetadata? = if (ids.hasAnyId) {
            runCatching { aioMetadata.enrich(ids, detail.name) }.getOrNull()
        } else null

        return Anime(
            id            = detail.id,
            title         = enriched?.title ?: detail.name,
            titleEnglish  = enriched?.title?.takeIf { it != detail.name },
            titleJapanese = enriched?.titleNative,
            titleRomaji   = enriched?.titleRomaji,
            coverImage    = detail.poster ?: enriched?.coverImageUrl ?: "",
            bannerImage   = detail.background ?: enriched?.bannerImageUrl ?: "",
            synopsis      = enriched?.synopsis ?: detail.description,
            genres        = enriched?.genres ?: detail.genres ?: emptyList(),
            status        = enriched?.status?.toAnimeStatus() ?: AnimeStatus.UNKNOWN,
            totalEpisodes = enriched?.episodeCount ?: detail.videos?.size,
            rating        = enriched?.score,
            popularity    = null,
            season        = null,
            year          = enriched?.year ?: detail.year?.toIntOrNull(),
            isAdult       = enriched?.isAdult ?: false,
            studios       = enriched?.studios ?: emptyList()
        )
    }

    // ─── Stream conversion ────────────────────────────────────────────────────

    /**
     * Convert NuvioTV stream objects into [ProviderStream]s.
     * Entries with no usable URL or magnet hash are dropped.
     */
    fun nuvioStreamsToProviderStreams(
        streams: List<NuvioStream>,
        sourcePluginId: String = PROVIDER_ID
    ): List<ProviderStream> = streams.mapNotNull { s ->
        val url = s.url
            ?: s.infoHash?.let { "magnet:?xt=urn:btih:$it" }
            ?: return@mapNotNull null

        val quality = inferQuality(s.title ?: s.name ?: "")

        ProviderStream(
            providerId    = sourcePluginId,
            providerName  = PROVIDER_NAME,
            url           = url,
            quality       = quality,
            qualityLabel  = quality.name,
            type          = if (s.infoHash != null) StreamType.TORRENT else StreamType.DIRECT,
            language      = "ja",
            subtitles     = emptyList(),
            headers       = s.behaviorHints?.httpHeaders ?: emptyMap(),
            referer       = null,
            isWorking     = true,
            metadata      = StreamMetadata(
                codec    = s.title?.let { extractCodec(it) },
                fileSize = s.behaviorHints?.videoSize,
                fileName = s.behaviorHints?.filename
            )
        )
    }

    // ─── ID resolution ────────────────────────────────────────────────────────

    private suspend fun resolveIds(detail: NuvioMetaDetail): UnifiedAnimeId {
        val malId = detail.links
            ?.firstOrNull { it.category == "mal" || it.url.contains("myanimelist.net") }
            ?.url?.substringAfterLast("/")?.toLongOrNull()
        val anilistId = detail.links
            ?.firstOrNull { it.category == "anilist" || it.url.contains("anilist.co") }
            ?.url?.substringAfterLast("/")?.toLongOrNull()

        return if (malId != null || anilistId != null) {
            aioMetadata.resolveIds(anilistId = anilistId, malId = malId, title = detail.name)
        } else {
            // resolveIds() always returns a non-null UnifiedAnimeId; the fallback
            // here handles the case where the title fuzzy-match finds nothing.
            aioMetadata.resolveIds(title = detail.name).takeIf { it.hasAnyId }
                ?: UnifiedAnimeId(canonicalTitle = detail.name)
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private fun inferQuality(title: String): StreamQuality {
        val t = title.uppercase()
        return when {
            "4K" in t || "2160" in t || "UHD" in t -> StreamQuality.UHD_4K
            "1080" in t                             -> StreamQuality.HD_1080
            "720" in t                              -> StreamQuality.HD_720
            "480" in t                              -> StreamQuality.SD_480
            "360" in t                              -> StreamQuality.SD_360
            else                                    -> StreamQuality.UNKNOWN
        }
    }

    private fun extractCodec(title: String): String? {
        val t = title.uppercase()
        return when {
            "HEVC" in t || "H265" in t || "X265" in t -> "hevc"
            "H264" in t || "X264" in t || "AVC" in t  -> "h264"
            "AV1"  in t                                -> "av1"
            "VP9"  in t                                -> "vp9"
            else                                       -> null
        }
    }

    private fun String.toAnimeStatus(): AnimeStatus = when (lowercase()) {
        "finished airing", "finished"        -> AnimeStatus.FINISHED
        "currently airing", "ongoing"        -> AnimeStatus.RELEASING
        "not yet aired"                      -> AnimeStatus.NOT_YET_RELEASED
        else                                 -> AnimeStatus.UNKNOWN
    }
}

// ─── NuvioTV / Stremio Cinemeta-compatible data models ───────────────────────

/** Lightweight catalog card — used in home row lists. */
data class NuvioMetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val year: String? = null,
    val genres: List<String>? = null,
    val imdbRating: String? = null,
    val posterShape: String? = null
)

/** Full metadata detail fetched when the detail screen opens. */
data class NuvioMetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val year: String? = null,
    val genres: List<String>? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val released: String? = null,
    val status: String? = null,
    val country: String? = null,
    val language: String? = null,
    val links: List<NuvioLink>? = null,
    val videos: List<NuvioVideo>? = null,
    val trailers: List<NuvioTrailer>? = null
)

data class NuvioLink(val name: String, val category: String, val url: String)

data class NuvioVideo(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val released: String? = null,
    val thumbnail: String? = null,
    val overview: String? = null
)

data class NuvioTrailer(val source: String, val type: String = "Trailer")

/** Stream object as produced by NuvioTV addon handlers (Stremio protocol). */
data class NuvioStream(
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    val subtitles: List<NuvioSubtitle>? = null,
    val behaviorHints: NuvioBehaviorHints? = null
)

data class NuvioSubtitle(val id: String, val url: String, val lang: String)

data class NuvioBehaviorHints(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val httpHeaders: Map<String, String>? = null
)
