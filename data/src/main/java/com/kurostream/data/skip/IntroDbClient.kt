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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntroDbClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val BASE_URL = "https://introdb.org/api/v1"
        private const val TIMEOUT_MS = 10000L
    }

    suspend fun getSkipTimes(
        anilistId: Int,
        episodeNumber: Int,
    ): Result<Map<SkipType, SkipInterval>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/skip/$anilistId/$episodeNumber"
            
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
                return@withContext Result.failure(Exception("IntroDB HTTP ${response.code}"))
            }
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val jsonObj = json.parseToJsonElement(body).jsonObject
            
            val skipMap = mutableMapOf<SkipType, SkipInterval>()
            
            jsonObj["intro"]?.jsonObject?.let { intro ->
                val start = intro["start"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val end = intro["end"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                if (end > start) skipMap[SkipType.INTRO] = SkipInterval(start, end)
            }
            
            jsonObj["outro"]?.jsonObject?.let { outro ->
                val start = outro["start"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val end = outro["end"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                if (end > start) skipMap[SkipType.OUTRO] = SkipInterval(start, end)
            }
            
            if (skipMap.isEmpty()) {
                return@withContext Result.failure(Exception("No skip intervals found"))
            }
            
            Result.success(skipMap)
        } catch (e: Exception) {
            Timber.e(e, "IntroDB fetch failed for anilistId=$anilistId ep=$episodeNumber")
            Result.failure(e)
        }
    }
}
