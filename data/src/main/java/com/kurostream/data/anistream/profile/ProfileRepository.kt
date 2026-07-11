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

package com.kurostream.data.anistream.profile

import com.kurostream.legacyui.anistream.ui.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val prefs: ProfilePreferences
) {

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: Flow<UserProfile?> = _activeProfile.asStateFlow()

    fun getAllProfiles(): Flow<List<UserProfile>> = profileDao.getAllFlow()

    suspend fun getProfileById(id: String): UserProfile? = profileDao.getById(id)

    suspend fun createProfile(profile: UserProfile) {
        profileDao.insert(profile)
        if (profileDao.getCount() == 1) {
            setActiveProfile(profile.id)
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        profileDao.update(profile)
        if (prefs.getActiveProfileId() == profile.id) {
            _activeProfile.value = profile
        }
    }

    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteById(profileId)
        if (prefs.getActiveProfileId() == profileId) {
            prefs.clearActiveProfile()
            _activeProfile.value = null
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        prefs.setActiveProfileId(profileId)
        _activeProfile.value = profileDao.getById(profileId)
    }

    fun getActiveProfileId(): String? = prefs.getActiveProfileId()

    suspend fun updateWatchTime(profileId: String, minutes: Int) {
        profileDao.updateWatchTime(profileId, minutes)
    }

    suspend fun resetDailyWatchTime() {
        profileDao.resetAllWatchTime()
    }
}
