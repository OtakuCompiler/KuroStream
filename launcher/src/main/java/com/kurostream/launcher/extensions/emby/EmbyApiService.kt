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

package com.kurostream.launcher.extensions.emby

import retrofit2.Response
import retrofit2.http.*

interface EmbyApiService {

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(@Body request: EmbyAuthRequest): Response<EmbyAuthResponse>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "Overview,RunTimeTicks,MediaSources,UserData",
        @Query("Limit") limit: Int? = null
    ): Response<EmbyItemsResponse>

    @GET("Users/{userId}/Views")
    suspend fun getViews(@Path("userId") userId: String): Response<EmbyItemsResponse>

    @GET("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String
    ): Response<EmbyPlaybackInfoResponse>

    @GET("Videos/{itemId}/stream")
    suspend fun getStreamUrl(
        @Path("itemId") itemId: String,
        @Query("Static") static: Boolean = true
    ): Response<Void>

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(@Body request: EmbyPlaybackProgress): Response<Void>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(@Body request: EmbyPlaybackProgress): Response<Void>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(@Body request: EmbyPlaybackProgress): Response<Void>
}

data class EmbyPlaybackInfoResponse(
    val MediaSources: List<EmbyMediaSource>?,
    val PlaySessionId: String?
)

data class EmbyPlaybackProgress(
    val ItemId: String,
    val PositionTicks: Long?,
    val IsPaused: Boolean = false,
    val IsMuted: Boolean = false,
    val VolumeLevel: Int = 100,
    val PlayMethod: String = "DirectPlay",
    val PlaySessionId: String? = null
)
