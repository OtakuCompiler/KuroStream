// This file is part of KuroStream.
//
// Addic7edProvider — Addic7ed subtitle source via Jsoup HTML scraping.
// Addic7ed exposes search results as an HTML table with no official API.
// Requires a browser-like User-Agent to avoid 403.
//
// Known limitations (by design of the upstream site):
//   - Rate-limited; implement request throttling in KuroSubtitleEngine.
//   - Season/episode filtering requires exact show name matching.
//   - No API key; may break if the HTML structure changes.
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
class Addic7edProvider @Inject constructor(
    private val client: OkHttpClient,
) : SubtitleProvider {

    override val id = "addic7ed"
    override val name = "Addic7ed"
    override val requiresAuth = false

    private val baseUrl = "https://www.addic7ed.com"

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search.php?search=$encodedQuery&Submit=Search"

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
        val rows = doc.select("table.tablespacing tr:has(td)")

        return rows.mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size < 6) return@mapNotNull null

            val language = cols.getOrNull(3)?.text()?.trim() ?: return@mapNotNull null
            val filteredLanguages = languages.takeIf { it.isNotEmpty() }
            if (filteredLanguages != null && !filteredLanguages.any { lang ->
                    language.contains(lang, ignoreCase = true) ||
                    language.contains(langToEnglish(lang), ignoreCase = true)
                }
            ) return@mapNotNull null

            val showName = cols.getOrNull(0)?.text()?.trim() ?: return@mapNotNull null
            val episodeTag = cols.getOrNull(1)?.text()?.trim() ?: return@mapNotNull null
            val (epSeason, epNumber) = parseEpisodeTag(episodeTag)
            val downloadHref = cols.getOrNull(9)?.select("a")?.first()?.attr("href")
                ?: cols.getOrNull(9)?.select("a")?.attr("href")

            val episodeMatch = episodeInfo?.let { ep ->
                (ep.seasonNumber == null || ep.seasonNumber == epSeason) &&
                        (ep.episodeNumber == null || ep.episodeNumber == epNumber)
            } ?: true

            if (!episodeMatch) return@mapNotNull null

            SubtitleCandidate(
                id = "addic7ed_${showName}_${epSeason}x${epNumber}_$language",
                mediaId = showName,
                languageCode = languageCodeFromAddic7ed(language),
                languageName = language,
                label = "$showName - S${epSeason.toString().padStart(2, '0')}E${epNumber.toString().padStart(2, '0')}",
                format = SubtitleFormat.SRT,
                sourceUrl = if (downloadHref.isNullOrBlank()) null else baseUrl + downloadHref,
                providerId = id,
            )
        }
    }

    private fun parseEpisodeTag(tag: String): Pair<Int?, Int?> {
        val regex = Regex("""(\d+)x(\d+)""")
        val match = regex.find(tag)
        return if (match != null) {
            Pair(match.groupValues[1].toIntOrNull(), match.groupValues[2].toIntOrNull())
        } else {
            Pair(null, null)
        }
    }

    private fun languageCodeFromAddic7ed(name: String): String = when {
        name.contains("English", ignoreCase = true) -> "en"
        name.contains("Spanish", ignoreCase = true) -> "es"
        name.contains("French", ignoreCase = true) -> "fr"
        name.contains("German", ignoreCase = true) -> "de"
        name.contains("Portuguese", ignoreCase = true) -> "pt"
        name.contains("Italian", ignoreCase = true) -> "it"
        name.contains("Russian", ignoreCase = true) -> "ru"
        name.contains("Arabic", ignoreCase = true) -> "ar"
        name.contains("Hindi", ignoreCase = true) -> "hi"
        else -> name.take(2).lowercase()
    }

    private fun langToEnglish(code: String): String = when (code.lowercase()) {
        "en" -> "English"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "pt" -> "Portuguese"
        "it" -> "Italian"
        "ru" -> "Russian"
        else -> code
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? = candidate.sourceUrl

    override suspend fun getLanguages(): List<String> = emptyList()

    override fun isEnabled(): Boolean = true
}
