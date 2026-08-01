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

import kotlinx.serialization.Serializable

@Serializable
data class StremioManifest(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val resources: List<StremioResource> = emptyList(),
    val catalogs: List<StremioCatalog>? = null,
    val types: List<String>? = null,
    val languages: List<String>? = null,
    val behaviorHints: StremioBehaviorHints? = null,
)

@Serializable
data class StremioBehaviorHints(
    val rating: Float? = null,
    val defaultVideo: Boolean? = null,
    val hasSubtitles: Boolean? = null,
)

@Serializable
data class StremioResource(
    val name: String,
    val types: List<String>? = null,
    val idPrefixes: List<String>? = null,
    val idRequired: Boolean? = null,
)

@Serializable
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<Map<String, String>>? = null,
    val genres: List<String>? = null,
)

@Serializable
data class StremioCatalogResponse(
    val metas: List<StremioMetaPreview> = emptyList(),
)

@Serializable
data class StremioMetaPreview(
    val id: String,
    val name: String,
    val description: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val type: String? = null,
    val genres: List<String>? = null,
    val releaseInfo: String? = null,
)

@Serializable
data class StremioMetaResponse(
    val meta: StremioMetaPreview? = null,
)

@Serializable
data class StremioStream(
    val url: String,
    val quality: String? = null,
    val headers: Map<String, String>? = null,
    val title: String? = null,
    val name: String? = null,
)

@Serializable
data class StremioStreamResponse(
    val streams: List<StremioStream> = emptyList(),
)

@Serializable
data class StremioSubtitle(
    val id: String,
    val url: String,
    val lang: String? = null,
    val name: String? = null,
    val format: String? = null,
)

@Serializable
data class StremioSubtitlesResponse(
    val subtitles: List<StremioSubtitle> = emptyList(),
)
