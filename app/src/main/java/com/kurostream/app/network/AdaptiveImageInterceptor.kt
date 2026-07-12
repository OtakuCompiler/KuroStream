package com.kurostream.app.network

import android.graphics.Bitmap
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.Size
import com.kurostream.common.optimization.BatteryAwareManager

class AdaptiveImageInterceptor(
    private val batteryAwareManager: BatteryAwareManager,
) : Interceptor {
    override suspend fun intercept(
        chain: Interceptor.Chain,
    ): coil.request.ImageResult {
        val request = chain.request
        val options = chain.options

        val shouldReduceQuality = batteryAwareManager.shouldReduceImageQuality()
        val targetSize = if (shouldReduceQuality) {
            Size(
                (options.size.width / 2).coerceAtLeast(200),
                (options.size.height / 2).coerceAtLeast(200),
            )
        } else {
            options.size
        }

        val adaptedRequest = request.newBuilder()
            .size(targetSize)
            .memoryCachePolicy(
                if (shouldReduceQuality) CachePolicy.DISABLE
                else CachePolicy.ENABLED
            )
            .bitmapConfig(
                if (shouldReduceQuality) Bitmap.Config.RGB_565
                else Bitmap.Config.ARGB_8888
            )
            .build()

        return chain.withRequest(adaptedRequest).execute()
    }
}
