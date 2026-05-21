package com.kurostream.tv.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.tv.data.local.profile.InvalidPinException
import com.kurostream.tv.data.local.profile.LockoutException
import com.kurostream.tv.data.local.profile.ProfileRepository
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for PIN Lock Screen.
 */
data class PinLockUiState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val remainingAttempts: Int = 5,
    val error: String? = null
)

/**
 * ViewModel for PIN Lock Screen.
 */
@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PinLockUiState())
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()
    
    private var maxAttempts = 5
    private var failedAttempts = 0
    
    init {
        observeLockoutStatus()
    }
    
    private fun observeLockoutStatus() {
        viewModelScope.launch(ioDispatcher) {
            profileRepository.isLockedOut.collect { isLockedOut ->
                if (isLockedOut) {
                    val remainingSeconds = profileRepository.lockoutRemainingSeconds.first()
                    _uiState.update { 
                        it.copy(
                            isLockedOut = true,
                            lockoutRemainingSeconds = remainingSeconds
                        )
                    }
                    // Start countdown
                    startLockoutCountdown()
                } else {
                    _uiState.update { it.copy(isLockedOut = false) }
                }
            }
        }
    }
    
    private fun startLockoutCountdown() {
        viewModelScope.launch(ioDispatcher) {
            while (_uiState.value.isLockedOut && _uiState.value.lockoutRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                val remaining = profileRepository.lockoutRemainingSeconds.first()
                _uiState.update { 
                    it.copy(
                        lockoutRemainingSeconds = remaining,
                        isLockedOut = remaining > 0
                    )
                }
            }
        }
    }
    
    /**
     * Verify PIN for app lock or profile.
     */
    fun verifyPin(pin: String, isAppLock: Boolean, profileId: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = if (isAppLock) {
                profileRepository.verifyAppPin(pin)
            } else if (profileId != null) {
                profileRepository.switchProfile(profileId, pin).map { true }
            } else {
                Result.failure(Exception("Invalid verification request"))
            }
            
            result.fold(
                onSuccess = { isValid ->
                    if (isValid) {
                        failedAttempts = 0
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isVerified = true,
                                remainingAttempts = maxAttempts
                            )
                        }
                    } else {
                        handleFailedAttempt()
                    }
                },
                onFailure = { error ->
                    when (error) {
                        is InvalidPinException -> handleFailedAttempt()
                        is LockoutException -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    isLockedOut = true,
                                    lockoutRemainingSeconds = (error.remainingMs / 1000).toInt(),
                                    error = "Too many failed attempts"
                                )
                            }
                        }
                        else -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Verification failed"
                                )
                            }
                        }
                    }
                }
            )
        }
    }
    
    private fun handleFailedAttempt() {
        failedAttempts++
        val remaining = maxAttempts - failedAttempts
        
        _uiState.update { 
            it.copy(
                isLoading = false,
                error = "Invalid PIN",
                remainingAttempts = remaining.coerceAtLeast(0)
            )
        }
    }
    
    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Reset verification state.
     */
    fun reset() {
        _uiState.update { PinLockUiState() }
        failedAttempts = 0
    }
}
