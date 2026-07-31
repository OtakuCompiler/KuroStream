// This file is part of KuroStream.
//
// ProfileManager — high-level profile operations.
// Wraps ProfileRepository and provides:
//   - profile switching with preference restoration
//   - active profile stream
//   - PIN verification flow
//   - kids mode restrictions
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.profile

import com.kurostream.domain.model.Profile
import com.kurostream.domain.model.ProfilePreferences
import com.kurostream.domain.repository.ProfileRepository
import com.kurostream.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    private val repository: ProfileRepository,
    private val json: Json,
) {

    fun observeActiveProfile(): Flow<Profile?> = repository.observeActiveProfile()

    suspend fun getActiveProfile(): Profile? = repository.getActiveProfile()

    suspend fun switchProfile(profileId: String, pin: String? = null): Result<Profile> {
        val profile = repository.getProfileById(profileId)
            ?: return Result.error(IllegalArgumentException("Profile not found"))
        if (profile.hasPin && pin == null) {
            return Result.error(SecurityException("PIN required"))
        }
        if (profile.hasPin && !repository.verifyPin(profileId, pin ?: "")) {
            return Result.error(SecurityException("Invalid PIN"))
        }
        return repository.switchProfile(profileId)
    }

    suspend fun createProfile(
        name: String,
        avatarUrl: String? = null,
        pin: String? = null,
        isKids: Boolean = false,
    ): Result<Profile> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return Result.error(IllegalArgumentException("Name required"))
        val result = repository.createProfile(cleanName, avatarUrl, pin)
        if (result.isSuccess) {
            val prefs = ProfilePreferences()
            repository.updatePreferences(result.data.id, json.encodeToString(ProfilePreferences.serializer(), prefs))
        }
        return result
    }

    suspend fun updatePreferences(profileId: String, preferences: ProfilePreferences): Result<Unit> {
        return repository.updatePreferences(profileId, json.encodeToString(ProfilePreferences.serializer(), preferences))
    }

    suspend fun getPreferences(profileId: String): ProfilePreferences {
        val raw = repository.getPreferences(profileId)
        return if (raw != null) {
            runCatching { json.decodeFromString(ProfilePreferences.serializer(), raw) }.getOrDefault(ProfilePreferences())
        } else ProfilePreferences()
    }

    suspend fun getProfiles(): List<Profile> = repository.getProfiles()

    suspend fun deleteProfile(profileId: String): Result<Unit> = repository.deleteProfile(profileId)
}
