// This file is part of KuroStream.
//
// PodnapisiProvider — Podnapisi subtitle source via HTML scraping.
// Podnapisi exposes search results as HTML pages with no official REST API.
// Requires a browser-like User-Agent to avoid 403.
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
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodnapisiProvider @Inject constructor(
    private val client: OkHttpClient,
) : SubtitleProvider {

    override val id = "podnapisi"
    override val name = "Podnapisi"
    override val requiresAuth = false

    private val baseUrl = "https://www.podnapisi.net"

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val lang = languages.firstOrNull() ?: "en"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/subtitles/search/?keywords=$encodedQuery&language=$lang"

            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseResults(body, episodeInfo, languages)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseResults(
        html: String,
        episodeInfo: EpisodeInfo?,
        languages: List<String>,
    ): List<SubtitleCandidate> {
        val doc = Jsoup.parse(html)
        val entries = doc.select(".subtitle-entry, .table tbody tr")

        return entries.mapNotNull { entry ->
            val titleEl = entry.selectFirst(".subtitle-title, td:nth-child(1)")
            val title = titleEl?.text()?.trim() ?: return@mapNotNull null

            val langEl = entry.selectFirst(".subtitle-language, td:nth-child(2)")
            val language = langEl?.text()?.trim() ?: return@mapNotNull null

            val filteredLanguages = languages.takeIf { it.isNotEmpty() }
            if (filteredLanguages != null && !filteredLanguages.any {
                    language.contains(it, ignoreCase = true) ||
                            language.contains(langToEnglish(it), ignoreCase = true)
                }
            ) return@mapNotNull null

            val downloadLink = entry.selectFirst("a[href*=download], a[href*=srt]")?.attr("href")
                ?: entry.selectFirst("a[href]")?.attr("href")

            SubtitleCandidate(
                id = "podnapisi_${title.hashCode()}_$language",
                mediaId = title,
                languageCode = languageCodeFromPodnapisi(language),
                languageName = language,
                label = title,
                format = SubtitleFormat.SRT,
                sourceUrl = if (downloadLink.isNullOrBlank()) null else
                    if (downloadLink.startsWith("http")) downloadLink else baseUrl + downloadLink,
                providerId = id,
            )
        }
    }

    private fun languageCodeFromPodnapisi(name: String): String = when {
        name.contains("English", ignoreCase = true) -> "en"
        name.contains("Croatian", ignoreCase = true) -> "hr"
        name.contains("Serbian", ignoreCase = true) -> "sr"
        name.contains("Slovenian", ignoreCase = true) -> "sl"
        name.contains("Bosnian", ignoreCase = true) -> "bs"
        name.contains("Macedonian", ignoreCase = true) -> "mk"
        name.contains("Albanian", ignoreCase = true) -> "sq"
        else -> name.take(2).lowercase()
    }

    private fun langToEnglish(code: String): String = when (code.lowercase()) {
        "en" -> "English"
        "hr" -> "Croatian"
        "sr" -> "Serbian"
        "sl" -> "Slovenian"
        else -> code
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? = candidate.sourceUrl

    override suspend fun getLanguages(): List<String> = emptyList()

    override fun isEnabled(): Boolean = true
}
