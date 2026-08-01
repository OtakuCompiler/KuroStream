package com.kurostream.data.subtitle

import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.model.EpisodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleDownloadManager @Inject constructor(
    private val subtitleEngine: KuroSubtitleEngine,
) {

    suspend fun searchAndDownloadBest(
        title: String,
        languages: List<String> = listOf("en", "ja"),
        episodeInfo: EpisodeInfo? = null,
    ): File? = withContext(Dispatchers.IO) {
        val candidates = subtitleEngine.searchSubtitles(title, languages, episodeInfo)
        val best = subtitleEngine.selectBestSubtitle(candidates)
        best?.let { subtitleEngine.downloadSubtitle(it, title) }
    }
}