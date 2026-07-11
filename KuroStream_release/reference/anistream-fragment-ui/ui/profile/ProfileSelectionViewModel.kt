package com.kurostream.legacyui.anistream.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileNavEvent>()
    val navigationEvent: SharedFlow<ProfileNavEvent> = _navigationEvent.asSharedFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.getAllProfiles()
                .collectLatest { _profiles.value = it }
        }
    }

    fun selectProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.setActiveProfile(profile.id)
                _navigationEvent.emit(ProfileNavEvent.NavigateToHome)
            } catch (e: Exception) {
                _navigationEvent.emit(ProfileNavEvent.ShowError("Failed to select profile: ${e.message}"))
            }
        }
    }

    fun createProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.createProfile(profile)
                // Auto-select if first profile
                if (_profiles.value.size <= 1) {
                    selectProfile(profile)
                }
            } catch (e: Exception) {
                _navigationEvent.emit(ProfileNavEvent.ShowError("Failed to create profile"))
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(profile)
            } catch (e: Exception) {
                _navigationEvent.emit(ProfileNavEvent.ShowError("Failed to update profile"))
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(profileId)
            } catch (e: Exception) {
                _navigationEvent.emit(ProfileNavEvent.ShowError("Failed to delete profile"))
            }
        }
    }

    fun onProfileFocused(profile: UserProfile) {
        // Could show preview/info
    }
}

sealed class ProfileNavEvent {
    object NavigateToHome : ProfileNavEvent()
    data class ShowError(val message: String) : ProfileNavEvent()
}
