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

package com.kurostream.launcher.firebase

import com.kurostream.launcher.firebase.firestore.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val purchaseRepository: PurchaseRepository
) {
    private val _userState = MutableStateFlow<UserState>(UserState.NotLoggedIn)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    val currentUser: AuthUser?
        get() = authManager.getCurrentUser()

    init {
        // Observe auth state and update user state
        // In a real implementation, this would use a coroutine scope
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val result = authManager.signInWithEmail(email, password)
        if (result is AuthResult.Success) {
            result.user?.let { syncUserData(it.uid) }
        }
        return result
    }

    suspend fun signUp(email: String, password: String, displayName: String?): AuthResult {
        return authManager.signUpWithEmail(email, password, displayName)
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        val result = authManager.signInWithGoogle(idToken)
        if (result is AuthResult.Success) {
            result.user?.let { syncUserData(it.uid) }
        }
        return result
    }

    suspend fun signOut() {
        authManager.signOut()
        _userState.value = UserState.NotLoggedIn
    }

    suspend fun resetPassword(email: String): AuthResult {
        return authManager.sendPasswordReset(email)
    }

    suspend fun updateProfile(displayName: String?, photoUrl: String?): AuthResult {
        return authManager.updateProfile(displayName, photoUrl)
    }

    suspend fun deleteAccount(): AuthResult {
        val userId = currentUser?.uid ?: return AuthResult.Error("Not authenticated")

        // Delete purchase data first
        purchaseRepository.deleteAllPurchases(userId)

        return authManager.deleteAccount()
    }

    /**
     * Check if user has premium/addon access
     */
    suspend fun hasAddon(addonId: String): Boolean {
        val userId = currentUser?.uid ?: return false
        return purchaseRepository.hasPurchase(userId, addonId)
    }

    /**
     * Get all purchased addons for current user
     */
    suspend fun getPurchasedAddons(): List<String> {
        val userId = currentUser?.uid ?: return emptyList()
        return purchaseRepository.getPurchasedAddons(userId)
    }

    private suspend fun syncUserData(userId: String) {
        try {
            val purchases = purchaseRepository.getPurchasedAddons(userId)
            _userState.value = UserState.LoggedIn(
                userId = userId,
                purchasedAddons = purchases
            )
        } catch (e: Exception) {
            _userState.value = UserState.LoggedIn(
                userId = userId,
                purchasedAddons = emptyList()
            )
        }
    }
}

sealed class UserState {
    object NotLoggedIn : UserState()
    data class LoggedIn(
        val userId: String,
        val purchasedAddons: List<String>
    ) : UserState()
    data class Error(val message: String) : UserState()
}
