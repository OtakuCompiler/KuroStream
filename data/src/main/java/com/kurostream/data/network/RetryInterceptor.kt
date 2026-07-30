package com.kurostream.data.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 1000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        repeat(maxRetries) { attempt ->
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || !response.code.shouldRetry()) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                lastException = e
                Timber.w(e, "Request failed (attempt ${attempt + 1}/$maxRetries)")
            }

            if (attempt < maxRetries - 1) {
                val delay = baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
                Thread.sleep(delay)
            }
        }

        throw lastException ?: IOException("Max retries exceeded")
    }

    private val Int.shouldRetry: Boolean
        get() = this in 500..599 || this == 408 || this == 429
}
