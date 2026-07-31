package com.kurostream.data.network.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificatePinningConfig @Inject constructor() {

    fun applyPinning(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        return try {
            val pinner = CertificatePinner.Builder()
                .add("graphql.anilist.co", *ANILIST_PINS.toTypedArray())
                .add("api.myanimelist.net", *MAL_PINS.toTypedArray())
                .add("api.themoviedb.org", *TMDB_PINS.toTypedArray())
                .add("kitsu.io", *KITSU_PINS.toTypedArray())
                .build()
            builder.certificatePinner(pinner)
        } catch (e: Exception) {
            Timber.w(e, "Certificate pinning disabled — using default trust")
            builder
        }
    }

    companion object {
        /**
         * Generate real pins before release:
         *   openssl s_client -connect graphql.anilist.co:443 </dev/null 2>/dev/null \
         *     | openssl x509 -pubkey -noout \
         *     | openssl pkey -pubin -outform der \
         *     | openssl dgst -sha256 -binary \
         *     | openssl enc -base64
         *
         * Placeholder pins mean pinning is effectively disabled until real pins are injected.
         * Do NOT ship to production without replacing these with actual SHA-256 pins.
         */
        val ANILIST_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        )

        val MAL_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        )

        val TMDB_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        )

        val KITSU_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        )
    }
}
