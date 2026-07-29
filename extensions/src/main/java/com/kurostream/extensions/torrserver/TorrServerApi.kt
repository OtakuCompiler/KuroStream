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

package com.kurostream.extensions.torrserver

import retrofit2.http.*
import retrofit2.Response

interface TorrServerApi {

    @GET("/echo")
    suspend fun echo(): Response<String>

    @POST("/settings")
    suspend fun updateSettings(@Body settings: Map<String, Any>): Response<Map<String, Any>>

    @GET("/settings")
    suspend fun getSettings(): Response<Map<String, Any>>

    @GET("/torrents")
    suspend fun listTorrents(): Response<List<TorrServerTorrent>>

    @POST("/torrents")
    suspend fun addTorrent(@Body request: AddTorrentRequest): Response<TorrServerTorrent>

    @GET("/torrents/{hash}")
    suspend fun getTorrent(@Path("hash") hash: String): Response<TorrServerTorrent>

    @POST("/torrents/{hash}")
    suspend fun actionTorrent(
        @Path("hash") hash: String,
        @Query("action") action: String
    ): Response<Map<String, Any>>

    @GET("/torrents/{hash}/stat")
    suspend fun getTorrentStat(@Path("hash") hash: String): Response<TorrentStat>

    @GET("/stream/{hash}/{file_index}")
    suspend fun streamFile(
        @Path("hash") hash: String,
        @Path("file_index") fileIndex: Int
    ): Response<Void>

    @GET("/playlist/{hash}/{file_index}.m3u")
    suspend fun getPlaylist(
        @Path("hash") hash: String,
        @Path("file_index") fileIndex: Int
    ): Response<String>

    @DELETE("/torrents/{hash}")
    suspend fun removeTorrent(@Path("hash") hash: String): Response<Map<String, Any>>
}

data class AddTorrentRequest(
    val link: String,
    val title: String? = null,
    val poster: String? = null,
    val data: String? = null,
    val save_to_db: Boolean? = null
)
