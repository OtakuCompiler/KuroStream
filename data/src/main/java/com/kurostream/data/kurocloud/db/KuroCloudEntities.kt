package com.kurostream.data.kurocloud.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kuro_entitlements")
data class KuroEntitlementsEntity(
    @PrimaryKey val userId: String,
    @ColumnInfo(name = "owned_item_ids") val ownedItemIds: String, // JSON array
    @ColumnInfo(name = "has_skins_pass") val hasSkinsPass: Boolean,
    @ColumnInfo(name = "active_skin_id") val activeSkinId: String?,
    @ColumnInfo(name = "last_synced") val lastSynced: Long,
)

@Entity(tableName = "kuro_catalog")
data class KuroCatalogEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val description: String?,
    val price: Double,
    val currency: String,
    @ColumnInfo(name = "skin_id") val skinId: String?,
    val type: String,
    val tier: String?,
    @ColumnInfo(name = "preview_image_url") val previewImageUrl: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "kuro_purchases")
data class KuroPurchaseEntity(
    @PrimaryKey val id: String, // itemId_timestamp
    @ColumnInfo(name = "item_id") val itemId: String,
    val amount: Double,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "kuro_catalog_meta")
data class KuroCatalogMetaEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long,
    @ColumnInfo(name = "etag") val etag: String?,
)