// This file is part of KuroStream.
//
// OpenSubtitlesProvider — native OpenSubtitles integration.
// Extends existing OpenSubtitlesApi with the SubtitleProvider interface.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import com.kurostream.data.remote.api.OpenSubtitlesApi
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSubtitlesProvider @Inject constructor(
    private val api: OpenSubtitlesApi,
) : SubtitleProvider {

    override val id = "opensubtitles"
    override val name = "OpenSubtitles"
    override val requiresAuth = true

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchSubtitles(
                query = query,
                languages = languages.joinToString(","),
                seasonNumber = episodeInfo?.seasonNumber,
                episodeNumber = episodeInfo?.episodeNumber,
            )
            if (response.isSuccessful) {
                response.body()?.data?.mapNotNull { item ->
                    val attr = item.attributes ?: return@mapNotNull null
                    val file = attr.files?.firstOrNull()
                    val format = guessFormat(file?.fileName)
                    SubtitleCandidate(
                        id = item.id ?: return@mapNotNull null,
                        mediaId = item.id,
                        languageCode = attr.language ?: "unknown",
                        languageName = attr.language ?: "unknown",
                        label = file?.fileName,
                        format = format,
                        sourceUrl = attr.url,
                        isDefault = false,
                        isHearingImpaired = attr.hearing_impaired ?: false,
                        providerId = id,
                    )
                } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? =
        candidate.sourceUrl

    override suspend fun getLanguages(): List<String> = emptyList()

    override fun isEnabled(): Boolean = true

    private fun guessFormat(fileName: String?): SubtitleFormat = when {
        fileName == null -> SubtitleFormat.UNKNOWN
        fileName.endsWith(".srt", true) -> SubtitleFormat.SRT
        fileName.endsWith(".ass", true) -> SubtitleFormat.ASS
        fileName.endsWith(".ssa", true) -> SubtitleFormat.ASS
        fileName.endsWith(".vtt", true) -> SubtitleFormat.VTT
        fileName.endsWith(".ttml", true) -> SubtitleFormat.TTML
        else -> SubtitleFormat.UNKNOWN
    }
}
