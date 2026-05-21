package com.kurostream.tv.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for Kuro Stream
 * Stores watch history, favorites, and cached metadata
 */
@Database(
    entities = [
        WatchHistoryEntity::class,
        FavoriteEntity::class,
        CachedAnimeEntity::class,
        WatchProgressEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KuroDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cachedAnimeDao(): CachedAnimeDao
    abstract fun watchProgressDao(): WatchProgressDao
}

// Type converters
class Converters {
    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @androidx.room.TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }
    
    @androidx.room.TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }
    
    @androidx.room.TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull { it.toLongOrNull() }
    }
}

// Entity classes
@androidx.room.Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @androidx.room.PrimaryKey val id: String,
    val animeId: String,
    val animeTitle: String,
    val episodeNumber: Int,
    val episodeTitle: String?,
    val coverImage: String?,
    val watchedAt: Long,
    val watchDurationMs: Long,
    val totalDurationMs: Long,
    val lastPositionMs: Long,
    val isCompleted: Boolean
)

@androidx.room.Entity(tableName = "favorites")
data class FavoriteEntity(
    @androidx.room.PrimaryKey val animeId: String,
    val title: String,
    val coverImage: String?,
    val addedAt: Long,
    val totalEpisodes: Int?,
    val status: String?,
    val rating: Float?
)

@androidx.room.Entity(tableName = "cached_anime")
data class CachedAnimeEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val titleJapanese: String?,
    val description: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val status: String?,
    val releaseYear: Int?,
    val genres: String?, // Comma-separated
    val rating: Float?,
    val totalEpisodes: Int?,
    val cachedAt: Long,
    val malId: Long?,
    val anilistId: Long?,
    val kitsuId: String?
)

@androidx.room.Entity(
    tableName = "watch_progress",
    primaryKeys = ["animeId", "episodeNumber"]
)
data class WatchProgressEntity(
    val animeId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val isCompleted: Boolean
)

// DAOs
@androidx.room.Dao
interface WatchHistoryDao {
    @androidx.room.Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 50): List<WatchHistoryEntity>
    
    @androidx.room.Query("SELECT * FROM watch_history WHERE animeId = :animeId ORDER BY episodeNumber DESC")
    suspend fun getHistoryForAnime(animeId: String): List<WatchHistoryEntity>
    
    @androidx.room.Query("SELECT * FROM watch_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun getHistoryEntry(animeId: String, episodeNumber: Int): WatchHistoryEntity?
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntity)
    
    @androidx.room.Delete
    suspend fun delete(entry: WatchHistoryEntity)
    
    @androidx.room.Query("DELETE FROM watch_history WHERE animeId = :animeId")
    suspend fun deleteForAnime(animeId: String)
    
    @androidx.room.Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@androidx.room.Dao
interface FavoriteDao {
    @androidx.room.Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavorites(): List<FavoriteEntity>
    
    @androidx.room.Query("SELECT * FROM favorites WHERE animeId = :animeId")
    suspend fun getFavorite(animeId: String): FavoriteEntity?
    
    @androidx.room.Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE animeId = :animeId)")
    suspend fun isFavorite(animeId: String): Boolean
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)
    
    @androidx.room.Delete
    suspend fun delete(favorite: FavoriteEntity)
    
    @androidx.room.Query("DELETE FROM favorites WHERE animeId = :animeId")
    suspend fun deleteById(animeId: String)
}

@androidx.room.Dao
interface CachedAnimeDao {
    @androidx.room.Query("SELECT * FROM cached_anime WHERE id = :id")
    suspend fun getById(id: String): CachedAnimeEntity?
    
    @androidx.room.Query("SELECT * FROM cached_anime WHERE malId = :malId")
    suspend fun getByMalId(malId: Long): CachedAnimeEntity?
    
    @androidx.room.Query("SELECT * FROM cached_anime WHERE title LIKE '%' || :query || '%' OR titleJapanese LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<CachedAnimeEntity>
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(anime: CachedAnimeEntity)
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(animeList: List<CachedAnimeEntity>)
    
    @androidx.room.Query("DELETE FROM cached_anime WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @androidx.room.Query("DELETE FROM cached_anime")
    suspend fun clearAll()
}

@androidx.room.Dao
interface WatchProgressDao {
    @androidx.room.Query("SELECT * FROM watch_progress WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun getProgress(animeId: String, episodeNumber: Int): WatchProgressEntity?
    
    @androidx.room.Query("SELECT * FROM watch_progress WHERE animeId = :animeId ORDER BY episodeNumber")
    suspend fun getAllProgressForAnime(animeId: String): List<WatchProgressEntity>
    
    @androidx.room.Query("SELECT * FROM watch_progress WHERE isCompleted = 0 ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getContinueWatching(limit: Int = 20): List<WatchProgressEntity>
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(progress: WatchProgressEntity)
    
    @androidx.room.Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun deleteForAnime(animeId: String)
    
    @androidx.room.Query("UPDATE watch_progress SET isCompleted = 1 WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun markCompleted(animeId: String, episodeNumber: Int)
}
