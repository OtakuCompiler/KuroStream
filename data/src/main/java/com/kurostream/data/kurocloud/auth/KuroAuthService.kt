package com.kurostream.data.kurocloud.auth

import com.kurostream.data.kurocloud.KuroAuthTokens
import com.kurostream.data.kurocloud.KuroSignInRequest
import com.kurostream.data.kurocloud.KuroSignUpRequest
import com.kurostream.data.kurocloud.KuroRefreshRequest
import retrofit2.http.*

interface KuroAuthService {
    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Header("apikey") anonKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: KuroSignInRequest
    ): KuroAuthTokens

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") anonKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: KuroSignUpRequest
    ): KuroAuthTokens

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") anonKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: KuroRefreshRequest
    ): KuroAuthTokens
}