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

package com.kurostream.plugin.sdk.api

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.VideoSource

interface TorrentSource : ExtensionApi {
    override suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>> {
        return Result.error(UnsupportedOperationException("Torrent sources handled by TorrentService"))
    }

    override fun getCapabilities(): Set<ExtensionCapability> {
        return setOf(ExtensionCapability.TORRENT_STREAMING)
    }
}