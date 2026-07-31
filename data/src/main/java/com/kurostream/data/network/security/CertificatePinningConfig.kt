package com.kurostream.data.network.security

import okhttp3.CertificatePinner

object CertificatePinningConfig {
    
    fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("graphql.anilist.co", 
                "sha256/PLACEHOLDER_1",
                "sha256/PLACEHOLDER_2")
            .add("api.myanimelist.net",
                "sha256/PLACEHOLDER_3",
                "sha256/PLACEHOLDER_4")
            .add("api.themoviedb.org",
                "sha256/PLACEHOLDER_5",
                "sha256/PLACEHOLDER_6")
            .build()
    }
}
