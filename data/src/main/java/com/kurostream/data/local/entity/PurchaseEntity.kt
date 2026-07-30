package com.kurostream.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val purchaseToken: String,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long,
    @ColumnInfo(name = "product_type") val productType: String = "",
    val status: String = "active",
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
    @ColumnInfo(name = "refunded_at") val refundedAt: Long? = null,
    @ColumnInfo(name = "refund_reason") val refundReason: String? = null,
    val isPremium: Boolean = false,
    val isConsumed: Boolean = false,
    val downloadUrl: String = "",
    val checksum: String = ""
)
