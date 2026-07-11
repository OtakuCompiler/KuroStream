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

import com.kurostream.extensions.domain.model.CatalogItem
import com.kurostream.extensions.domain.model.MediaDetail
import com.kurostream.extensions.domain.model.StreamSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioAddonRepository @Inject constructor() {

    private val addonClients = mutableMapOf<String, StremioAddonApi>()

    fun registerAddon(baseUrl: String): StremioAddonApi {
        val normalizedUrl = baseUrl.trimEnd('/') + "/"
        return addonClients.getOrPut(normalizedUrl) {
            createAddonClient(normalizedUrl)
        }
    }

    fun getRegisteredAddons(): Map<String, StremioAddonApi> = addonClients.toMap()

    suspend fun fetchManifest(baseUrl: String): Result<StremioManifest> = try {
        val client = registerAddon(baseUrl)
        val response = client.getManifest()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to fetch manifest: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun fetchCatalog(
        baseUrl: String,
        type: String,
        catalogId: String,
        extra: String = ""
    ): Flow<Result<List<CatalogItem>>> = flow {
        try {
            val client = registerAddon(baseUrl)
            val response = if (extra.isNotBlank()) {
                client.getCatalogWithExtra(type, catalogId, extra)
            } else {
                client.getCatalog(type, catalogId)
            }
            if (response.isSuccessful) {
                val items = response.body()?.metas?.map { it.toCatalogItem(baseUrl) } ?: emptyList()
                emit(Result.success(items))
            } else {
                emit(Result.failure(Exception("Catalog fetch failed: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun fetchMeta(baseUrl: String, type: String, id: String): Result<MediaDetail> = try {
        val client = registerAddon(baseUrl)
        val response = client.getMeta(type, id)
        if (response.isSuccessful && response.body()?.meta != null) {
            Result.success(response.body()!!.meta!!.toMediaDetail())
        } else {
            Result.failure(Exception("Meta fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun fetchStreams(baseUrl: String, type: String, id: String): Result<List<StreamSource>> = try {
        val client = registerAddon(baseUrl)
        val response = client.getStreams(type, id)
        if (response.isSuccessful) {
            val streams = response.body()?.streams?.map { it.toStreamSource() } ?: emptyList()
            Result.success(streams)
        } else {
            Result.failure(Exception("Streams fetch failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun createAddonClient(baseUrl: String): StremioAddonApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(StremioAddonApi::class.java)
    }
}

private fun StremioMetaPreview.toCatalogItem(addonUrl: String): CatalogItem = CatalogItem(
    id = "stremio:${addonUrl}:${id}",
    title = name,
    posterUrl = poster,
    backdropUrl = banner,
    type = type,
    year = releaseInfo,
    rating = imdbRating?.toFloatOrNull(),
    genres = genres ?: emptyList(),
    source = "stremio:${addonUrl}"
)

private fun StremioMeta.toMediaDetail(): MediaDetail = MediaDetail(
    id = id,
    title = name,
    description = description,
    posterUrl = poster,
    backdropUrl = background,
    type = type,
    rating = imdbRating?.toFloatOrNull(),
    genres = genres ?: emptyList(),
    cast = cast ?: emptyList(),
    director = director ?: emptyList(),
    year = releaseInfo,
    runtime = runtime,
    episodes = videos?.map { video ->
        com.kurostream.extensions.domain.model.Episode(
            id = video.id,
            title = video.title,
            episodeNumber = video.episode ?: 0,
            seasonNumber = video.season ?: 0,
            thumbnail = video.thumbnail,
            overview = video.overview,
            released = video.released
        )
    } ?: emptyList()
)

private fun StremioStream.toStreamSource(): StreamSource = StreamSource(
    url = url,
    name = name ?: title ?: "Stream",
    quality = title?.let { extractQuality(it) } ?: "Unknown",
    headers = behaviorHints?.proxyHeaders?.get("request") ?: emptyMap(),
    isHls = url?.endsWith(".m3u8") == true,
    isDash = url?.endsWith(".mpd") == true
)

private fun extractQuality(title: String): String {
    val regex = Regex("(\d{3,4}p|4K|HDR|SD|HD|FHD|UHD)", RegexOption.IGNORE_CASE)
    return regex.find(title)?.value ?: "Unknown"
}
