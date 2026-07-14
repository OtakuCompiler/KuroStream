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

package com.kurostream.data.repository

import com.kurostream.data.local.dao.ProfileDao
import com.kurostream.data.local.entity.ProfileEntity
import com.kurostream.domain.model.Profile
import com.kurostream.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao
) : ProfileRepository {

    companion object {
        private const val MAX_PROFILES = 5
        private const val PIN_SALT = "kurostream_pin_salt_v1"
    }

    override fun observeAllProfiles(): Flow<List<Profile>> {
        return profileDao.observeAll().map { it.map { e -> e.toDomain() } }
    }

    override fun observeActiveProfile(): Flow<Profile?> {
        return profileDao.observeActive().map { it?.toDomain() }
    }

    override suspend fun getActiveProfile(): Profile? = profileDao.getActive()?.toDomain()
    override suspend fun getProfileById(id: String): Profile? = profileDao.getById(id)?.toDomain()

    override suspend fun createProfile(name: String, avatarUrl: String?, pin: String?): Result<Profile> {
        if (profileDao.count() >= MAX_PROFILES) {
            return Result.failure(IllegalStateException("Max $MAX_PROFILES profiles"))
        }
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name required"))

        val entity = ProfileEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            avatarUrl = avatarUrl,
            pinHash = pin?.let { hashPin(it) },
            isActive = profileDao.count() == 0
        )
        profileDao.insert(entity)
        return Result.success(entity.toDomain())
    }

    override suspend fun updateProfile(id: String, name: String?, avatarUrl: String?): Result<Profile> {
        val existing = profileDao.getById(id) ?: return Result.failure(IllegalArgumentException("Not found"))
        val updated = existing.copy(name = name?.trim() ?: existing.name, avatarUrl = avatarUrl ?: existing.avatarUrl)
        profileDao.update(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun switchProfile(profileId: String): Result<Profile> {
        val target = profileDao.getById(profileId) ?: return Result.failure(IllegalArgumentException("Not found"))
        profileDao.switchActiveProfile(profileId)
        return Result.success(target.copy(isActive = true).toDomain())
    }

    override suspend fun deleteProfile(profileId: String): Result<Unit> {
        val profile = profileDao.getById(profileId) ?: return Result.failure(IllegalArgumentException("Not found"))
        if (profileDao.count() <= 1) return Result.failure(IllegalStateException("Cannot delete last profile"))
        val remaining = profileDao.deleteAndGetRemaining(profile)
        if (profile.isActive && remaining.isNotEmpty()) {
            profileDao.update(remaining.first().copy(isActive = true))
        }
        return Result.success(Unit)
    }

    override suspend fun setPin(profileId: String, pin: String): Result<Unit> {
        if (pin.length !in 4..6) return Result.failure(IllegalArgumentException("PIN 4-6 digits"))
        val profile = profileDao.getById(profileId) ?: return Result.failure(IllegalArgumentException("Not found"))
        profileDao.update(profile.copy(pinHash = hashPin(pin)))
        return Result.success(Unit)
    }

    override suspend fun removePin(profileId: String): Result<Unit> {
        val profile = profileDao.getById(profileId) ?: return Result.failure(IllegalArgumentException("Not found"))
        profileDao.update(profile.copy(pinHash = null))
        return Result.success(Unit)
    }

    override suspend fun verifyPin(profileId: String, pin: String): Boolean {
        return profileDao.getById(profileId)?.pinHash == hashPin(pin)
    }

    override suspend fun hasPin(profileId: String): Boolean = profileDao.getById(profileId)?.pinHash != null

    override suspend fun updatePreferences(profileId: String, preferencesJson: String): Result<Unit> {
        val profile = profileDao.getById(profileId) ?: return Result.failure(IllegalArgumentException("Not found"))
        profileDao.update(profile.copy(preferencesJson = preferencesJson))
        return Result.success(Unit)
    }

    override suspend fun getPreferences(profileId: String): String? = profileDao.getById(profileId)?.preferencesJson

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$PIN_SALT$pin".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun ProfileEntity.toDomain() = Profile(id, name, avatarUrl, pinHash != null, isActive, createdAt, preferencesJson)
}
