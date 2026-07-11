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

import com.google.gson.annotations.SerializedName

data class PlexResourceResponse(
    @SerializedName("name") val name: String,
    @SerializedName("clientIdentifier") val clientIdentifier: String,
    @SerializedName("provides") val provides: String?,
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("connections") val connections: List<PlexConnection>?
)

data class PlexConnection(
    @SerializedName("uri") val uri: String,
    @SerializedName("local") val local: Boolean?
)

data class PlexPinStatusResponse(
    @SerializedName("authToken") val authToken: String?,
    @SerializedName("clientIdentifier") val clientIdentifier: String?
)

data class PlexLibrarySection(
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String
)

data class PlexLibraryResponse(
    @SerializedName("MediaContainer") val mediaContainer: PlexMediaContainer?
)

data class PlexMediaContainer(
    @SerializedName("Directory") val directories: List<PlexLibrarySection>?,
    @SerializedName("Metadata") val metadata: List<PlexMetadata>?,
    @SerializedName("title1") val title1: String?
)

data class PlexMetadata(
    @SerializedName("ratingKey") val ratingKey: String,
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("parentTitle") val parentTitle: String?,
    @SerializedName("grandparentTitle") val grandparentTitle: String?,
    @SerializedName("index") val index: Int?,
    @SerializedName("parentIndex") val parentIndex: Int?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("art") val art: String?,
    @SerializedName("viewOffset") val viewOffset: Long?,
    @SerializedName("viewCount") val viewCount: Int?,
    @SerializedName("Media") val media: List<PlexMedia>?
)

data class PlexMedia(
    @SerializedName("id") val id: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("bitrate") val bitrate: Long?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("aspectRatio") val aspectRatio: Double?,
    @SerializedName("videoCodec") val videoCodec: String?,
    @SerializedName("audioCodec") val audioCodec: String?,
    @SerializedName("videoResolution") val videoResolution: String?,
    @SerializedName("container") val container: String?,
    @SerializedName("Part") val parts: List<PlexMediaPart>?
)

data class PlexMediaPart(
    @SerializedName("id") val id: String?,
    @SerializedName("key") val key: String,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("file") val file: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("container") val container: String?,
    @SerializedName("Stream") val streams: List<PlexStream>?
)

data class PlexStream(
    @SerializedName("id") val id: String?,
    @SerializedName("streamType") val streamType: Int,
    @SerializedName("codec") val codec: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("selected") val selected: Boolean?,
    @SerializedName("displayTitle") val displayTitle: String?
)
