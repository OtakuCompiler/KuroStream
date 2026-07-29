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

package com.kurostream.data.remote.api

import com.kurostream.data.remote.dto.opensubtitles.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OpenSubtitlesApi {
    @GET("subtitles")
    suspend fun searchSubtitles(
        @Query("query") query: String? = null,
        @Query("imdb_id") imdbId: String? = null,
        @Query("tmdb_id") tmdbId: Int? = null,
        @Query("moviehash") movieHash: String? = null,
        @Query("moviebytesize") movieByteSize: Long? = null,
        @Query("languages") languages: String? = null,
        @Query("year") year: Int? = null,
        @Query("season_number") seasonNumber: Int? = null,
        @Query("episode_number") episodeNumber: Int? = null,
        @Query("page") page: Int = 1
    ): Response<OpenSubtitlesSearchResponse>

    @GET("subtitles/{file_id}")
    suspend fun getSubtitleInfo(@Path("file_id") fileId: Int): Response<OpenSubtitlesSubtitleInfoResponse>

    @POST("download")
    suspend fun downloadSubtitle(@Body body: OpenSubtitlesDownloadRequest): Response<OpenSubtitlesDownloadResponse>

    @GET("download")
    suspend fun getDownloadLink(
        @Query("file_id") fileId: Int,
        @Query("token") token: String? = null
    ): Response<OpenSubtitlesDownloadResponse>

    @GET("infos/languages")
    suspend fun getLanguages(): Response<OpenSubtitlesLanguagesResponse>

    @GET("infos/formats")
    suspend fun getFormats(): Response<OpenSubtitlesFormatsResponse>

    @GET("subtitles/{file_id}/content")
    @Streaming
    suspend fun downloadSubtitleContent(@Path("file_id") fileId: Int): Response<ResponseBody>

    companion object { const val BASE_URL = "https://api.opensubtitles.com/api/v1/" }
}
