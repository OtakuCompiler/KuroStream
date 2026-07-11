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

package com.kurostream.domain.legacy.repository

import com.kurostream.common.result.Result
import com.kurostream.domain.entity.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeActiveProfile(): Flow<Profile?>
    fun observeAllProfiles(): Flow<List<Profile>>
    suspend fun createProfile(name: String, avatarUrl: String? = null): Result<Profile>
    suspend fun updateProfile(profile: Profile): Result<Unit>
    suspend fun deleteProfile(profileId: String): Result<Unit>
    suspend fun setActiveProfile(profileId: String): Result<Unit>
}
