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

package com.kurostream.data.local.dao

import androidx.room.*
import com.kurostream.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query("SELECT * FROM purchases ORDER BY purchase_date DESC")
    fun observeAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY purchase_date DESC")
    suspend fun getAllPurchases(): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE status = 'active' ORDER BY purchase_date DESC")
    fun observeActivePurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE status = 'active' ORDER BY purchase_date DESC")
    suspend fun getActivePurchases(): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE product_id = :productId")
    suspend fun getPurchase(productId: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE product_id = :productId")
    fun observePurchase(productId: String): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchases WHERE product_type = :productType AND status = 'active' ORDER BY purchase_date DESC")
    suspend fun getPurchasesByType(productType: String): List<PurchaseEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM purchases WHERE product_id = :productId AND status = 'active')")
    suspend fun isProductOwned(productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<PurchaseEntity>)

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Delete
    suspend fun delete(purchase: PurchaseEntity)

    @Query("DELETE FROM purchases WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM purchases WHERE status = 'expired' OR status = 'refunded'")
    suspend fun deleteInactivePurchases()

    @Query("UPDATE purchases SET sync_status = :status WHERE product_id = :productId")
    suspend fun updateSyncStatus(productId: String, status: String)

    @Query("UPDATE purchases SET status = :status, refunded_at = :refundedAt, refund_reason = :reason WHERE product_id = :productId")
    suspend fun markAsRefunded(productId: String, status: String, refundedAt: Long, reason: String?)
}