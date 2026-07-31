// This file is part of KuroStream.
//
// SubtitleProvider — common interface for subtitle sources.
// Implementations:
//   - OpenSubtitlesProvider
//   - SubDLProvider
//   - PodnapisiProvider
//   - TVSubtitlesProvider
//   - Addic7edProvider
//   - ExtensionSubtitleProvider
//   - TorrentEmbeddedProvider
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.flow.Flow

interface SubtitleProvider {
    val id: String
    val name: String
    val requiresAuth: Boolean

    suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo? = null,
    ): List<SubtitleCandidate>

    suspend fun getDownloadUrl(candidate: SubtitleCandidate): String?
    suspend fun getLanguages(): List<String>
    fun isEnabled(): Boolean
}
