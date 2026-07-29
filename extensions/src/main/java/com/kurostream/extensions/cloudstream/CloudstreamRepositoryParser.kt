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

package com.kurostream.extensions.cloudstream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudstreamRepositoryParser @Inject constructor() {

    private val client = OkHttpClient()

    suspend fun parseRepository(url: String): Result<List<CloudstreamRepoEntry>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            val json = JSONObject(body)
            val plugins = json.getJSONArray("plugins")
            val entries = mutableListOf<CloudstreamRepoEntry>()

            for (i in 0 until plugins.length()) {
                val plugin = plugins.getJSONObject(i)
                entries.add(parseRepoEntry(plugin))
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRepoEntry(json: JSONObject): CloudstreamRepoEntry {
        return CloudstreamRepoEntry(
            name = json.getString("name"),
            url = json.getString("url"),
            version = json.getInt("version"),
            versionName = json.optString("versionName"),
            author = json.optString("author"),
            description = json.optString("description"),
            status = json.optInt("status", 1),
            language = json.optString("language", "en"),
            tvTypes = json.optJSONArray("tvTypes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            iconUrl = json.optString("iconUrl"),
            apiVersion = json.optInt("apiVersion", 1)
        )
    }
}

data class CloudstreamRepoEntry(
    val name: String,
    val url: String,
    val version: Int,
    val versionName: String,
    val author: String,
    val description: String,
    val status: Int,
    val language: String,
    val tvTypes: List<String>,
    val iconUrl: String,
    val apiVersion: Int
)
