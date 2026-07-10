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

package com.kurostream.data.anistream.introskip

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniSkipApi @Inject constructor(
    private val httpClient: HttpClient
) {

    companion object {
        const val BASE_URL = "https://api.aniskip.com/v2"
    }

    suspend fun getSkipTimes(
        malId: Int? = null,
        anilistId: Int? = null,
        episodeNumber: Int,
        episodeLength: Double = 24.0
    ): Result<SkipTimesResponse> {
        return try {
            val response = httpClient.get("$BASE_URL/skip-times") {
                parameter("episodeLength", episodeLength)
                malId?.let { parameter("malId", it) }
                anilistId?.let { parameter("anilistId", it) }
                parameter("episodeNumber", episodeNumber)
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class SkipTimesResponse(
    val found: Boolean,
    val results: List<SkipResult>,
    val message: String? = null
)

@Serializable
data class SkipResult(
    val interval: SkipInterval,
    val skipType: String,
    val skipId: String,
    val episodeLength: Double
)

@Serializable
data class SkipInterval(
    val startTime: Double,
    val endTime: Double
)
