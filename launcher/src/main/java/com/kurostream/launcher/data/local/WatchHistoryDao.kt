package com.kurostream.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kurostream.launcher.data.local.entity.WatchHistoryEntity

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE seriesId = :seriesId ORDER BY watchedAt DESC")
    suspend fun getHistoryForSeries(seriesId: String): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    suspend fun getAllHistory(): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun deleteAll()
}
