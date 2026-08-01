// KuroStream - Anime Streaming for Android TV
// Copyright (C) 2026 KuroStream Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.data.skip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniSkipClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val BASE_URL = "https://api.aniskip.com/v2"
        private const val TIMEOUT_MS = 8000L
    }

    suspend fun getSkipTimes(
        malId: Int,
        episodeNumber: Int,
        episodeLength: Double = 24.0,
        types: List<SkipType> = SkipType.entries,
    ): Result<Map<SkipType, SkipInterval>> = withContext(Dispatchers.IO) {
        try {
            val typeParam = types.joinToString(",") { it.apiName }
            val url = "$BASE_URL/skip-times/$malId/$episodeNumber?types=$typeParam&episodeLength=$episodeLength"
            
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            
            val response = okHttpClient.newBuilder()
                .connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
                .newCall(request)
                .execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("AniSkip HTTP ${response.code}"))
            }
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val jsonObj = json.parseToJsonElement(body).jsonObject
            
            val found = jsonObj["found"]?.jsonPrimitive?.content?.toBoolean() ?: false
            if (!found) {
                return@withContext Result.failure(Exception("No skip data found"))
            }
            
            val results = jsonObj["results"]?.jsonArray ?: return@withContext Result.failure(Exception("No results"))
            
            val skipMap = mutableMapOf<SkipType, SkipInterval>()
            results.forEach { element ->
                val obj = element.jsonObject
                val skipType = obj["skipType"]?.jsonPrimitive?.content ?: return@forEach
                val intervalObj = obj["interval"]?.jsonObject ?: return@forEach
                
                val startTime = intervalObj["startTime"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val endTime = intervalObj["endTime"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                
                val type = when (skipType) {
                    "op" -> SkipType.INTRO
                    "ed" -> SkipType.OUTRO
                    "recap" -> SkipType.RECAP
                    "mixed-op" -> SkipType.MIXED_INTRO
                    "mixed-ed" -> SkipType.MIXED_OUTRO
                    else -> return@forEach
                }
                
                skipMap[type] = SkipInterval(startTime, endTime)
            }
            
            Result.success(skipMap)
        } catch (e: Exception) {
            Timber.e(e, "AniSkip fetch failed for malId=$malId ep=$episodeNumber")
            Result.failure(e)
        }
    }
}

enum class SkipType(val apiName: String) {
    INTRO("op"),
    OUTRO("ed"),
    RECAP("recap"),
    MIXED_INTRO("mixed-op"),
    MIXED_OUTRO("mixed-ed"),
}

data class SkipInterval(
    val startTime: Double,
    val endTime: Double,
)
