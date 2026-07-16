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

package com.kurostream.domain.repository

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeAllProfiles(): Flow<List<Profile>>
    fun observeActiveProfile(): Flow<Profile?>
    suspend fun getActiveProfile(): Profile?
    suspend fun getProfileById(id: String): Profile?

    suspend fun createProfile(name: String, avatarUrl: String? = null, pin: String? = null): Result<Profile>
    suspend fun updateProfile(id: String, name: String? = null, avatarUrl: String? = null): Result<Profile>
    suspend fun switchProfile(profileId: String): Result<Profile>
    suspend fun deleteProfile(profileId: String): Result<Unit>

    suspend fun setPin(profileId: String, pin: String): Result<Unit>
    suspend fun removePin(profileId: String): Result<Unit>
    suspend fun verifyPin(profileId: String, pin: String): Boolean
    suspend fun hasPin(profileId: String): Boolean

    suspend fun updatePreferences(profileId: String, preferencesJson: String): Result<Unit>
    suspend fun getPreferences(profileId: String): String?

    suspend fun getProfiles(): List<Profile>
    suspend fun getProfile(profileId: String): Profile?
    suspend fun saveProfile(profile: Profile): Result<Unit>
    suspend fun setActiveProfile(profileId: String): Result<Unit>
}
