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
import com.kurostream.data.local.entity.HomeRowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeRowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: HomeRowEntity)

    @Query("SELECT * FROM home_rows WHERE profile_id = :profileId ORDER BY order_index")
    suspend fun getByProfileId(profileId: String): List<HomeRowEntity>

    @Query("SELECT * FROM home_rows WHERE profile_id = :profileId AND is_visible = 1 ORDER BY order_index")
    fun observeVisibleByProfileId(profileId: String): Flow<List<HomeRowEntity>>

    @Query("DELETE FROM home_rows WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM home_rows WHERE profile_id = :profileId")
    suspend fun deleteByProfileId(profileId: String)

    @Query("SELECT * FROM home_rows")
    suspend fun getAll(): List<HomeRowEntity>
}