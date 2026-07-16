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

import com.kurostream.domain.model.Profile
import com.kurostream.domain.repository.ProfileRepository as NewProfileRepository
import kotlinx.coroutines.flow.Flow

@Deprecated(
    message = "Use com.kurostream.domain.repository.ProfileRepository instead. This legacy interface will be removed in a future version.",
    replaceWith = ReplaceWith("import com.kurostream.domain.repository.ProfileRepository")
)
interface ProfileRepository {
    fun observeActiveProfile(): Flow<Profile?>
    fun observeAllProfiles(): Flow<List<Profile>>
    suspend fun createProfile(name: String, avatarUrl: String? = null): com.kurostream.core.common.result.Result<Profile>
    suspend fun updateProfile(profile: Profile): com.kurostream.core.common.result.Result<Unit>
    suspend fun deleteProfile(profileId: String): com.kurostream.core.common.result.Result<Unit>
    suspend fun setActiveProfile(profileId: String): com.kurostream.core.common.result.Result<Unit>
}
