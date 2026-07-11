package com.kurostream.legacyui.anistream.ui.subtitle

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.kurostream.data.anistream.subtitle.SubtitleCache
import com.kurostream.data.anistream.subtitle.SubtitleMatcher
import com.kurostream.data.anistream.subtitle.SubtitleProvider
import com.kurostream.data.anistream.subtitle.SubtitleSearchQuery
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleInjector @Inject constructor(
    private val subtitleProviders: List<SubtitleProvider>,
    private val subtitleMatcher: SubtitleMatcher,
    private val subtitleCache: SubtitleCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun autoLoadSubtitle(
        player: ExoPlayer,
        videoUri: Uri,
        videoFile: File? = null,
        animeTitle: String,
        episodeNumber: Int? = null,
        preferredLanguage: String = "en"
    ) {
        scope.launch {
            // 1. Check local files first
            videoFile?.let { file ->
                val localSubs = subtitleMatcher.scanLocalSubtitles(file)
                if (localSubs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        attachSubtitle(player, localSubs.first())
                    }
                    return@launch
                }

                val hash = subtitleMatcher.calculateHash(file)
                searchAndAttach(
                    player = player,
                    query = SubtitleSearchQuery(
                        movieHash = hash,
                        movieByteSize = file.length(),
                        languages = listOf(preferredLanguage)
                    ),
                    videoFileName = file.name,
                    preferredLanguage = preferredLanguage
                )
                return@launch
            }

            // 3. Fallback to name-based search
            searchAndAttach(
                player = player,
                query = SubtitleSearchQuery(
                    query = "$animeTitle ${episodeNumber ?: ""}".trim(),
                    season = 1,
                    episode = episodeNumber,
                    languages = listOf(preferredLanguage)
                ),
                videoFileName = animeTitle,
                preferredLanguage = preferredLanguage
            )
        }
    }

    private suspend fun searchAndAttach(
        player: ExoPlayer,
        query: SubtitleSearchQuery,
        videoFileName: String,
        preferredLanguage: String
    ) {
        for (provider in subtitleProviders) {
            val result = provider.searchSubtitles(query)
            result.onSuccess { subtitles ->
                val bestMatch = subtitleMatcher.findBestMatch(
                    videoFileName,
                    subtitles,
                    preferredLanguage
                )

                bestMatch?.let { subtitle ->
                    val subtitleFile = downloadAndCache(subtitle, provider)
                    withContext(Dispatchers.Main) {
                        attachSubtitle(player, subtitleFile)
                    }
                    return
                }
            }
        }
    }

    private suspend fun downloadAndCache(
        subtitle: com.kurostream.data.anistream.subtitle.SubtitleResult,
        provider: SubtitleProvider
    ): File {
        subtitleCache.getCachedSubtitle(subtitle.id)?.let { return it }

        val downloadResult = provider.downloadSubtitle(subtitle.id)
        return downloadResult.getOrThrow().let { file ->
            subtitleCache.cacheSubtitle(subtitle.id, file)
        }
    }

    private fun attachSubtitle(player: ExoPlayer, subtitleFile: File) {
        val subtitleUri = Uri.fromFile(subtitleFile)
        val subtitleMimeType = when (subtitleFile.extension.lowercase()) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ssa", "ass" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }

        val subtitleItem = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(subtitleMimeType)
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val currentMediaItem = player.currentMediaItem?.buildUpon()
            ?.setSubtitleConfigurations(listOf(subtitleItem))
            ?.build()

        currentMediaItem?.let { player.setMediaItem(it) }
    }

    fun cleanup() {
        scope.cancel()
    }
}
