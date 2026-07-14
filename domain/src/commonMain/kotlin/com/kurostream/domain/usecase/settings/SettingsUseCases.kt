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

package com.kurostream.domain.usecase.settings

import com.kurostream.core.common.result.Result
import com.kurostream.domain.repository.SettingsRepository

class GetSettingUseCase< T >(
    private val repository: SettingsRepository,
    private val key: String
) {
    suspend operator fun invoke(): Result<T?> {
        return try {
            Result.success(repository.getSetting(key))
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class SetSettingUseCase< T >(
    private val repository: SettingsRepository,
    private val key: String
) {
    suspend operator fun invoke(value: T): Result<Unit> {
        return try {
            repository.setSetting(key, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class BoolSettingUseCase(
    private val repository: SettingsRepository,
    private val key: String,
    private val defaultValue: Boolean = false
) {
    suspend operator fun invoke(): Boolean {
        return try {
            repository.getSetting<Boolean>(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    suspend fun set(value: Boolean): Result<Unit> {
        return try {
            repository.setSetting(key, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class IntSettingUseCase(
    private val repository: SettingsRepository,
    private val key: String,
    private val defaultValue: Int = 0
) {
    suspend operator fun invoke(): Int {
        return try {
            repository.getSetting<Int>(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    suspend fun set(value: Int): Result<Unit> {
        return try {
            repository.setSetting(key, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}

class StringSettingUseCase(
    private val repository: SettingsRepository,
    private val key: String,
    private val defaultValue: String = ""
) {
    suspend operator fun invoke(): String {
        return try {
            repository.getSetting<String>(key) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    suspend fun set(value: String): Result<Unit> {
        return try {
            repository.setSetting(key, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}