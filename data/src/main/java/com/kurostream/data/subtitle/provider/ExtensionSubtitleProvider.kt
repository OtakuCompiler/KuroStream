// This file is part of KuroStream.
//
// ExtensionSubtitleProvider — bridge for extension-based subtitle sources
// (Stremio addons, Cloudstream providers, Nuvio extensions).
// Keeps the interface aligned with SubtitleProvider so the ranking engine
// can treat extensions as first-class subtitle sources.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.SubtitleFormat
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionSubtitleProvider @Inject constructor() : SubtitleProvider {

    override val id = "extension"
    override val name = "Extensions"
    override val requiresAuth = false

    override suspend fun search(
        query: String,
        languages: List<String>,
        episodeInfo: EpisodeInfo?,
    ): List<SubtitleCandidate> = withContext(Dispatchers.IO) {
        try {
            val extensionResults = emptyList<SubtitleCandidate>()
            extensionResults
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDownloadUrl(candidate: SubtitleCandidate): String? = candidate.sourceUrl
    override suspend fun getLanguages(): List<String> = emptyList()
    override fun isEnabled(): Boolean = true
}
