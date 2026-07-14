package com.kurostream.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kurostream.launcher.data.local.entity.LibraryItemEntity

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_items WHERE source = :source")
    suspend fun getItemsBySource(source: String): List<LibraryItemEntity>

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getItem(id: String): LibraryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
