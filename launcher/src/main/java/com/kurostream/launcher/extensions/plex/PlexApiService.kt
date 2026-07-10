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

package com.kurostream.launcher.extensions.plex

import retrofit2.Response
import retrofit2.http.*

interface PlexApiService {
    @POST("pins")
    @FormUrlEncoded
    suspend fun createPin(
        @Field("strong") strong: Boolean,
        @Field("X-Plex-Client-Identifier") clientId: String
    ): Response<PlexPinResponse>

    @GET("pins/{pinId}")
    suspend fun checkPin(
        @Path("pinId") pinId: String,
        @Query("X-Plex-Client-Identifier") clientId: String
    ): Response<PlexPinStatusResponse>

    @GET("resources")
    suspend fun getResources(
        @Query("X-Plex-Token") token: String,
        @Query("X-Plex-Client-Identifier") clientId: String,
        @Query("includeHttps") includeHttps: Boolean = true
    ): Response<List<PlexResourceResponse>>
}

interface PlexServerApiService {
    @GET("library/sections")
    suspend fun getLibrarySections(): Response<PlexLibraryResponse>

    @GET("library/sections/{sectionKey}/all")
    suspend fun getLibraryItems(
        @Path("sectionKey") sectionKey: String,
        @Query("type") type: Int? = null
    ): Response<PlexLibraryResponse>

    @GET("library/metadata/{ratingKey}")
    suspend fun getMetadata(
        @Path("ratingKey") ratingKey: String
    ): Response<PlexLibraryResponse>

    @GET("library/metadata/{ratingKey}/children")
    suspend fun getChildren(
        @Path("ratingKey") ratingKey: String
    ): Response<PlexLibraryResponse>

    @GET("{partKey}")
    suspend fun getStream(
        @Path("partKey") partKey: String,
        @Query("download") download: Boolean = false
    ): Response<Void>

    @GET(":/timeline")
    suspend fun updateTimeline(
        @Query("ratingKey") ratingKey: String,
        @Query("key") key: String,
        @Query("state") state: String,
        @Query("time") time: Long,
        @Query("duration") duration: Long
    ): Response<Void>
}
