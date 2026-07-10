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

import com.kurostream.data.remote.dto.tvdb.TvdbDtos
import com.kurostream.data.remote.dto.imdb.ImdbDtos
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TvdbApi {
    @GET("series/{id}")
    suspend fun getSeries(
        @Path("id") id: String,
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/json",
    ): Response<TvdbDtos.SeriesResponse>

    @GET("search/series")
    suspend fun searchSeries(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/json",
    ): Response<TvdbDtos.SearchResponse>

    @GET("series/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Header("Authorization") auth: String,
    ): Response<TvdbDtos.EpisodesResponse>
}

interface ImdbApi {
    @GET("title/{id}")
    suspend fun getTitle(
        @Path("id") id: String,
        @Query("includeFullPlot") includeFullPlot: Boolean = true,
        @Query("includeEpisodes") includeEpisodes: Boolean = false,
    ): Response<ImdbDtos.TitleResponse>

    @GET("search/title")
    suspend fun searchTitles(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        0,
    ): Response<ImdbDtos.SearchResponse>

    @GET("title/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: String,
        @Query("season") season: Int? = null,
    ): Response<ImdbDtos.EpisodesResponse>
}