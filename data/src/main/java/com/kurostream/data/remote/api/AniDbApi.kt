package com.kurostream.data.remote.api

import com.kurostream.data.remote.dto.anidb.AniDbAnime
import com.kurostream.data.remote.dto.anidb.AniDbSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AniDbApi {
    @GET("anime/{id}")
    suspend fun getAnime(
        @Path("id") id: String,
    ): Response<AniDbAnime>

    @GET("search/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
    ): Response<List<AniDbAnime>>

    @GET("anime/search/anidb_id/{id}")
    suspend fun getAnimeByAniListId(
        @Path("id") id: Int,
    ): Response<List<AniDbAnime>>

    @GET("anime/search/mal_id/{id}")
    suspend fun getAnimeByMalId(
        @Path("id") id: Int,
    ): Response<List<AniDbAnime>>

    @GET("search/seasonal/{year}/{season}")
    suspend fun getSeasonalAnime(
        @Path("year") year: Int,
        @Path("season") season: String,
    ): Response<List<AniDbAnime>>

    @GET("search/popular")
    suspend fun getPopularAnime(
        @Query("limit") limit: Int = 20,
    ): Response<List<AniDbAnime>>
}
