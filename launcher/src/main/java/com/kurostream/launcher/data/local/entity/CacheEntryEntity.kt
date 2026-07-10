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

package com.kurostream.launcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kurostream.launcher.cache.CachePriority
import com.kurostream.launcher.cache.CacheStatus

@Entity(tableName = "cache_entries")
data class CacheEntryEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val mediaUrl: String,
    val localPath: String,
    val status: CacheStatus,
    val priority: CachePriority,
    val progress: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val scheduledAt: Long = 0L,
    val completedAt: Long? = null,
    val lastAccessed: Long = 0L,
    val failureCount: Int = 0
)
