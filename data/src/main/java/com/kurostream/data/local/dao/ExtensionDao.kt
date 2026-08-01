package com.kurostream.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kurostream.data.local.entity.ExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM extensions")
    fun observeAll(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE id = :id")
    suspend fun getById(id: String): ExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExtensionEntity)

    @Update
    suspend fun update(entity: ExtensionEntity)

    @Delete
    suspend fun delete(entity: ExtensionEntity)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM extensions WHERE isEnabled = 1 AND isInstalled = 1")
    fun observeEnabled(): Flow<List<ExtensionEntity>>

    @Query("UPDATE extensions SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}
