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
import com.kurostream.data.local.entity.AddonConfigEntity

@Dao
interface AddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AddonConfigEntity)

    @Query("SELECT * FROM addon_configs WHERE extensionId = :extensionId")
    suspend fun getByExtensionId(extensionId: String): AddonConfigEntity?

    @Query("SELECT * FROM addon_configs WHERE isEnabled = 1")
    suspend fun getEnabled(): List<AddonConfigEntity>

    @Query("SELECT * FROM addon_configs")
    suspend fun getAll(): List<AddonConfigEntity>
}