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

package com.kurostream.data.remote.dto.youtube

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeDtos(val placeholder: String = "")

@Serializable
data class SearchResponse(
    val kind: String,
    val etag: String,
    val nextPageToken: String?,
    val regionCode: String?,
    val pageInfo: PageInfo,
    val items: List<SearchItem> = emptyList(),
)

@Serializable
data class PageInfo(
    val totalResults: Int,
    val resultsPerPage: Int,
)

@Serializable
data class SearchItem(
    val kind: String,
    val etag: String,
    val id: SearchId,
    val snippet: SearchSnippet,
)

@Serializable
data class SearchId(
    val kind: String,
    val videoId: String?,
    val channelId: String?,
    val playlistId: String?,
)

@Serializable
data class SearchSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String,
    val liveBroadcastContent: String,
    val publishTime: String,
)

@Serializable
data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?,
    val standard: Thumbnail?,
    val maxres: Thumbnail?,
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class VideoListResponse(
    val kind: String,
    val etag: String,
    val pageInfo: PageInfo,
    val items: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val kind: String,
    val etag: String,
    val id: String,
    val snippet: VideoSnippet,
    val contentDetails: ContentDetails?,
    val statistics: Statistics?,
)

@Serializable
data class VideoSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String,
    val tags: List<String> = emptyList(),
    val categoryId: String,
    val liveBroadcastContent: String,
    val localized: Localized,
    val defaultAudioLanguage: String?,
)

@Serializable
data class Localized(
    val title: String,
    val description: String,
)

@Serializable
data class ContentDetails(
    val duration: String,
    val dimension: String,
    val definition: String,
    val caption: String,
    val licensedContent: Boolean,
    val regionRestriction: RegionRestriction?,
    val contentRating: ContentRating?,
    val projection: String,
)

@Serializable
data class RegionRestriction(
    val allowed: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
)

@Serializable
data class ContentRating(
    val ytRating: String?,
)

@Serializable
data class Statistics(
    val viewCount: String?,
    val likeCount: String?,
    val dislikeCount: String?,
    val favoriteCount: String?,
    val commentCount: String?,
)