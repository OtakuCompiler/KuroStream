// This file is part of KuroStream.
//
// SubDLProvider — SubDL subtitle provider.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubDLProvider @Inject constructor(
    private val client: OkHttpClient,
) : SubtitleProvider {

    override val id = "subdl"
    override val name = "SubDL"
    override val requiresAuth = false

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.subdl.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: return@withContext emptyList()
                val out = mutableListOf<SubtitleCandidate>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val lang = item.optString("language", "unknown")
                    if (languages.isNotEmpty() && lang !in languages) continue
                    out += SubtitleCandidate(
                        id = item.optString("id", i.toString()),
                        mediaId = query,
                        languageCode = lang,
                        languageName = lang,
                        label = item.optString("release_name"),
                        format = SubtitleFormat.SRT,
                        sourceUrl = item.optString("url"),
                        isDefault = false,
                        isHearingImpaired = item.optBoolean("hi", false),
                        providerId = id,
                    )
                }
                out
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? = candidate.sourceUrl
    override suspend fun getLanguages(): List<String> = emptyList()
    override fun isEnabled(): Boolean = true
}
