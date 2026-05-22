package com.kurostream.data.source.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM content ORDER BY cachedAt DESC")
    fun getAllContent(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE id = :id")
    suspend fun getContentById(id: String): ContentEntity?

    @Query("SELECT * FROM content WHERE title LIKE '%' || :query || '%'")
    suspend fun searchContent(query: String): List<ContentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ContentEntity)

    @Query("DELETE FROM content WHERE cachedAt < :olderThan")
    suspend fun deleteOldCache(olderThan: Long)
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins ORDER BY addedAt DESC")
    fun getAllPlugins(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE isEnabled = 1")
    fun getEnabledPlugins(): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plugin: PluginEntity)

    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE plugins SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history WHERE contentId = :contentId")
    suspend fun getByContentId(contentId: String): HistoryEntity?

    @Query("UPDATE history SET progressMs = :progressMs, watchedAt = :watchedAt WHERE contentId = :contentId")
    suspend fun updateProgress(contentId: String, progressMs: Long, watchedAt: Long)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)
}
