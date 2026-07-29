package com.kurostream.domain.security

class PermissiveSignatureVerifier : SignatureVerifier {
    override fun verify(apkPath: String): Result<String> {
        return Result.success("permissive-fingerprint")
    }
}
