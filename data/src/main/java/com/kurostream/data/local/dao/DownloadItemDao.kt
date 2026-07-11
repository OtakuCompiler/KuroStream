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

import androidx.room.*
import com.kurostream.data.local.entity.DownloadItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadItemDao {
    @Query("SELECT * FROM download_items WHERE profileId = :profileId ORDER BY startedAt DESC")
    fun observeByProfile(profileId: String): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM download_items WHERE status = :status")
    suspend fun getByStatus(status: String): List<DownloadItemEntity>

    @Query("SELECT * FROM download_items WHERE mediaItemId = :mediaItemId AND profileId = :profileId")
    suspend fun getByMediaAndProfile(mediaItemId: String, profileId: String): DownloadItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadItemEntity)

    @Update
    suspend fun update(download: DownloadItemEntity)

    @Delete
    suspend fun delete(download: DownloadItemEntity)

    @Query("DELETE FROM download_items WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    @Query("UPDATE download_items SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float)
}
