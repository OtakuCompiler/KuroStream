package com.kurostream.data.remote.api

import com.kurostream.data.remote.dto.kitsu.KitsuModels
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KitsuApi {
    @GET("anime/{id}")
    suspend fun getAnimeDetails(@Path("id") id: String): Response<KitsuModels.AnimeDetail>

    @GET("anime")
    suspend fun searchAnime(@Query("filter[text]") query: String, @Query("page[limit]") limit: Int = 20): Response<KitsuModels.SearchResponse>
}
