package com.kurostream.app.metadata

import com.kurostream.data.metadata.UnifiedMetadataRepositoryImpl
import com.kurostream.domain.metadata.UnifiedAnimeDetails
import com.kurostream.domain.metadata.MetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for metadata retrieval. Delegates to the data layer's
 * [UnifiedMetadataRepositoryImpl] which aggregates multiple providers
 * (AniList, MAL, Kitsu, TMDB, TVDB, IMDb) with automatic fallback.
 */
@Singleton
class UnifiedMetadataRepository @Inject constructor(
    private val repositoryImpl: UnifiedMetadataRepositoryImpl
) {
    /**
     * Fetch metadata for a given media ID as a JSON string.
     * Returns empty string if no metadata could be retrieved.
     */
    suspend fun fetchMetadata(mediaId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val result = repositoryImpl.getAnimeDetails(mediaId)
                when (result) {
                    is MetadataResult.Success -> formatMetadata(result.data)
                    is MetadataResult.Partial -> formatMetadata(result.data)
                    is MetadataResult.Error -> ""
                    is MetadataResult.NotFound -> ""
                    is MetadataResult.RateLimited -> ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Get parsed anime details if available.
     */
    suspend fun getDetails(mediaId: String): UnifiedAnimeDetails? {
        return withContext(Dispatchers.IO) {
            try {
                val result = repositoryImpl.getAnimeDetails(mediaId)
                when (result) {
                    is MetadataResult.Success -> result.data
                    is MetadataResult.Partial -> result.data
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Search for anime across all enabled providers.
     */
    suspend fun search(query: String, page: Int = 1, limit: Int = 20): List<UnifiedAnimeDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val result = repositoryImpl.searchAnime(query, page, limit)
                when (result) {
                    is MetadataResult.Success -> result.data
                    is MetadataResult.Partial -> result.data
                    else -> emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Get trending anime from all providers.
     */
    suspend fun getTrending(limit: Int = 20): List<UnifiedAnimeDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val result = repositoryImpl.getTrendingAnime(limit)
                when (result) {
                    is MetadataResult.Success -> result.data
                    is MetadataResult.Partial -> result.data
                    else -> emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun formatMetadata(details: UnifiedAnimeDetails): String {
        return buildString {
            append("Title: ${details.title}")
            if (details.titleEnglish != null && details.titleEnglish != details.title) {
                append("\nEnglish: ${details.titleEnglish}")
            }
            if (!details.description.isNullOrBlank()) {
                append("\n\n${details.description}")
            }
            if (details.genres.isNotEmpty()) {
                append("\n\nGenres: ${details.genres.joinToString(", ")}")
            }
            if (details.score != null) {
                append("\nScore: ${"%.1f".format(details.score)}")
            }
            if (details.episodeCount != null) {
                append("\nEpisodes: ${details.episodeCount}")
            }
            if (details.seasonYear != null) {
                append("\nYear: ${details.seasonYear}")
            }
        }
    }
}
