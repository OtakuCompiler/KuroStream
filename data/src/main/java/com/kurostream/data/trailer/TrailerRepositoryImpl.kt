// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.data.trailer

import com.kurostream.core.common.result.Result
import com.kurostream.data.remote.dto.tmdb.TmdbDtos
import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.data.remote.api.YouTubeApi
import com.kurostream.domain.metadata.TrailerRepository
import com.kurostream.domain.model.Trailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrailerRepositoryImpl @Inject constructor(
    private val youTubeApi: YouTubeApi,
    private val tmdbApi: TmdbApi,
) : TrailerRepository {

    override suspend fun getTrailerForAnime(animeId: String): Result<Trailer> = withContext(Dispatchers.IO) {
        try {
            // Try TMDB first
            val tmdbResult = tmdbApi.getExternalIds(animeId)
            if (tmdbResult.isSuccessful && tmdbResult.body()?.youtubeKey != null) {
                val youtubeKey = tmdbResult.body()!!.youtubeKey!!
                return@withContext Result.success(Trailer(
                    url = "https://www.youtube.com/watch?v=$youtubeKey",
                    title = "Official Trailer",
                    thumbnailUrl = "https://img.youtube.com/vi/$youtubeKey/hqdefault.jpg",
                    source = "tmdb",
                    publishedAt = null
                ))
            }

            // Fallback to YouTube search
            val searchQuery = "$animeId trailer"
            val youtubeResult = youTubeApi.searchVideos(searchQuery, maxResults = 5)
            youtubeResult.data?.items?.firstOrNull { it.id?.videoId != null }?.let { item ->
                val url = "https://www.youtube.com/watch?v=${item.id.videoId}"
                return@withContext Result.success(Trailer(
                    url = url,
                    title = item.snippet.title,
                    thumbnailUrl = item.snippet.thumbnails?.high?.url,
                    source = "youtube",
                    publishedAt = item.snippet.publishedAt
                ))
            }

            Result.error(Exception("No trailer found"))
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun searchTrailers(query: String): Result<List<Trailer>> = withContext(Dispatchers.IO) {
        try {
            val youtubeResult = youTubeApi.searchVideos(query, maxResults = 10)
            val trailers = youtubeResult.data?.items?.mapNotNull { item ->
                item.id?.videoId?.let { videoId ->
                    Trailer(
                        url = "https://www.youtube.com/watch?v=$videoId",
                        title = item.snippet.title,
                        thumbnailUrl = item.snippet.thumbnails?.high?.url,
                        source = "youtube",
                        publishedAt = item.snippet.publishedAt
                    )
                }
            } ?: emptyList()

            Result.success(trailers)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun observeTrailerAvailability(animeId: String): Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            emit(false) // Initial value
            // In a real implementation, this would observe the trailer cache
        }.distinctUntilChanged()
    }
}