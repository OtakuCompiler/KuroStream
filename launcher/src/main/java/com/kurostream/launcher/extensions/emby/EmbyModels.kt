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

import com.google.gson.annotations.SerializedName

data class EmbyAuthRequest(
    @SerializedName("Username") val Username: String,
    @SerializedName("Pw") val Pw: String
)

data class EmbyAuthResponse(
    @SerializedName("AccessToken") val AccessToken: String,
    @SerializedName("User") val User: EmbyUser?,
    @SerializedName("ServerId") val ServerId: String?
)

data class EmbyUser(
    @SerializedName("Id") val Id: String,
    @SerializedName("Name") val Name: String,
    @SerializedName("ServerId") val ServerId: String?
)

data class EmbyLibraryItem(
    @SerializedName("Id") val Id: String,
    @SerializedName("Name") val Name: String,
    @SerializedName("Type") val Type: String,
    @SerializedName("Overview") val Overview: String?,
    @SerializedName("RunTimeTicks") val RunTimeTicks: Long?,
    @SerializedName("SeriesName") val SeriesName: String?,
    @SerializedName("SeasonName") val SeasonName: String?,
    @SerializedName("IndexNumber") val IndexNumber: Int?,
    @SerializedName("ParentIndexNumber") val ParentIndexNumber: Int?,
    @SerializedName("ImageTags") val ImageTags: Map<String, String>?,
    @SerializedName("BackdropImageTags") val BackdropImageTags: List<String>?,
    @SerializedName("MediaSources") val MediaSources: List<EmbyMediaSource>?,
    @SerializedName("UserData") val UserData: EmbyUserData?
)

data class EmbyMediaSource(
    @SerializedName("Id") val Id: String,
    @SerializedName("Path") val Path: String?,
    @SerializedName("Protocol") val Protocol: String?,
    @SerializedName("Container") val Container: String?,
    @SerializedName("Size") val Size: Long?,
    @SerializedName("Bitrate") val Bitrate: Long?,
    @SerializedName("MediaStreams") val MediaStreams: List<EmbyMediaStream>?
)

data class EmbyMediaStream(
    @SerializedName("Type") val Type: String,
    @SerializedName("Codec") val Codec: String?,
    @SerializedName("Language") val Language: String?,
    @SerializedName("IsDefault") val IsDefault: Boolean?,
    @SerializedName("Index") val Index: Int?
)

data class EmbyUserData(
    @SerializedName("PlaybackPositionTicks") val PlaybackPositionTicks: Long?,
    @SerializedName("Played") val Played: Boolean?,
    @SerializedName("PlayedPercentage") val PlayedPercentage: Double?
)

data class EmbyItemsResponse(
    @SerializedName("Items") val Items: List<EmbyLibraryItem>,
    @SerializedName("TotalRecordCount") val TotalRecordCount: Int
)
