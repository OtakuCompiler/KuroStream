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

package com.kurostream.plugin.sdk.manager

import com.kurostream.common.result.Result
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.plugin.sdk.api.ExtensionApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ExtensionManager {
    fun observeAllExtensions(): StateFlow<List<ExtensionInfo>>
    fun observeEnabledExtensions(): Flow<List<ExtensionInfo>>
    fun getExtensionApi(extensionId: String): ExtensionApi?
    fun getEnabledApis(): List<ExtensionApi>
    suspend fun install(path: String): Result<ExtensionInfo>
    suspend fun uninstall(extensionId: String): Result<Unit>
    suspend fun enable(extensionId: String): Result<Unit>
    suspend fun disable(extensionId: String): Result<Unit>
    suspend fun refresh(): Result<Unit>
}
