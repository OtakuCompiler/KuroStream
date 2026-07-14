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

package com.kurostream.domain.usecase.profile

import com.kurostream.core.common.result.Result
import com.kurostream.domain.model.Profile
import com.kurostream.domain.repository.ProfileRepository

class GetProfilesUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Result<List<Profile>> {
        return try {
            Result.success(repository.getProfiles())
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class GetProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String): Result<Profile> {
        return try {
            val profile = repository.getProfile(profileId)
            if (profile != null) Result.success(profile)
            else Result.error(RuntimeException("Profile not found"))
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class CreateProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile): Result<Unit> {
        return try {
            repository.saveProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class UpdateProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile): Result<Unit> {
        return try {
            repository.saveProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class DeleteProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String): Result<Unit> {
        return try {
            repository.deleteProfile(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class GetActiveProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Result<Profile> {
        return try {
            val profile = repository.getActiveProfile()
            if (profile != null) Result.success(profile)
            else Result.error(RuntimeException("No active profile"))
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class SetActiveProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String): Result<Unit> {
        return try {
            repository.setActiveProfile(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}