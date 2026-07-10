// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.plugin.sdk.sandbox

import com.kurostream.common.result.Result
import com.kurostream.plugin.sdk.api.ExtensionApi
import com.kurostream.plugin.sdk.api.ExtensionConfig
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.AnimeDetails
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.VideoSource

class ExtensionSandbox(
    private val delegate: ExtensionApi,
    private val policy: SandboxPolicy
) : ExtensionApi {
    override val extensionId: String = delegate.extensionId
    override val info: ExtensionInfo = delegate.info
    private var isDestroyed = false

    override suspend fun onCreate(config: ExtensionConfig) { checkNotDestroyed(); delegate.onCreate(config) }
    override suspend fun onEnable() { checkNotDestroyed(); delegate.onEnable() }
    override suspend fun onDisable() { checkNotDestroyed(); delegate.onDisable() }
    override suspend fun onDestroy() { if (isDestroyed) return; isDestroyed = true; runCatching { delegate.onDestroy() } }
    override fun getCapabilities(): Set<ExtensionCapability> { checkNotDestroyed(); return delegate.getCapabilities() }
    override suspend fun getHomeRows(): Result<List<HomeRow>> { checkNotDestroyed(); return delegate.getHomeRows() }
    override suspend fun search(query: String, page: Int, limit: Int): Result<List<MediaItem>> { checkNotDestroyed(); return delegate.search(query, page, limit) }
    override suspend fun getAnimeDetails(mediaId: String): Result<AnimeDetails> { checkNotDestroyed(); return delegate.getAnimeDetails(mediaId) }
    override suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>> { checkNotDestroyed(); return delegate.getVideoSources(episodeId) }
    override suspend fun getSubtitleCandidates(episodeId: String): Result<List<SubtitleCandidate>> { checkNotDestroyed(); return delegate.getSubtitleCandidates(episodeId) }
    override suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float): Result<Unit> { checkNotDestroyed(); return delegate.reportProgress(mediaId, episodeNumber, progressPercent) }
    override suspend fun syncWatchlist(): Result<List<MediaItem>> { checkNotDestroyed(); return delegate.syncWatchlist() }

    private fun checkNotDestroyed() { if (isDestroyed) throw IllegalStateException("Extension $extensionId has been destroyed") }
}

data class SandboxPolicy(
    val initTimeoutMs: Long = 10_000L,
    val callTimeoutMs: Long = 30_000L,
    val maxMemoryMb: Int = 128,
    val allowNetwork: Boolean = true,
    val allowFileRead: Boolean = true,
    val allowFileWrite: Boolean = false,
    val allowedHosts: List<String> = emptyList()
)
