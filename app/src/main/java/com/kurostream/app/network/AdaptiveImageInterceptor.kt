package com.kurostream.app.network

import android.graphics.Bitmap
import coil.intercept.Interceptor
import coil.request.CachePolicy
import com.kurostream.common.optimization.BatteryAwareManager

class AdaptiveImageInterceptor(
    private val batteryAwareManager: BatteryAwareManager,
) : Interceptor {
    override suspend fun intercept(
        chain: Interceptor.Chain,
    ): coil.request.ImageResult {
        val request = chain.request
        val shouldReduceQuality = batteryAwareManager.shouldReduceImageQuality()

        val adaptedRequest = request.newBuilder()
            .memoryCachePolicy(
                if (shouldReduceQuality) CachePolicy.WRITE_ONLY
                else CachePolicy.ENABLED
            )
            .diskCachePolicy(CachePolicy.ENABLED)
            .bitmapConfig(
                if (shouldReduceQuality) Bitmap.Config.RGB_565
                else Bitmap.Config.ARGB_8888
            )
            .build()

        return chain.proceed(adaptedRequest)
    }
}
