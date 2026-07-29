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

package com.kurostream.marketplace

/**
 * Shared types for KuroStream Marketplace across all platforms.
 * These types are used by:
 * - Marketplace Website (TypeScript)
 * - Android TV App (Kotlin)
 * - LG webOS App (JavaScript)
 * - Samsung Tizen App (JavaScript)
 */

// ============================================
// PRODUCT TYPES
// ============================================

enum class ProductType {
    SKIN,
    ADDON,
    SUBSCRIPTION
}

enum class Platform(val id: String) {
    ANDROID_TV("android_tv"),
    GOOGLE_TV("google_tv"),
    FIRE_TV("fire_tv"),
    LG_WEBOS("webos"),
    SAMSUNG_TIZEN("tizen"),
    DESKTOP("desktop"),
    ANDROID("android");

    companion object {
        fun fromId(id: String): Platform? = entries.find { it.id == id }
    }
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val longDescription: String? = null,
    val productType: ProductType,
    val category: String,
    val price: Long, // in cents
    val currency: String = "usd",
    val images: List<String> = emptyList(),
    val version: String,
    val minAppVersion: String,
    val platformCompatibility: List<String>,
    val downloadUrl: String,
    val checksum: String,
    val features: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isPremium: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ============================================
// PURCHASE TYPES
// ============================================

enum class PurchaseStatus {
    ACTIVE,
    REFUNDED,
    EXPIRED,
    REVOKED
}

data class Purchase(
    val productId: String,
    val productType: ProductType,
    val version: String,
    val purchaseDate: Long,
    val status: PurchaseStatus,
    val downloadUrl: String,
    val checksum: String,
    val licenseVersion: Int = 1,
    val platformCompatibility: List<String> = emptyList(),
    val lastSync: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val expiresAt: Long? = null,
    val refundedAt: Long? = null,
    val refundReason: String? = null
)

enum class SyncStatus {
    SYNCED,           // In sync with server
    PENDING_UPLOAD,   // Local changes not yet uploaded
    PENDING_VERIFICATION // Local only, needs server verification
}

// ============================================
// USER TYPES
// ============================================

data class User(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val preferredPlatform: Platform = Platform.ANDROID_TV,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val profileId: String,
    val name: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================
// SKIN TYPES
// ============================================

data class SkinManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val previewUrl: String? = null,
    val author: String? = null,
    val minAppVersion: String,
    val platformCompatibility: List<String>,
    val assets: List<SkinAsset> = emptyList()
)

data class SkinAsset(
    val type: AssetType,
    val path: String,
    val url: String? = null
)

enum class AssetType {
    COLOR_SCHEME,
    FONT,
    ICON_SET,
    BACKGROUND_IMAGE,
    ANIMATION,
    SOUND_EFFECT
}

data class InstalledSkin(
    val id: String,
    val name: String,
    val version: String,
    val previewUrl: String? = null,
    val installedAt: Long = System.currentTimeMillis()
)

// ============================================
// EXTENSION TYPES
// ============================================

data class ExtensionManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val minAppVersion: String,
    val requiredPermissions: List<String> = emptyList(),
    val entryPoint: String, // Main script/file to load
    val assets: List<String> = emptyList()
)

data class ExtensionInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val minAppVersion: String,
    val requiredPermissions: List<String> = emptyList(),
    val isOwned: Boolean = false,
    val isEnabled: Boolean = false,
    val purchaseRequired: Boolean = true
)

// ============================================
// SYNC TYPES
// ============================================

data class SyncMetadata(
    val lastSyncAt: Long,
    val syncVersion: Int,
    val pendingChanges: List<String> = emptyList(),
    val deviceTokens: List<String> = emptyList()
)

// ============================================
// API RESPONSE TYPES
// ============================================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

data class OwnershipResponse(
    val owned: Boolean,
    val productId: String,
    val version: String? = null,
    val downloadUrl: String? = null,
    val checksum: String? = null,
    val licenseVersion: Int? = null,
    val status: String? = null
)

data class CheckoutSessionResponse(
    val sessionId: String,
    val url: String
)

// ============================================
// ERROR TYPES
// ============================================

enum class MarketplaceError {
    NOT_AUTHENTICATED,
    PRODUCT_NOT_FOUND,
    PURCHASE_NOT_FOUND,
    ALREADY_OWNED,
    PLATFORM_NOT_SUPPORTED,
    VERSION_NOT_SUPPORTED,
    NETWORK_ERROR,
    PAYMENT_FAILED,
    CHECKSUM_MISMATCH,
    INSTALLATION_FAILED,
    UNKNOWN_ERROR
}