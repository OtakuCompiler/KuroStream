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

package com.kurostream.data.anistream.addons

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonDao {

    @Query("SELECT * FROM addons ORDER BY installedAt DESC")
    fun getAllFlow(): Flow<List<Addon>>

    @Query("SELECT * FROM addons WHERE id = :id")
    suspend fun getById(id: String): Addon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(addon: Addon)

    @Update
    suspend fun update(addon: Addon)

    @Query("DELETE FROM addons WHERE id = :id")
    suspend fun deleteById(id: String)
}
