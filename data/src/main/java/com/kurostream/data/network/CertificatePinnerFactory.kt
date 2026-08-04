package com.kurostream.data.network

import android.os.Build
import okhttp3.CertificatePinner
import timber.log.Timber

object CertificatePinnerFactory {
    /**
     * PRODUCTION: Replace placeholder pins before release.
     * Retrieve each host's current pin with:
     *   echo | openssl s_client -connect HOST:443 -servername HOST 2>/dev/null \
     *     | openssl x509 -noout -pubkey \
     *     | openssl pkey -pubin -outform DER \
     *     | openssl dgst -sha256 -binary | openssl enc -base64
     *
     * Always include a backup pin (next rotation key) so deploys never break.
     * Placeholder pins are DISABLED in debug builds to avoid blocking API calls
     * during development before real pins are generated.
     */
    fun create(isDebugBuild: Boolean = false): CertificatePinner {
        if (isDebugBuild) {
            Timber.w("CertificatePinner: disabled in debug build — replace pins before release")
            return CertificatePinner.Builder().build()
        }
        // TODO before release: replace each pin value with real SHA-256 public-key fingerprints.
        // Current values are intentionally empty to prevent accidental TLS breakage.
        return CertificatePinner.Builder().build()
    }

    fun logPinInstructions() {
        Timber.i("Get pins: echo | openssl s_client -connect HOST:443 -servername HOST 2>/dev/null | openssl x509 -noout -pubkey | openssl pkey -pubin -outform DER | openssl dgst -sha256 -binary | openssl enc -base64")
    }
}