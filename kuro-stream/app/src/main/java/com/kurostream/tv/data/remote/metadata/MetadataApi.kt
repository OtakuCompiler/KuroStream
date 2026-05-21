package com.kurostream.tv.data.remote.metadata

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cinemeta API Interface for anime metadata.
 * Cinemeta is a Stremio addon that provides metadata for movies and series.
 */
interface CinemetaApi {
    
    companion object {
        const val BASE_URL = "https://v3-cinemeta.strem.io/"
    }
    
    /**
     * Get anime metadata by IMDB ID.
     */
    @GET("meta/series/{imdbId}.json")
    suspend fun getSeriesMetadata(
        @Path("imdbId") imdbId: String
    ): Response<CinemetaMetaResponse>
    
    /**
     * Search for anime/series.
     */
    @GET("catalog/series/top.json")
    suspend fun getTopSeries(): Response<CinemetaCatalogResponse>
    
    /**
     * Get catalog with search query.
     */
    @GET("catalog/series/top/search={query}.json")
    suspend fun searchSeries(
        @Path("query") query: String
    ): Response<CinemetaCatalogResponse>
}

/**
 * Cinemeta metadata response.
 */
data class CinemetaMetaResponse(
    val meta: CinemetaMeta?
)

data class CinemetaMeta(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val logo: String?,
    val description: String?,
    val releaseInfo: String?,
    val imdbRating: String?,
    val runtime: String?,
    val genres: List<String>?,
    val cast: List<String>?,
    val director: List<String>?,
    val writer: List<String>?,
    val videos: List<CinemetaVideo>?,
    val links: List<CinemetaLink>?,
    val trailers: List<CinemetaTrailer>?
)

data class CinemetaVideo(
    val id: String,
    val title: String?,
    val season: Int?,
    val episode: Int?,
    val released: String?,
    val overview: String?,
    val thumbnail: String?
)

data class CinemetaLink(
    val name: String,
    val category: String,
    val url: String
)

data class CinemetaTrailer(
    val source: String,
    val type: String?
)

data class CinemetaCatalogResponse(
    val metas: List<CinemetaMeta>?
)

/**
 * Kitsu API Interface for anime metadata.
 * Kitsu is a popular anime database with extensive metadata.
 */
interface KitsuApi {
    
    companion object {
        const val BASE_URL = "https://kitsu.io/api/edge/"
    }
    
    /**
     * Search for anime.
     */
    @GET("anime")
    suspend fun searchAnime(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 20,
        @Query("page[offset]") offset: Int = 0
    ): Response<KitsuAnimeListResponse>
    
    /**
     * Get anime by ID.
     */
    @GET("anime/{id}")
    suspend fun getAnime(
        @Path("id") id: String
    ): Response<KitsuAnimeResponse>
    
    /**
     * Get trending anime.
     */
    @GET("trending/anime")
    suspend fun getTrendingAnime(
        @Query("limit") limit: Int = 20
    ): Response<KitsuAnimeListResponse>
    
    /**
     * Get anime episodes.
     */
    @GET("anime/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") animeId: String,
        @Query("page[limit]") limit: Int = 25,
        @Query("page[offset]") offset: Int = 0
    ): Response<KitsuEpisodeListResponse>
    
    /**
     * Get anime categories/genres.
     */
    @GET("anime/{id}/categories")
    suspend fun getCategories(
        @Path("id") animeId: String
    ): Response<KitsuCategoryListResponse>
    
    /**
     * Get anime streaming links.
     */
    @GET("anime/{id}/streaming-links")
    suspend fun getStreamingLinks(
        @Path("id") animeId: String
    ): Response<KitsuStreamingLinksResponse>
    
    /**
     * Get anime by season.
     */
    @GET("anime")
    suspend fun getAnimeBySeason(
        @Query("filter[seasonYear]") year: Int,
        @Query("filter[season]") season: String, // winter, spring, summer, fall
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-averageRating"
    ): Response<KitsuAnimeListResponse>
    
    /**
     * Get top rated anime.
     */
    @GET("anime")
    suspend fun getTopRated(
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-averageRating"
    ): Response<KitsuAnimeListResponse>
    
    /**
     * Get most popular anime.
     */
    @GET("anime")
    suspend fun getMostPopular(
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-userCount"
    ): Response<KitsuAnimeListResponse>
}

/**
 * Kitsu API Response models.
 */
data class KitsuAnimeListResponse(
    val data: List<KitsuAnimeData>?,
    val links: KitsuPaginationLinks?,
    val meta: KitsuMeta?
)

data class KitsuAnimeResponse(
    val data: KitsuAnimeData?
)

data class KitsuAnimeData(
    val id: String,
    val type: String,
    val attributes: KitsuAnimeAttributes?,
    val relationships: KitsuAnimeRelationships?
)

data class KitsuAnimeAttributes(
    val createdAt: String?,
    val updatedAt: String?,
    val slug: String?,
    val synopsis: String?,
    val description: String?,
    val coverImageTopOffset: Int?,
    val titles: KitsuTitles?,
    val canonicalTitle: String?,
    val abbreviatedTitles: List<String>?,
    val averageRating: String?,
    val ratingFrequencies: Map<String, String>?,
    val userCount: Int?,
    val favoritesCount: Int?,
    val startDate: String?,
    val endDate: String?,
    val nextRelease: String?,
    val popularityRank: Int?,
    val ratingRank: Int?,
    val ageRating: String?,
    val ageRatingGuide: String?,
    val subtype: String?, // TV, movie, OVA, ONA, special, music
    val status: String?, // current, finished, tba, unreleased, upcoming
    val tba: String?,
    val posterImage: KitsuImage?,
    val coverImage: KitsuImage?,
    val episodeCount: Int?,
    val episodeLength: Int?,
    val totalLength: Int?,
    val youtubeVideoId: String?,
    val showType: String?,
    val nsfw: Boolean?
)

