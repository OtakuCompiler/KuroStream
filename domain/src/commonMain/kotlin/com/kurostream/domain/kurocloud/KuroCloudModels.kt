package com.kurostream.domain.kurocloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KuroUser(
    val id: String,
    val email: String,
    val displayName: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
)

@Serializable
data class KuroEntitlements(
    @SerialName("owned_item_ids") val ownedItemIds: List<String> = emptyList(),
    @SerialName("has_skins_pass") val hasSkinsPass: Boolean = false,
    @SerialName("active_skin_id") val activeSkinId: String?,
)

@Serializable
data class KuroMeResponse(
    val user: KuroUser,
    val entitlements: KuroEntitlements,
    val purchases: List<KuroPurchase> = emptyList(),
)

@Serializable
data class KuroPurchase(
    @SerialName("item_id") val itemId: String,
    val amount: Double,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class KuroCatalogItem(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val currency: String = "USD",
    @SerialName("skin_id") val skinId: String?,
    val type: String,
    val tier: String?,
    @SerialName("preview_image_url") val previewImageUrl: String?,
)

@Serializable
data class KuroCatalogResponse(
    val skins: List<KuroCatalogItem> = emptyList(),
    val passes: List<KuroCatalogItem> = emptyList(),
    @SerialName("updated_at") val updatedAt: String?,
)

@Serializable
data class KuroAuthTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class KuroSignInRequest(
    val email: String,
    val password: String,
)

@Serializable
data class KuroSignUpRequest(
    val email: String,
    val password: String,
    @SerialName("data") val data: Map<String, String> = emptyMap(),
)

@Serializable
data class KuroRefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class KuroClaimPurchaseRequest(
    @SerialName("item_id") val itemId: String,
)

@Serializable
data class KuroSetActiveSkinRequest(
    @SerialName("item_id") val itemId: String,
)

@Serializable
data class KuroSyncResponse(
    val added: KuroSyncCounts = KuroSyncCounts(),
    val deleted: KuroSyncCounts = KuroSyncCounts(),
)

@Serializable
data class KuroSyncCounts(
    val movies: Int = 0,
    val episodes: Int = 0,
    val skins: Int = 0,
)