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

import com.kurostream.data.remote.dto.tmdb.DiscoverResponse
import com.kurostream.data.remote.dto.tmdb.ExternalIdResponse
import com.kurostream.data.remote.dto.tmdb.SearchResponse
import com.kurostream.data.remote.dto.tmdb.TrendingResponse
import com.kurostream.data.remote.dto.tmdb.TvShow
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("append_to_response") appendToResponse: String = "external_ids,credits,videos,content_ratings",
    ): Response<TvShow>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
    ): Response<SearchResponse>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("with_keywords") withKeywords: String = "anime",
        @Query("first_air_date_year") firstAirDateYear: Int? = null,
        @Query("air_date.gte") airDateGte: String? = null,
        @Query("air_date.lte") airDateLte: String? = null,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US",
    ): Response<DiscoverResponse>

    @GET("find/{external_id}")
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("external_source") externalSource: String,
        @Query("language") language: String = "en-US",
    ): Response<ExternalIdResponse>

    @GET("trending/tv/week")
    suspend fun getTrendingTv(
        @Query("language") language: String = "en-US",
    ): Response<TrendingResponse>
}
