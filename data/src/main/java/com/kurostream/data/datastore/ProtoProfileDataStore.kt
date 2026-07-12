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

package com.kurostream.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.kurostream.datastore.ProfileProto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtoProfileDataStore @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<ProfileProto.ProfilePreferences> = context.profileDataStore(
        fileName = "profile_prefs.pb",
        serializer = ProfileSerializer
    )

    val profilePrefsFlow: Flow<ProfileProto.ProfilePreferences> = dataStore.data

    suspend fun setActiveProfileId(profileId: String) {
        dataStore.updateData { currentPrefs ->
            currentPrefs.toBuilder().setActiveProfileId(profileId).build()
        }
    }

    fun getActiveProfileId(): Flow<String?> = dataStore.data.map { it.activeProfileId.ifEmpty { null } }

    suspend fun clearActiveProfile() {
        dataStore.updateData { currentPrefs ->
            currentPrefs.toBuilder().clearActiveProfileId().build()
        }
    }
}

val Context.profileDataStore: DataStore<ProfileProto.ProfilePreferences>
    get() = ProtoProfileDataStore(this).dataStore
