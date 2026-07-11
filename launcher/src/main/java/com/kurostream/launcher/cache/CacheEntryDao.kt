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

package com.kurostream.launcher.cache

import androidx.room.*
import com.kurostream.launcher.data.local.entity.CacheEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheEntryDao {
    @Query("SELECT * FROM cache_entries WHERE id = :id")
    suspend fun getEntry(id: String): CacheEntryEntity?

    @Query("SELECT * FROM cache_entries")
    suspend fun getAllEntries(): List<CacheEntryEntity>

    @Query("SELECT * FROM cache_entries WHERE seriesId = :seriesId")
    fun getEntriesForSeries(seriesId: String): Flow<List<CacheEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CacheEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CacheEntryEntity>)

    @Query("UPDATE cache_entries SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: CacheStatus)

    @Query("UPDATE cache_entries SET lastAccessed = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)

    @Delete
    suspend fun delete(entry: CacheEntryEntity)

    @Query("DELETE FROM cache_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM cache_entries WHERE status = :status")
    suspend fun getByStatus(status: CacheStatus): List<CacheEntryEntity>
}
