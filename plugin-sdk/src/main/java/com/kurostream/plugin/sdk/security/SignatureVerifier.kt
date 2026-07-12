// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.plugin.sdk.security

import com.kurostream.core.common.result.Result
import javax.inject.Inject

interface SignatureVerifier {
    suspend fun verify(path: String): Result<String>
    fun isTrusted(fingerprint: String): Boolean
    suspend fun trustFingerprint(fingerprint: String): Result<Unit>
    suspend fun revokeFingerprint(fingerprint: String): Result<Unit>
}

class PermissiveSignatureVerifier @Inject constructor() : SignatureVerifier {
    override suspend fun verify(path: String): Result<String> = Result.Success("DEV_TRUSTED")
    override fun isTrusted(fingerprint: String): Boolean = true
    override suspend fun trustFingerprint(fingerprint: String): Result<Unit> = Result.Success(Unit)
    override suspend fun revokeFingerprint(fingerprint: String): Result<Unit> = Result.Success(Unit)
}
