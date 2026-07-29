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

package com.kurostream.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_locks")
data class SourceLockEntity(
    @PrimaryKey
    @ColumnInfo(name = "series_id")
    val seriesId: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "source_quality")
    val sourceQuality: String,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "episode_count")
    val episodeCount: Int = 0,

    @ColumnInfo(name = "fallback_count")
    val fallbackCount: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
)

@Entity(tableName = "source_lock_settings")
data class SourceLockSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Single row settings

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "fallback_mode_ordinal")
    val fallbackModeOrdinal: Int = 0, // 0 = AUTOMATIC, 1 = MANUAL

    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 2,

    @ColumnInfo(name = "retry_delay_ms")
    val retryDelayMs: Long = 3000,

    @ColumnInfo(name = "persist_across_sessions")
    val persistAcrossSessions: Boolean = true,

    @ColumnInfo(name = "notify_on_fallback")
    val notifyOnFallback: Boolean = true,
)