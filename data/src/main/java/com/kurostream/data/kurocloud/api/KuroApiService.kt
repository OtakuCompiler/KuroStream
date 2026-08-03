package com.kurostream.data.kurocloud.api

import com.kurostream.data.kurocloud.KuroCatalogResponse
import com.kurostream.data.kurocloud.KuroClaimPurchaseRequest
import com.kurostream.data.kurocloud.KuroMeResponse
import com.kurostream.data.kurocloud.KuroPurchase
import com.kurostream.data.kurocloud.KuroSetActiveSkinRequest
import com.kurostream.data.kurocloud.KuroSyncResponse
import retrofit2.http.*

interface KuroApiService {
    @GET("api/public/v1/catalog")
    suspend fun getCatalog(): KuroCatalogResponse

    @GET("api/public/v1/me")
    suspend fun getMe(
        @Header("Authorization") auth: String
    ): KuroMeResponse

    @GET("api/public/v1/purchases")
    suspend fun getPurchases(
        @Header("Authorization") auth: String
    ): List<KuroPurchase>

    @POST("api/public/v1/purchases")
    suspend fun claimPurchase(
        @Header("Authorization") auth: String,
        @Body body: KuroClaimPurchaseRequest
    ): KuroSyncResponse

    @POST("api/public/v1/active-skin")
    suspend fun setActiveSkin(
        @Header("Authorization") auth: String,
        @Body body: KuroSetActiveSkinRequest
    ): KuroSyncResponse
}