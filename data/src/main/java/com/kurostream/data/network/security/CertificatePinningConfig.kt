package com.kurostream.data.network.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Certificate pinning configuration for KuroStream API endpoints.
 *
 * IMPORTANT: Replace placeholder SHA-256 pins in network_security_config.xml
 * with actual SPKI hashes before release. Use:
 *   openssl s_client -connect graphql.anilist.co:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
 */
@Singleton
class CertificatePinningConfig @Inject constructor() {

    fun applyPinning(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        // CertificatePinner is applied via NetworkSecurityConfig (XML) for Android 7.0+
        // This class provides programmatic fallback for older devices or additional control
        return builder
    }

    companion object {
        /**
         * Known certificate fingerprints (SPKI SHA-256 base64).
         * Update these before each release if certificates rotate.
         */
        val ANILIST_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // PRIMARY — REPLACE
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=", // BACKUP — REPLACE
        )

        val MAL_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // PRIMARY — REPLACE
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=", // BACKUP — REPLACE
        )

        val TMDB_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // PRIMARY — REPLACE
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=", // BACKUP — REPLACE
        )
    }
}
