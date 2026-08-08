// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.network.security

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Per-host authentication interceptor.
 *
 * Maps host patterns to header injection rules so each API's auth can be
 * configured in one place without per-Retrofit interceptor duplication.
 */
class AuthInterceptor : Interceptor {

    data class AuthRule(
        val hostPattern: String,
        val headerName: String,
        val headerValue: String,
    )

    private val rules = mutableListOf<AuthRule>()

    fun addRule(hostPattern: String, headerName: String, headerValue: String) {
        rules.add(AuthRule(hostPattern, headerName, headerValue))
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        val matchingRule = rules.firstOrNull { rule ->
            host == rule.hostPattern || host.endsWith(".${rule.hostPattern}")
        } ?: return chain.proceed(original)

        val newRequest = original.newBuilder()
            .header(matchingRule.headerName, matchingRule.headerValue)
            .build()
        return chain.proceed(newRequest)
    }
}
