package com.kurostream.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kurostream.launcher.data.local.entity.PurchaseEntity

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE userId = :userId")
    suspend fun getPurchasesForUser(userId: String): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchase(id: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedPurchases(userId: String): List<PurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity)

    @Query("UPDATE purchases SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM purchases WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
