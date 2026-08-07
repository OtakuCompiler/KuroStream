package com.kurostream.data.remote.api

import com.kurostream.data.remote.dto.ann.AnnSearchResponse
import com.kurostream.data.remote.dto.ann.AnnAnimeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnnApi {
    @GET("encyclopedia/anime/{id}")
    suspend fun getAnime(
        @Path("id") id: String,
    ): Response<AnnAnimeResponse>

    @GET("encyclopedia/search/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<AnnSearchResponse>
}
