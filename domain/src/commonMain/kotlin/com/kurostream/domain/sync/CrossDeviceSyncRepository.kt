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

package com.kurostream.domain.sync

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface CrossDeviceSyncRepository {
    suspend fun sync(): Result<Unit>
    suspend fun pushData(data: Any): Result<Unit>
    suspend fun pullData(): Result<Any?>
    
    // Watch progress sync
    suspend fun syncWatchProgress(progress: WatchProgress): Result<Unit>
    suspend fun getRemoteWatchProgress(profileId: String, mediaId: String): Result<WatchProgress?>
    suspend fun getAllRemoteWatchProgress(profileId: String): Result<List<WatchProgress>>
    fun observeRemoteWatchProgress(profileId: String, mediaId: String): Flow<Result<WatchProgress?>>
    
    // Device management
    suspend fun registerDevice(profileId: String, deviceId: String, deviceName: String): Result<Unit>
    suspend fun updateDeviceHeartbeat(deviceId: String): Result<Unit>
    suspend fun getDevicesForProfile(profileId: String): Result<List<DeviceInfo>>
    
    data class DeviceInfo(
        val deviceId: String,
        val profileId: String,
        val deviceName: String,
        val lastActive: Long,
        val appVersion: String
    )
}