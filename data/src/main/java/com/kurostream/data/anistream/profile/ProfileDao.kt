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

package com.kurostream.data.anistream.profile

import androidx.room.*
import com.kurostream.legacyui.anistream.ui.profile.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY lastUsedAt DESC")
    fun getAllFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM profiles ORDER BY lastUsedAt DESC")
    suspend fun getAll(): List<UserProfile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): UserProfile?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile)

    @Update
    suspend fun update(profile: UserProfile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE profiles SET watchTimeMinutesToday = watchTimeMinutesToday + :minutes WHERE id = :profileId")
    suspend fun updateWatchTime(profileId: String, minutes: Int)

    @Query("UPDATE profiles SET watchTimeMinutesToday = 0")
    suspend fun resetAllWatchTime()

    @Query("UPDATE profiles SET lastUsedAt = :timestamp WHERE id = :profileId")
    suspend fun updateLastUsed(profileId: String, timestamp: Long = System.currentTimeMillis())
}