data class KitsuTitles(
    val en: String?,
    val en_jp: String?,
    val ja_jp: String?,
    val en_us: String?
)

data class KitsuImage(
    val tiny: String?,
    val small: String?,
    val medium: String?,
    val large: String?,
    val original: String?
)

data class KitsuAnimeRelationships(
    val genres: KitsuRelationship?,
    val categories: KitsuRelationship?,
    val castings: KitsuRelationship?,
    val installments: KitsuRelationship?,
    val mappings: KitsuRelationship?,
    val reviews: KitsuRelationship?,
    val mediaRelationships: KitsuRelationship?,
    val characters: KitsuRelationship?,
    val staff: KitsuRelationship?,
    val productions: KitsuRelationship?,
    val quotes: KitsuRelationship?,
    val episodes: KitsuRelationship?,
    val streamingLinks: KitsuRelationship?,
    val animeProductions: KitsuRelationship?,
    val animeCharacters: KitsuRelationship?,
    val animeStaff: KitsuRelationship?
)

data class KitsuRelationship(
    val links: KitsuRelationshipLinks?
)

data class KitsuRelationshipLinks(
    val self: String?,
    val related: String?
)

data class KitsuPaginationLinks(
    val first: String?,
    val prev: String?,
    val next: String?,
    val last: String?
)

data class KitsuMeta(
    val count: Int?
)

data class KitsuEpisodeListResponse(
    val data: List<KitsuEpisodeData>?,
    val links: KitsuPaginationLinks?,
    val meta: KitsuMeta?
)

data class KitsuEpisodeData(
    val id: String,
    val type: String,
    val attributes: KitsuEpisodeAttributes?
)

data class KitsuEpisodeAttributes(
    val createdAt: String?,
    val updatedAt: String?,
    val titles: KitsuTitles?,
    val canonicalTitle: String?,
    val seasonNumber: Int?,
    val number: Int?,
    val relativeNumber: Int?,
    val synopsis: String?,
    val description: String?,
    val airdate: String?,
    val length: Int?,
    val thumbnail: KitsuImage?
)

data class KitsuCategoryListResponse(
    val data: List<KitsuCategoryData>?
)

data class KitsuCategoryData(
    val id: String,
    val type: String,
    val attributes: KitsuCategoryAttributes?
)

data class KitsuCategoryAttributes(
    val createdAt: String?,
    val updatedAt: String?,
    val title: String?,
    val description: String?,
    val totalMediaCount: Int?,
    val slug: String?,
    val nsfw: Boolean?,
    val childCount: Int?
)

data class KitsuStreamingLinksResponse(
    val data: List<KitsuStreamingLinkData>?
)

data class KitsuStreamingLinkData(
    val id: String,
    val type: String,
    val attributes: KitsuStreamingLinkAttributes?
)

data class KitsuStreamingLinkAttributes(
    val createdAt: String?,
    val updatedAt: String?,
    val url: String?,
    val subs: List<String>?,
    val dubs: List<String>?
)

/**
 * Metadata Repository for combining multiple metadata sources.
 */
interface MetadataRepository {
    
    /**
     * Search anime across all metadata sources.
     */
    suspend fun searchAnime(query: String, limit: Int = 20): Result<List<AnimeMetadata>>
    
    /**
     * Get detailed metadata for an anime.
     */
    suspend fun getAnimeMetadata(
        kitsuId: String? = null,
        imdbId: String? = null,
        malId: String? = null
    ): Result<AnimeMetadata?>
    
    /**
     * Get trending anime.
     */
    suspend fun getTrendingAnime(limit: Int = 20): Result<List<AnimeMetadata>>
    
    /**
     * Get top rated anime.
     */
    suspend fun getTopRatedAnime(limit: Int = 20): Result<List<AnimeMetadata>>
    
    /**
     * Get anime episodes.
     */
    suspend fun getEpisodes(kitsuId: String): Result<List<EpisodeMetadata>>
    
    /**
     * Get seasonal anime.
     */
    suspend fun getSeasonalAnime(
        year: Int,
        season: String,
        limit: Int = 20
    ): Result<List<AnimeMetadata>>
}

/**
 * Unified anime metadata model.
 */
data class AnimeMetadata(
    val kitsuId: String?,
    val imdbId: String?,
    val malId: String?,
    val title: String,
    val titleJapanese: String?,
    val titleEnglish: String?,
    val synopsis: String?,
    val posterUrl: String?,
    val coverUrl: String?,
    val trailerUrl: String?,
    val averageRating: Float?,
    val popularityRank: Int?,
    val ratingRank: Int?,
    val status: String?,
    val ageRating: String?,
    val subtype: String?,
    val episodeCount: Int?,
    val episodeLength: Int?,
    val startDate: String?,
    val endDate: String?,
    val genres: List<String>,
    val studios: List<String>,
    val isNsfw: Boolean
)

/**
 * Unified episode metadata model.
 */
data class EpisodeMetadata(
    val id: String,
    val number: Int,
    val seasonNumber: Int?,
    val title: String?,
    val synopsis: String?,
    val thumbnailUrl: String?,
    val airDate: String?,
    val durationMinutes: Int?
)
