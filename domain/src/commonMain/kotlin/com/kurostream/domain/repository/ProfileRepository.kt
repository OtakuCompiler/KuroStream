package com.kurostream.domain.repository

import com.kurostream.domain.result.Result
import com.kurostream.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeAllProfiles(): Flow<List<Profile>>
    fun observeActiveProfile(): Flow<Profile?>
    suspend fun getActiveProfile(): Profile?
    suspend fun getProfileById(id: String): Profile?
    suspend fun createProfile(name: String, avatarUrl: String?, pin: String?): Result<Profile>
    suspend fun updateProfile(id: String, name: String?, avatarUrl: String?): Result<Profile>
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
