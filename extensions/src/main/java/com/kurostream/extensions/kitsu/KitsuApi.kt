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

package com.kurostream.extensions.kitsu

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KitsuApi {

    @GET("edge/anime")
    suspend fun getAnimeList(
        @Query("page[limit]") limit: Int = 20,
        @Query("page[offset]") offset: Int = 0,
        @Query("filter[season]") season: String? = null,
        @Query("filter[seasonYear]") seasonYear: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("filter[text]") search: String? = null,
        @Query("include") include: String = "genres,characters,staff"
    ): Response<KitsuAnimeListResponse>

    @GET("edge/anime/{id}")
    suspend fun getAnime(
        @Path("id") id: String,
        @Query("include") include: String = "genres,characters,staff,castings.person,castings.character"
    ): Response<KitsuAnimeResponse>

    @GET("edge/trending/anime")
    suspend fun getTrendingAnime(
        @Query("page[limit]") limit: Int = 20
    ): Response<KitsuAnimeListResponse>

    @GET("edge/anime")
    suspend fun getSeasonalAnime(
        @Query("filter[season]") season: String,
        @Query("filter[seasonYear]") year: Int,
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-user_count"
    ): Response<KitsuAnimeListResponse>

    @GET("edge/anime")
    suspend fun getPopularAnime(
        @Query("page[limit]") limit: Int = 20,
        @Query("sort") sort: String = "-user_count"
    ): Response<KitsuAnimeListResponse>

    @GET("edge/characters/{id}")
    suspend fun getCharacter(@Path("id") id: String): Response<KitsuCharacterResponse>

    @GET("edge/anime/{id}/characters")
    suspend fun getAnimeCharacters(
        @Path("id") id: String,
        @Query("page[limit]") limit: Int = 20
    ): Response<KitsuCharacterListResponse>

    @GET("edge/anime/{id}/staff")
    suspend fun getAnimeStaff(
        @Path("id") id: String,
        @Query("page[limit]") limit: Int = 20
    ): Response<KitsuStaffListResponse>
}
