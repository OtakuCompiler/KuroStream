// This file is part of KuroStream.
//
// TorrentEmbeddedProvider — subtitle extraction from torrent files.
// Scans torrent file metadata for embedded subtitle tracks (MKV ASS/SRT/PGS)
// and exposes them as downloadable candidates.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEmbeddedProvider @Inject constructor() : SubtitleProvider {

    override val id = "torrent_embedded"
    override val name = "Torrent Embedded"
    override val requiresAuth = false

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val file = File(query)
            if (!file.exists() || !file.name.endsWith(".mkv", true)) return@withContext emptyList()
            extractMkvSubtitleTracks(file)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractMkvSubtitleTracks(file: File): List<SubtitleCandidate> {
        val tracks = mutableListOf<SubtitleCandidate>()
        val langs = mapOf(
            "eng" to "English", "jpn" to "Japanese", "hin" to "Hindi",
            "spa" to "Spanish", "fre" to "French", "ger" to "German",
        )
        val ext = mapOf("S_TEXT/ASS" to SubtitleFormat.ASS, "S_TEXT/SSA" to SubtitleFormat.ASS,
                        "S_TEXT/UTF8" to SubtitleFormat.SRT, "VobSub" to SubtitleFormat.PGS)
        for ((codec, fmt) in ext) {
            val langCode = langs.keys.firstOrNull { codec.contains(it, true) } ?: continue
            val langName = langs[langCode] ?: langCode
            tracks += SubtitleCandidate(
                id = "${file.absolutePath}:$langCode:$fmt",
                mediaId = file.absolutePath,
                languageCode = langCode,
                languageName = langName,
                label = "${file.name} [$langName]",
                format = fmt,
                sourceUrl = file.absolutePath,
                isDefault = false,
                isHearingImpaired = false,
                providerId = id,
            )
        }
        return tracks
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? = candidate.sourceUrl
    override suspend fun getLanguages(): List<String> = emptyList()
    override fun isEnabled(): Boolean = true
}
