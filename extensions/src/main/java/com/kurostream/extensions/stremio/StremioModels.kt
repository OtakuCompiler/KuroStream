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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StremioManifest(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "version") val version: String,
    @Json(name = "catalogs") val catalogs: List<StremioCatalogDef> = emptyList(),
    @Json(name = "resources") val resources: List<StremioResource> = emptyList(),
    @Json(name = "types") val types: List<String> = emptyList(),
    @Json(name = "idPrefixes") val idPrefixes: List<String>? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "background") val background: String? = null,
    @Json(name = "behaviorHints") val behaviorHints: BehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class StremioCatalogDef(
    @Json(name = "type") val type: String,
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "extra") val extra: List<ExtraDef>? = null,
    @Json(name = "extraSupported") val extraSupported: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ExtraDef(
    @Json(name = "name") val name: String,
    @Json(name = "isRequired") val isRequired: Boolean = false,
    @Json(name = "options") val options: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioResource(
    @Json(name = "name") val name: String,
    @Json(name = "types") val types: List<String>? = null,
    @Json(name = "idPrefixes") val idPrefixes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class BehaviorHints(
    @Json(name = "configurable") val configurable: Boolean = false,
    @Json(name = "configurationRequired") val configurationRequired: Boolean = false
)

@JsonClass(generateAdapter = true)
data class StremioCatalogResponse(
    @Json(name = "metas") val metas: List<StremioMetaPreview> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioMetaPreview(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "name") val name: String,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "posterShape") val posterShape: String? = null,
    @Json(name = "banner") val banner: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "imdbRating") val imdbRating: String? = null,
    @Json(name = "releaseInfo") val releaseInfo: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "runtime") val runtime: String? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "behaviorHints") val behaviorHints: MetaBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class MetaBehaviorHints(
    @Json(name = "defaultVideoId") val defaultVideoId: String? = null
)

@JsonClass(generateAdapter = true)
data class StremioMetaResponse(
    @Json(name = "meta") val meta: StremioMeta? = null
)

@JsonClass(generateAdapter = true)
data class StremioMeta(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "name") val name: String,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "posterShape") val posterShape: String? = null,
    @Json(name = "background") val background: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "releaseInfo") val releaseInfo: String? = null,
    @Json(name = "imdbRating") val imdbRating: String? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "runtime") val runtime: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "awards") val awards: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "cast") val cast: List<String>? = null,
    @Json(name = "director") val director: List<String>? = null,
    @Json(name = "writer") val writer: List<String>? = null,
    @Json(name = "trailers") val trailers: List<Trailer>? = null,
    @Json(name = "videos") val videos: List<StremioVideo>? = null,
    @Json(name = "links") val links: List<StremioLink>? = null,
    @Json(name = "behaviorHints") val behaviorHints: MetaBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class Trailer(
    @Json(name = "source") val source: String,
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class StremioVideo(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "episode") val episode: Int? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "streams") val streams: List<StremioStream>? = null
)

@JsonClass(generateAdapter = true)
data class StremioLink(
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class StremioStreamResponse(
    @Json(name = "streams") val streams: List<StremioStream> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioStream(
    @Json(name = "url") val url: String? = null,
    @Json(name = "ytId") val ytId: String? = null,
    @Json(name = "infoHash") val infoHash: String? = null,
    @Json(name = "fileIdx") val fileIdx: Int? = null,
    @Json(name = "externalUrl") val externalUrl: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "subtitles") val subtitles: List<SubtitleSource>? = null,
    @Json(name = "sources") val sources: List<String>? = null,
    @Json(name = "behaviorHints") val behaviorHints: StreamBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class StreamBehaviorHints(
    @Json(name = "notWebReady") val notWebReady: Boolean = false,
    @Json(name = "proxyHeaders") val proxyHeaders: Map<String, Map<String, String>>? = null,
    @Json(name = "filename") val filename: String? = null
)

@JsonClass(generateAdapter = true)
data class SubtitleSource(
    @Json(name = "url") val url: String,
    @Json(name = "lang") val lang: String
)

@JsonClass(generateAdapter = true)
data class StremioSubtitlesResponse(
    @Json(name = "subtitles") val subtitles: List<StremioSubtitle> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioSubtitle(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String,
    @Json(name = "lang") val lang: String
)
