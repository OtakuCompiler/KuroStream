package com.kurostream.data.kurocloud.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KuroEntitlementsDao {
    @Query("SELECT * FROM kuro_entitlements WHERE userId = :userId")
    fun observeEntitlements(userId: String): Flow<KuroEntitlementsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KuroEntitlementsEntity)

    @Update
    suspend fun update(entity: KuroEntitlementsEntity)

    @Delete
    suspend fun delete(entity: KuroEntitlementsEntity)

    @Query("DELETE FROM kuro_entitlements")
    suspend fun clear()
}

@Dao
interface KuroCatalogDao {
    @Query("SELECT * FROM kuro_catalog ORDER BY type, name")
    fun observeAllCatalog(): Flow<List<KuroCatalogEntity>>

    @Query("SELECT * FROM kuro_catalog WHERE type = :type")
    fun observeCatalogByType(type: String): Flow<List<KuroCatalogEntity>>

    @Query("SELECT * FROM kuro_catalog WHERE itemId = :itemId")
    suspend fun getById(itemId: String): KuroCatalogEntity?

    @Query("SELECT * FROM kuro_catalog WHERE skin_id = :skinId")
    suspend fun getBySkinId(skinId: String): KuroCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KuroCatalogEntity>)

    @Delete
    suspend fun delete(item: KuroCatalogEntity)

    @Query("DELETE FROM kuro_catalog")
    suspend fun clear()
}

@Dao
interface KuroPurchaseDao {
    @Query("SELECT * FROM kuro_purchases ORDER BY created_at DESC")
    fun observePurchases(): Flow<List<KuroPurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KuroPurchaseEntity>)

    @Query("DELETE FROM kuro_purchases WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM kuro_purchases")
    suspend fun clear()
}

@Dao
interface KuroCatalogMetaDao {
    @Query("SELECT * FROM kuro_catalog_meta WHERE id = 1")
    fun observeMeta(): Flow<KuroCatalogMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: KuroCatalogMetaEntity)

    @Update
    suspend fun update(meta: KuroCatalogMetaEntity)
}