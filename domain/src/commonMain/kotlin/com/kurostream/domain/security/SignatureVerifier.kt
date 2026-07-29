package com.kurostream.domain.security

interface SignatureVerifier {
    fun verify(apkPath: String): Result<String>
}
