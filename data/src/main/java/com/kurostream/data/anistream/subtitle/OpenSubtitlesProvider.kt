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

package com.kurostream.data.anistream.subtitle

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSubtitlesProvider @Inject constructor(
    private val httpClient: HttpClient,
    private val cacheDir: File
) : SubtitleProvider {

    override val name: String = "OpenSubtitles"

    private val baseUrl = "https://api.opensubtitles.com/api/v1"
    private val apiKey = "YOUR_API_KEY" // Replace with BuildConfig.OPEN_SUBTITLES_API_KEY

    override suspend fun searchSubtitles(query: SubtitleSearchQuery): Result<List<SubtitleResult>> {
        return try {
            val response = httpClient.get("$baseUrl/subtitles") {
                header("Api-Key", apiKey)
                header("Accept", "application/json")
                query.query?.let { parameter("query", it) }
                query.imdbId?.let { parameter("imdb_id", it) }
                query.tmdbId?.let { parameter("tmdb_id", it.toString()) }
                query.season?.let { parameter("season_number", it.toString()) }
                query.episode?.let { parameter("episode_number", it.toString()) }
                query.languages.takeIf { it.isNotEmpty() }?.let {
                    parameter("languages", it.joinToString(","))
                }
                query.movieHash?.let { parameter("moviehash", it) }
            }

            val result = response.body<OpenSubtitlesSearchResponse>()
            Result.success(result.data.map { it.toSubtitleResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSubtitle(subtitleId: String): Result<File> {
        return try {
            val response = httpClient.post("$baseUrl/download") {
                header("Api-Key", apiKey)
                header("Accept", "application/json")
                contentType(ContentType.Application.Json)
                setBody(mapOf("file_id" to subtitleId.toInt()))
            }

            val downloadInfo = response.body<OpenSubtitlesDownloadResponse>()
            val subtitleFile = File(cacheDir, "subtitle_${subtitleId}.srt")

            // Download actual file from link
            val fileResponse = httpClient.get(downloadInfo.link)
            subtitleFile.writeBytes(fileResponse.body())

            Result.success(subtitleFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun OpenSubtitlesData.toSubtitleResult(): SubtitleResult {
        return SubtitleResult(
            id = attributes.files.firstOrNull()?.fileId?.toString() ?: id,
            language = attributes.language,
            languageName = attributes.language,
            filename = attributes.files.firstOrNull()?.fileName ?: attributes.release ?: "unknown",
            format = attributes.format ?: "srt",
            downloadCount = attributes.downloadCount ?: 0,
            isHearingImpaired = attributes.hearingImpaired ?: false,
            rating = attributes.ratings ?: 0f,
            uploadDate = attributes.uploadDate
        )
    }
}

@Serializable
data class OpenSubtitlesSearchResponse(
    val totalCount: Int,
    val data: List<OpenSubtitlesData>
)

@Serializable
data class OpenSubtitlesData(
    val id: String,
    val type: String,
    val attributes: OpenSubtitlesAttributes
)

@Serializable
data class OpenSubtitlesAttributes(
    @SerialName("subtitle_id") val subtitleId: String? = null,
    val language: String,
    @SerialName("download_count") val downloadCount: Int? = null,
    @SerialName("upload_date") val uploadDate: String? = null,
    val ratings: Float? = null,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean? = null,
    val format: String? = null,
    val release: String? = null,
    val files: List<OpenSubtitlesFile> = emptyList()
)

@Serializable
data class OpenSubtitlesFile(
    @SerialName("file_id") val fileId: Int,
    @SerialName("file_name") val fileName: String? = null
)

@Serializable
data class OpenSubtitlesDownloadResponse(
    val link: String,
    @SerialName("file_name") val fileName: String,
    val requests: Int,
    val remaining: Int,
    val message: String? = null
)
