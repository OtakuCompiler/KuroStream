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

package com.kurostream.extensions.stremio

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface StremioAddonApi {

    @GET("manifest.json")
    suspend fun getManifest(): Response<StremioManifest>

    @GET("catalog/{type}/{id}/{extra}.json")
    suspend fun getCatalogWithExtra(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("extra") extra: String
    ): Response<StremioCatalogResponse>

    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<StremioCatalogResponse>

    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<StremioMetaResponse>

    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<StremioStreamResponse>

    @GET("subtitles/{type}/{id}.json")
    suspend fun getSubtitles(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<StremioSubtitlesResponse>
}
