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

package com.kurostream.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kurostream.data.local.entity.SourceLockEntity
import com.kurostream.data.local.entity.SourceLockSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceLockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lock: SourceLockEntity)

    @Update
    suspend fun update(lock: SourceLockEntity)

    @Query("SELECT * FROM source_locks WHERE series_id = :seriesId")
    suspend fun getBySeriesId(seriesId: String): SourceLockEntity?

    @Query("SELECT * FROM source_locks WHERE series_id = :seriesId")
    fun observeBySeriesId(seriesId: String): Flow<SourceLockEntity?>

    @Query("SELECT * FROM source_locks WHERE is_active = 1")
    fun observeAllActive(): Flow<List<SourceLockEntity>>

    @Query("DELETE FROM source_locks WHERE series_id = :seriesId")
    suspend fun deleteBySeriesId(seriesId: String)

    @Query("DELETE FROM source_locks WHERE last_used_at < :threshold AND is_active = 0")
    suspend fun cleanupOldInactive(threshold: Long)

    @Query("DELETE FROM source_locks")
    suspend fun deleteAll()

    // Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SourceLockSettingsEntity)

    @Query("SELECT * FROM source_lock_settings WHERE id = 1")
    suspend fun getSettings(): SourceLockSettingsEntity?

    @Query("SELECT * FROM source_lock_settings WHERE id = 1")
    fun observeSettings(): Flow<SourceLockSettingsEntity?>

    // Fallback logging
    @Query("INSERT INTO source_lock_fallbacks (series_id, from_provider, to_provider, reason, timestamp) VALUES (:seriesId, :fromProvider, :toProvider, :reason, :timestamp)")
    suspend fun recordFallback(
        seriesId: String,
        fromProvider: String,
        toProvider: String,
        reason: String,
        timestamp: Long
    )
}