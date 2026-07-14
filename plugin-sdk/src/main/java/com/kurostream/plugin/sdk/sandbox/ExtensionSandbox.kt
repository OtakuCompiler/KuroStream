package com.kurostream.plugin.sdk.sandbox

import com.kurostream.core.common.result.Result
import com.kurostream.plugin.sdk.api.ExtensionApi
import com.kurostream.plugin.sdk.api.ExtensionConfig
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.AnimeDetails
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.VideoSource
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class ExtensionSandbox(
    private val delegate: ExtensionApi,
    private val policy: SandboxPolicy
) : ExtensionApi {
    override val extensionId: String = delegate.extensionId
    override val info: ExtensionInfo = delegate.info
    private var isDestroyed = false

    override suspend fun onCreate(config: ExtensionConfig) {
        checkNotDestroyed()
        enforceTimeout("onCreate", policy.initTimeoutMs) { delegate.onCreate(config) }
    }

    override suspend fun onEnable() { checkNotDestroyed(); delegate.onEnable() }
    override suspend fun onDisable() { checkNotDestroyed(); delegate.onDisable() }
    override suspend fun onDestroy() { if (isDestroyed) return; isDestroyed = true; runCatching { delegate.onDestroy() } }
    override fun getCapabilities(): Set<ExtensionCapability> { checkNotDestroyed(); return delegate.getCapabilities() }

    override suspend fun getHomeRows(): Result<List<HomeRow>> {
        checkNotDestroyed()
        return enforceTimeout("getHomeRows", policy.callTimeoutMs) { delegate.getHomeRows() }
    }

    override suspend fun search(query: String, page: Int, limit: Int): Result<List<MediaItem>> {
        checkNotDestroyed()
        return enforceTimeout("search", policy.callTimeoutMs) { delegate.search(query, page, limit) }
    }

    override suspend fun getAnimeDetails(mediaId: String): Result<AnimeDetails> {
        checkNotDestroyed()
        return enforceTimeout("getAnimeDetails", policy.callTimeoutMs) { delegate.getAnimeDetails(mediaId) }
    }

    override suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>> {
        checkNotDestroyed()
        return enforceTimeout("getVideoSources", policy.callTimeoutMs) { delegate.getVideoSources(episodeId) }
    }

    override suspend fun getSubtitleCandidates(episodeId: String): Result<List<SubtitleCandidate>> {
        checkNotDestroyed()
        return enforceTimeout("getSubtitleCandidates", policy.callTimeoutMs) { delegate.getSubtitleCandidates(episodeId) }
    }

    override suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float): Result<Unit> {
        checkNotDestroyed()
        return enforceTimeout("reportProgress", policy.callTimeoutMs) { delegate.reportProgress(mediaId, episodeNumber, progressPercent) }
    }

    override suspend fun syncWatchlist(): Result<List<MediaItem>> {
        checkNotDestroyed()
        return enforceTimeout("syncWatchlist", policy.callTimeoutMs) { delegate.syncWatchlist() }
    }

    private suspend fun <T> enforceTimeout(name: String, timeoutMs: Long, block: suspend () -> T): T {
        return try {
            withTimeout(timeoutMs) { block() }
        } catch (e: TimeoutCancellationException) {
            throw SandboxException("$name timed out after ${timeoutMs}ms for extension $extensionId")
        }
    }

    private fun checkNotDestroyed() {
        if (isDestroyed) throw IllegalStateException("Extension $extensionId has been destroyed")
    }
}

class SandboxException(message: String) : Exception(message)

data class SandboxPolicy(
    val initTimeoutMs: Long = 10_000L,
    val callTimeoutMs: Long = 30_000L,
    val maxMemoryMb: Int = 128,
    val allowNetwork: Boolean = true,
    val allowFileRead: Boolean = true,
    val allowFileWrite: Boolean = false,
    val allowedHosts: List<String> = emptyList()
)
