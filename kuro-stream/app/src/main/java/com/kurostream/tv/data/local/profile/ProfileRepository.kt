package com.kurostream.tv.data.local.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profiles")

/**
 * Repository for managing user profiles with PIN lock functionality.
 * Supports multiple profiles per device for family sharing.
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.profileDataStore
    
    companion object {
        private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        private val KEY_PROFILES_JSON = stringPreferencesKey("profiles_json")
        private val KEY_APP_PIN_ENABLED = booleanPreferencesKey("app_pin_enabled")
        private val KEY_APP_PIN_HASH = stringPreferencesKey("app_pin_hash")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        private val KEY_LOCKOUT_UNTIL = stringPreferencesKey("lockout_until")
        
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    /**
     * Get the currently active profile.
     */
    val activeProfile: Flow<Profile?> = dataStore.data.map { prefs ->
        val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: return@map null
        val profilesJson = prefs[KEY_PROFILES_JSON] ?: return@map null
        parseProfiles(profilesJson).find { it.id == activeId }
    }
    
    /**
     * Get all profiles.
     */
    val allProfiles: Flow<List<Profile>> = dataStore.data.map { prefs ->
        val profilesJson = prefs[KEY_PROFILES_JSON] ?: return@map emptyList()
        parseProfiles(profilesJson)
    }
    
    /**
     * Check if app-level PIN is enabled.
     */
    val isAppPinEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_APP_PIN_ENABLED] ?: false
    }
    
    /**
     * Check if currently locked out due to failed attempts.
     */
    val isLockedOut: Flow<Boolean> = dataStore.data.map { prefs ->
        val lockoutUntil = prefs[KEY_LOCKOUT_UNTIL]?.toLongOrNull() ?: 0L
        System.currentTimeMillis() < lockoutUntil
    }
    
    /**
     * Get remaining lockout time in seconds.
     */
    val lockoutRemainingSeconds: Flow<Int> = dataStore.data.map { prefs ->
        val lockoutUntil = prefs[KEY_LOCKOUT_UNTIL]?.toLongOrNull() ?: 0L
        val remaining = lockoutUntil - System.currentTimeMillis()
        if (remaining > 0) (remaining / 1000).toInt() else 0
    }
    
    /**
     * Create a new profile.
     */
    suspend fun createProfile(
        name: String,
        avatarIndex: Int = 0,
        isKidsProfile: Boolean = false,
        pin: String? = null
    ): Result<Profile> {
        return try {
            val prefs = dataStore.data.first()
            val existingProfiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) } ?: emptyList()
            
            // Check max profiles limit
            if (existingProfiles.size >= 5) {
                return Result.failure(Exception("Maximum 5 profiles allowed"))
            }
            
            // Check for duplicate names
            if (existingProfiles.any { it.name.equals(name, ignoreCase = true) }) {
                return Result.failure(Exception("Profile name already exists"))
            }
            
            val profile = Profile(
                id = UUID.randomUUID().toString(),
                name = name,
                avatarIndex = avatarIndex,
                isKidsProfile = isKidsProfile,
                pinHash = pin?.let { hashPin(it) },
                createdAt = System.currentTimeMillis()
            )
            
            val updatedProfiles = existingProfiles + profile
            
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_PROFILES_JSON] = serializeProfiles(updatedProfiles)
                // Set as active if it's the first profile
                if (existingProfiles.isEmpty()) {
                    mutablePrefs[KEY_ACTIVE_PROFILE_ID] = profile.id
                }
            }
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing profile.
     */
    suspend fun updateProfile(
        profileId: String,
        name: String? = null,
        avatarIndex: Int? = null,
        isKidsProfile: Boolean? = null
    ): Result<Profile> {
        return try {
            val prefs = dataStore.data.first()
            val profiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) }?.toMutableList()
                ?: return Result.failure(Exception("No profiles found"))
            
            val index = profiles.indexOfFirst { it.id == profileId }
            if (index == -1) {
                return Result.failure(Exception("Profile not found"))
            }
            
            val existing = profiles[index]
            val updated = existing.copy(
                name = name ?: existing.name,
                avatarIndex = avatarIndex ?: existing.avatarIndex,
                isKidsProfile = isKidsProfile ?: existing.isKidsProfile
            )
            
            profiles[index] = updated
            
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_PROFILES_JSON] = serializeProfiles(profiles)
            }
            
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a profile.
     */
    suspend fun deleteProfile(profileId: String): Result<Unit> {
        return try {
            val prefs = dataStore.data.first()
            val profiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) }?.toMutableList()
                ?: return Result.failure(Exception("No profiles found"))
            
            // Don't allow deleting the last profile
            if (profiles.size <= 1) {
                return Result.failure(Exception("Cannot delete the last profile"))
            }
            
            profiles.removeAll { it.id == profileId }
            
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_PROFILES_JSON] = serializeProfiles(profiles)
                // If active profile was deleted, switch to first available
                if (mutablePrefs[KEY_ACTIVE_PROFILE_ID] == profileId) {
                    mutablePrefs[KEY_ACTIVE_PROFILE_ID] = profiles.first().id
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Switch to a different profile.
     * Requires PIN if the target profile has one set.
     */
    suspend fun switchProfile(profileId: String, pin: String? = null): Result<Profile> {
        return try {
            val prefs = dataStore.data.first()
            val profiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) }
                ?: return Result.failure(Exception("No profiles found"))
            
            val profile = profiles.find { it.id == profileId }
                ?: return Result.failure(Exception("Profile not found"))
            
            // Check PIN if required
            if (profile.pinHash != null) {
                if (pin == null) {
                    return Result.failure(PinRequiredException())
                }
                if (!verifyPin(pin, profile.pinHash)) {
                    recordFailedAttempt()
                    return Result.failure(InvalidPinException())
                }
            }
            
            // Reset failed attempts on successful switch
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_ACTIVE_PROFILE_ID] = profileId
                mutablePrefs[KEY_FAILED_ATTEMPTS] = 0
            }
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set or update PIN for a profile.
     */
    suspend fun setProfilePin(profileId: String, currentPin: String?, newPin: String?): Result<Unit> {
        return try {
            val prefs = dataStore.data.first()
            val profiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) }?.toMutableList()
                ?: return Result.failure(Exception("No profiles found"))
            
            val index = profiles.indexOfFirst { it.id == profileId }
            if (index == -1) {
                return Result.failure(Exception("Profile not found"))
            }
            
            val profile = profiles[index]
            
            // Verify current PIN if one exists
            if (profile.pinHash != null) {
                if (currentPin == null || !verifyPin(currentPin, profile.pinHash)) {
                    return Result.failure(InvalidPinException())
                }
            }
            
            // Validate new PIN format (4-6 digits)
            if (newPin != null && !isValidPin(newPin)) {
                return Result.failure(Exception("PIN must be 4-6 digits"))
            }
            
            profiles[index] = profile.copy(pinHash = newPin?.let { hashPin(it) })
            
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_PROFILES_JSON] = serializeProfiles(profiles)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Enable or disable app-level PIN lock.
     */
    suspend fun setAppPinLock(enabled: Boolean, pin: String?): Result<Unit> {
        return try {
            if (enabled && pin == null) {
                return Result.failure(Exception("PIN required to enable lock"))
            }
            
            if (pin != null && !isValidPin(pin)) {
                return Result.failure(Exception("PIN must be 4-6 digits"))
            }
            
            dataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_APP_PIN_ENABLED] = enabled
                if (enabled && pin != null) {
                    mutablePrefs[KEY_APP_PIN_HASH] = hashPin(pin)
                } else if (!enabled) {
                    mutablePrefs.remove(KEY_APP_PIN_HASH)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verify app-level PIN.
     */
    suspend fun verifyAppPin(pin: String): Result<Boolean> {
        return try {
            val prefs = dataStore.data.first()
            
            // Check if locked out
            val lockoutUntil = prefs[KEY_LOCKOUT_UNTIL]?.toLongOrNull() ?: 0L
            if (System.currentTimeMillis() < lockoutUntil) {
                return Result.failure(LockoutException(lockoutUntil - System.currentTimeMillis()))
            }
            
            val pinHash = prefs[KEY_APP_PIN_HASH]
                ?: return Result.failure(Exception("App PIN not set"))
            
            val isValid = verifyPin(pin, pinHash)
            
            if (isValid) {
                // Reset failed attempts
                dataStore.edit { mutablePrefs ->
                    mutablePrefs[KEY_FAILED_ATTEMPTS] = 0
                }
            } else {
                recordFailedAttempt()
            }
            
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Record a failed PIN attempt and trigger lockout if necessary.
     */
    private suspend fun recordFailedAttempt() {
        val prefs = dataStore.data.first()
        val attempts = (prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1
        
        dataStore.edit { mutablePrefs ->
            mutablePrefs[KEY_FAILED_ATTEMPTS] = attempts
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                mutablePrefs[KEY_LOCKOUT_UNTIL] = (System.currentTimeMillis() + LOCKOUT_DURATION_MS).toString()
            }
        }
    }
    
    /**
     * Create default profile if none exists.
     */
    suspend fun ensureDefaultProfile() {
        val prefs = dataStore.data.first()
        val profiles = prefs[KEY_PROFILES_JSON]?.let { parseProfiles(it) }
        
        if (profiles.isNullOrEmpty()) {
            createProfile(name = "Main Profile", avatarIndex = 0)
        }
    }
    
    // Utility functions
    
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun verifyPin(pin: String, hash: String): Boolean {
        return hashPin(pin) == hash
    }
    
    private fun isValidPin(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }
    
    private fun serializeProfiles(profiles: List<Profile>): String {
        return profiles.joinToString("|||") { profile ->
            listOf(
                profile.id,
                profile.name,
                profile.avatarIndex.toString(),
                profile.isKidsProfile.toString(),
                profile.pinHash ?: "",
                profile.createdAt.toString()
            ).joinToString("|||")
        }
    }
    
    private fun parseProfiles(json: String): List<Profile> {
        if (json.isEmpty()) return emptyList()
        
        return try {
            val parts = json.split("|||")
            val profiles = mutableListOf<Profile>()
            
            var i = 0
            while (i + 5 < parts.size) {
                profiles.add(
                    Profile(
                        id = parts[i],
                        name = parts[i + 1],
                        avatarIndex = parts[i + 2].toIntOrNull() ?: 0,
                        isKidsProfile = parts[i + 3].toBooleanStrictOrNull() ?: false,
                        pinHash = parts[i + 4].takeIf { it.isNotEmpty() },
                        createdAt = parts[i + 5].toLongOrNull() ?: 0L
                    )
                )
                i += 6
            }
            
            profiles
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * User profile data class.
 */
data class Profile(
    val id: String,
    val name: String,
    val avatarIndex: Int,
    val isKidsProfile: Boolean,
    val pinHash: String?,
    val createdAt: Long
) {
    val hasPinLock: Boolean
        get() = pinHash != null
}

/**
 * Available profile avatars.
 */
object ProfileAvatars {
    val avatars = listOf(
        "avatar_cat",
        "avatar_dog",
        "avatar_fox",
        "avatar_panda",
        "avatar_rabbit",
        "avatar_bear",
        "avatar_owl",
        "avatar_penguin"
    )
    
    fun getAvatarResource(index: Int): String {
        return avatars.getOrElse(index) { avatars[0] }
    }
}

/**
 * Custom exceptions for profile management.
 */
class PinRequiredException : Exception("PIN is required for this profile")
class InvalidPinException : Exception("Invalid PIN")
class LockoutException(val remainingMs: Long) : Exception("Too many failed attempts. Try again later.")
