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

package com.kurostream.data.remote.dto.opensubtitles

import com.google.gson.annotations.SerializedName

data class OpenSubtitlesSearchResponse(
    val total_pages: Int?, val total_count: Int?, val page: Int?, val data: List<OpenSubtitlesItem>?
)

data class OpenSubtitlesItem(val id: String?, val type: String?, val attributes: OpenSubtitlesAttributes?)

data class OpenSubtitlesAttributes(
    val subtitle_id: String?, val language: String?,
    @SerializedName("download_count") val downloadCount: Int?,
    @SerializedName("new_download_count") val newDownloadCount: Int?,
    val hearing_impaired: Boolean?, val hd: Boolean?, val fps: Double?, val votes: Int?, val ratings: Double?,
    @SerializedName("from_trusted") val fromTrusted: Boolean?,
    @SerializedName("foreign_parts_only") val foreignPartsOnly: Boolean?,
    @SerializedName("upload_date") val uploadDate: String?,
    @SerializedName("ai_translated") val aiTranslated: Boolean?,
    @SerializedName("nb_cd") val nbCd: Int?, val slug: String?,
    @SerializedName("legacy_subtitle_id") val legacySubtitleId: Int?,
    val uploader: OpenSubtitlesUploader?,
    @SerializedName("feature_details") val featureDetails: OpenSubtitlesFeatureDetails?,
    val url: String?, val relatedLinks: List<OpenSubtitlesRelatedLink>?,
    val files: List<OpenSubtitlesFile>?
)

data class OpenSubtitlesUploader(val uploader_id: Int?, val name: String?, val rank: String?)

data class OpenSubtitlesFeatureDetails(
    val feature_id: Int?, val feature_type: String?, val year: Int?, val title: String?,
    @SerializedName("movie_name") val movieName: String?,
    @SerializedName("imdb_id") val imdbId: Int?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    val season_number: Int?, val episode_number: Int?,
    val parent_imdb_id: Int?, val parent_title: String?,
    val parent_tmdb_id: Int?, val parent_feature_id: Int?
)

data class OpenSubtitlesRelatedLink(val label: String?, val url: String?, val img_url: String?)
data class OpenSubtitlesFile(@SerializedName("file_id") val fileId: Int?, @SerializedName("cd_number") val cdNumber: Int?, @SerializedName("file_name") val fileName: String?)

data class OpenSubtitlesSubtitleInfoResponse(val data: OpenSubtitlesItem?)

data class OpenSubtitlesDownloadRequest(
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("sub_format") val subFormat: String? = "srt",
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("strip_html") val stripHtml: Boolean? = true,
    @SerializedName("cleanup_links") val cleanupLinks: Boolean? = true,
    @SerializedName("remove_adds") val removeAdds: Boolean? = true
)

data class OpenSubtitlesDownloadResponse(
    val link: String?, @SerializedName("file_name") val fileName: String?,
    @SerializedName("requests") val requests: Int?, @SerializedName("remaining") val remaining: Int?,
    @SerializedName("message") val message: String?, @SerializedName("reset_time") val resetTime: String?,
    @SerializedName("reset_time_utc") val resetTimeUtc: String?
)

data class OpenSubtitlesLanguagesResponse(val data: List<OpenSubtitlesLanguage>?)
data class OpenSubtitlesLanguage(val language_code: String?, val language_name: String?, val uploads: Int?)
data class OpenSubtitlesFormatsResponse(val data: List<String>?, val total_count: Int?)
