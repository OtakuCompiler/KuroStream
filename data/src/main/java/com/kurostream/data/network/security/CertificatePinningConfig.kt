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
    /**
     * Returns a certificate pinner suitable for the current build variant.
     *
     * @param isDebugBuild pass BuildConfig.DEBUG. Pinning is skipped in debug
     *   builds because real pins are not yet provided, and OkHttp would reject
     *   every request against a host that has a non-matching (or absent) pin.
     *
     * BEFORE RELEASE: replace the empty builder with real SHA-256 SPKI pins
     * generated for each host. Include at least one backup/rotation pin per host.
     * Use CertificatePinnerFactory.logPinInstructions() for the correct openssl command.
     */
    fun createCertificatePinner(isDebugBuild: Boolean = false): CertificatePinner {
        if (isDebugBuild) {
            return CertificatePinner.Builder().build()
        }
        // TODO(security): populate with real pins before Play Store release.
        return CertificatePinner.Builder().build()
    }
}
