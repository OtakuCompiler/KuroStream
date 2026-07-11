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

import com.kurostream.data.remote.dto.anilist.AniListDtos
import com.kurostream.data.remote.dto.mal.MalDtos
import com.kurostream.data.remote.dto.tmdb.TmdbDtos
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface AniListApi {
    @POST("graphql")
    suspend fun getAnime(
        @Body query: AniListQuery
    ): Response<AniListDtos.MediaResponse>

    @POST("graphql")
    suspend fun searchAnime(
        @Body query: AniListQuery
    ): Response<AniListDtos.SearchResponse>

    @POST("graphql")
    suspend fun getAnimeByExternalId(
        @Body query: AniListQuery
    ): Response<AniListDtos.MediaResponse>

    @POST("graphql")
    suspend fun getSeasonalAnime(
        @Body query: AniListQuery
    ): Response<AniListDtos.SeasonalResponse>

    @POST("graphql")
    suspend fun getTrendingAnime(
        @Body query: AniListQuery
    ): Response<AniListDtos.TrendingResponse>
}

data class AniListQuery(
    val query: String,
    val variables: Map<String, Any> = emptyMap(),
)