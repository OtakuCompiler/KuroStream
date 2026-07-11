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

package com.kurostream.data.anistream.introskip

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntroSkipDao {

    @Query("SELECT * FROM intro_skips WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun getForEpisode(animeId: String, episodeNumber: Int): IntroSkipEntity?

    @Query("SELECT * FROM intro_skips WHERE animeId = :animeId")
    fun getForAnimeFlow(animeId: String): Flow<List<IntroSkipEntity>>

    @Query("SELECT * FROM intro_skips")
    suspend fun getAll(): List<IntroSkipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IntroSkipEntity)

    @Update
    suspend fun update(entity: IntroSkipEntity)

    @Query("DELETE FROM intro_skips WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun deleteForEpisode(animeId: String, episodeNumber: Int)

    @Query("UPDATE intro_skips SET introStartSec = :startSec, introEndSec = :endSec, updatedAt = :timestamp WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun updateIntro(animeId: String, episodeNumber: Int, startSec: Double, endSec: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE intro_skips SET outroStartSec = :startSec, outroEndSec = :endSec, updatedAt = :timestamp WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun updateOutro(animeId: String, episodeNumber: Int, startSec: Double, endSec: Double, timestamp: Long = System.currentTimeMillis())
}
