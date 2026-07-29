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

import com.kurostream.data.remote.dto.anilist.AniListAnimeDetailsRequest
import com.kurostream.data.remote.dto.anilist.AniListAnimeDetailsResponse
import com.kurostream.data.remote.dto.anilist.AniListSearchRequest
import com.kurostream.data.remote.dto.anilist.AniListSearchResponse
import com.kurostream.data.remote.dto.anilist.AniListTrendingRequest
import com.kurostream.data.remote.dto.anilist.AniListTrendingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {
    @POST("graphql")
    suspend fun searchAnime(
        @Body request: AniListSearchRequest
    ): Response<AniListSearchResponse>

    @POST("graphql")
    suspend fun getAnimeDetails(
        @Body request: AniListAnimeDetailsRequest
    ): Response<AniListAnimeDetailsResponse>

    @POST("graphql")
    suspend fun getTrendingAnime(
        @Body request: AniListTrendingRequest
    ): Response<AniListTrendingResponse>
}
