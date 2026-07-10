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

package com.kurostream.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class DownloadTask(
    val id: String,
    val mediaId: String,
    val episodeId: String? = null,
    val title: String,
    val url: String,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus { PENDING, QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }
