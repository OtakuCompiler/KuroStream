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

import com.kurostream.data.remote.dto.mal.MalDtos
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface MalApi {
    @GET("anime/{id}")
    @Headers("X-MAL-CLIENT-ID: {clientId}")
    suspend fun getAnimeDetails(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Query("fields") fields: String = "id,title,main_picture,alternative_titles,start_date,end_date,synopsis,mean,rank,popularity,num_list_users,num_scoring_users,nsfw,created_at,updated_at,media_type,status,genres,num_episodes,start_season,broadcast,source,average_episode_duration,rating,pictures,background,related_anime,related_manga,recommendations,studios,statistics",
    ): Response<MalDtos.AnimeDetail>

    @GET("anime/search")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = "id,title,main_picture,alternative_titles,start_date,end_date,synopsis,mean,rank,popularity,num_list_users,num_scoring_users,nsfw,created_at,updated_at,media_type,status,genres,num_episodes,start_season,broadcast,source,average_episode_duration,rating,pictures,background,related_anime,related_manga,recommendations,studios,statistics",
    ): Response<MalDtos.SearchResponse>

    @GET("anime/season/{year}/{season}")
    suspend fun getSeasonalAnime(
        @Path("year") year: Int,
        @Path("season") season: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<MalDtos.SeasonalResponse>

    @GET("anime/ranking")
    suspend fun getTopAnime(
        @Query("ranking_type") rankingType: String = "all",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<MalDtos.RankingResponse>
}