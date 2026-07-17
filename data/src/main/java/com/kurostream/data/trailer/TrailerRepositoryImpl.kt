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
import com.kurostream.data.remote.api.YouTubeApi
import com.kurostream.data.remote.dto.youtube.SearchResponse
import com.kurostream.domain.metadata.TrailerRepository
import com.kurostream.domain.model.Trailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrailerRepositoryImpl @Inject constructor(
    private val youTubeApi: YouTubeApi,
) : TrailerRepository {

    override suspend fun getTrailerForAnime(animeId: String): Result<Trailer> = withContext(Dispatchers.IO) {
        try {
            val searchQuery = "$animeId trailer"
            val youtubeResult: Response<SearchResponse> = youTubeApi.searchVideos(
                query = searchQuery,
                maxResults = 5,
                apiKey = "YOUTUBE_API_KEY" // Should be provided via DI
            )
            youtubeResult.body()?.items?.firstOrNull { it.id?.videoId != null }?.let { item ->
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
            val youtubeResult: Response<SearchResponse> = youTubeApi.searchVideos(
                query = query,
                maxResults = 10,
                apiKey = "YOUTUBE_API_KEY"
            )
            val trailers = youtubeResult.body()?.items?.mapNotNull { item ->
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
        return flow {
            emit(false) // Initial value
            // In a real implementation, this would observe the trailer cache
        }.distinctUntilChanged()
    }
}