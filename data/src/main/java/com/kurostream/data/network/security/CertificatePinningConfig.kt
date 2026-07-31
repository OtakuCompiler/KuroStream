package com.kurostream.data.network.security

import okhttp3.CertificatePinner

object CertificatePinningConfig {
    /**
     * Generates real SHA-256 pins for a host using:
     *   echo | openssl s_client -connect HOST:443 -servername HOST 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null | openssl pkey -pubin -outform DER 2>/dev/null | openssl dgst -sha256 -binary | openssl enc -base64
     *
     * Placeholder pins are intentionally not shipped because CertificatePinner
     * rejects any certificate whose pin does not match exactly, which would
     * break every API call until real pins are generated.
     */
    fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("graphql.anilist.co",
                "sha256/REPLACE_WITH_REAL_PIN_1",
                "sha256/REPLACE_WITH_REAL_PIN_2")
            .add("api.myanimelist.net",
                "sha256/REPLACE_WITH_REAL_PIN_3",
                "sha256/REPLACE_WITH_REAL_PIN_4")
            .add("api.themoviedb.org",
                "sha256/REPLACE_WITH_REAL_PIN_5",
                "sha256/REPLACE_WITH_REAL_PIN_6")
            .build()
    }
}
