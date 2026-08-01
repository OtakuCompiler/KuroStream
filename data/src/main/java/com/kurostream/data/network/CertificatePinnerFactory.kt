package com.kurostream.data.network

import okhttp3.CertificatePinner
import timber.log.Timber

object CertificatePinnerFactory {
    fun create(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("api.strem.io", "sha256/CHANGE_ME_CERT_PIN_1")
            .add("api.strem.io", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .add("api.real-debrid.com", "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=")
            .add("api.themoviedb.org", "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=")
            .add("graphql.anilist.co", "sha256/EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE=")
            .build()
    }

    fun logPinInstructions() {
        Timber.i("Get pins: openssl s_client -connect HOST:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64")
    }
}