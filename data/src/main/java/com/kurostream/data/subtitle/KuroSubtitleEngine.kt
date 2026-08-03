
// This file is part of KuroStream.
//
// KuroSubtitleEngine — main subtitle orchestrator.
// Discovers, ranks, downloads, caches, and converts subtitle formats.
// Supports:
//   - Native providers: OpenSubtitles, SubDL
//   - Extension providers
//   - Torrent embedded subtitles
//   - HTTP stream subtitles
//   - Local files
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.data.subtitle

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.model.EpisodeInfo
import com.kurostream.domain.subtitle.SubtitlePreferences
import com.kurostream.domain.subtitle.SubtitleSyncEngine
import com.kurostream.data.subtitle.provider.SubtitleProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
@JvmSuppressWildcards
class KuroSubtitleEngine @Inject constructor(
    private val preferences: SubtitlePreferences,
    private val cache: SubtitleCacheManager,
    private val rankingEngine: SubtitleRankingEngine,
    private val syncEngine: SubtitleSyncEngine,
    private val client: OkHttpClient,
    private val providers: Set<SubtitleProvider>,
) {

    suspend fun searchSubtitles(
        query: String,
        languages: List<String> = listOf(preferences.primaryLanguage),
        episodeInfo: EpisodeInfo? = null,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        val all = mutableListOf<SubtitleCandidate>()
        var lastError: Exception? = null
        for (provider in providers) {
            if (!provider.isEnabled()) continue
            try {
                all += provider.search(query, languages, episodeInfo)
            } catch (e: Exception) {
                lastError = e
                Timber.w(e, "Subtitle provider ${provider::class.simpleName} failed")
            }
        }
        if (all.isEmpty() && lastError != null) {
            return@withContext emptyList()
        }
        rankingEngine.rank(all, languages)
    }

    suspend fun selectBestSubtitle(
        candidates: List<SubtitleCandidate>,
    ): SubtitleCandidate? = rankingEngine.selectBest(candidates)

    suspend fun downloadSubtitle(
        candidate: SubtitleCandidate,
        mediaId: String,
    ): File? = withContext(Dispatchers.IO) {
        val url = candidate.sourceUrl ?: return@withContext null
        if (url.startsWith("http", ignoreCase = true)) {
            downloadFromUrl(url, mediaId, candidate.languageCode, candidate.providerId)
        } else {
            val f = File(url)
            if (f.exists()) cacheAndReturn(f, mediaId, candidate.languageCode, candidate.providerId) else null
        }
    }

    suspend fun getTorrentEmbeddedSubtitles(filePath: String): List<SubtitleCandidate> =
        searchSubtitles(filePath, languages = emptyList())

    suspend fun getHttpStreamSubtitles(m3u8Url: String): List<SubtitleCandidate> {
        val candidates = mutableListOf<SubtitleCandidate>()
        try {
            val url = java.net.URL(m3u8Url)
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use candidates
                val body = resp.body?.string() ?: return@use candidates
                val lines = body.lines()
                for (i in lines.indices step 2) {
                    val tag = lines.getOrNull(i) ?: continue
                    val uri = lines.getOrNull(i + 1) ?: continue
                    if (tag.contains("SUBTITLES", ignoreCase = true)) {
                        val langMatch = Regex("""LANGUAGE="([^"]+)"""").find(tag)
                        val lang = langMatch?.groupValues?.get(1) ?: "unknown"
                        val fmt = if (uri.endsWith(".vtt", true)) SubtitleFormat.VTT else SubtitleFormat.SRT
                        candidates += SubtitleCandidate(
                            id = "http_stream_$i",
                            mediaId = m3u8Url,
                            languageCode = lang,
                            languageName = lang,
                            label = "HTTP Stream [$lang]",
                            format = fmt,
                            sourceUrl = uri,
                            isDefault = false,
                            isHearingImpaired = false,
                            providerId = "http_stream",
                        )
                    }
                }
            }
        } catch (e: Exception) {
        }
        return candidates
    }

    private suspend fun downloadFromUrl(
        url: String,
        mediaId: String,
        languageCode: String,
        providerId: String,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                cache.cacheSubtitle(mediaId, languageCode, providerId, bytes)
                val out = File.createTempFile("sub_${languageCode}_", ".srt")
                FileOutputStream(out).use { it.write(bytes) }
                out
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun cacheAndReturn(
        file: File,
        mediaId: String,
        languageCode: String,
        providerId: String,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val bytes = file.readBytes()
            cache.cacheSubtitle(mediaId, languageCode, providerId, bytes)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun detectSubtitleFormat(file: File): SubtitleFormat = when {
        file.name.endsWith(".srt", true) -> SubtitleFormat.SRT
        file.name.endsWith(".ass", true) -> SubtitleFormat.ASS
        file.name.endsWith(".ssa", true) -> SubtitleFormat.ASS
        file.name.endsWith(".vtt", true) -> SubtitleFormat.VTT
        file.name.endsWith(".ttml", true) -> SubtitleFormat.TTML
        file.name.endsWith(".sub", true) -> SubtitleFormat.PGS
        else -> SubtitleFormat.UNKNOWN
    }
}
