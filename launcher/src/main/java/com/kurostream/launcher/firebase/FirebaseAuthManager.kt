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

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAuth: FirebaseAuth = Firebase.auth

    companion object {
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }

    /**
     * Observe authentication state as Flow
     */
    val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val state = if (user != null) {
                AuthState.Authenticated(mapFirebaseUser(user))
            } else {
                AuthState.Unauthenticated
            }
            trySend(state)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Get current user if authenticated
     */
    fun getCurrentUser(): AuthUser? {
        return firebaseAuth.currentUser?.let { mapFirebaseUser(it) }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(authResult.user?.let { mapFirebaseUser(it) })
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication failed")
        }
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String? = null): AuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

            // Update display name if provided
            displayName?.let { name ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                authResult.user?.updateProfile(profileUpdates)?.await()
            }

            // Send email verification
            authResult.user?.sendEmailVerification()?.await()

            AuthResult.Success(authResult.user?.let { mapFirebaseUser(it) })
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            AuthResult.Success(authResult.user?.let { mapFirebaseUser(it) })
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }

    /**
     * Get Google Sign-In client for UI integration
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(account: GoogleSignInAccount?): AuthResult {
        return try {
            val idToken = account?.idToken
                ?: return AuthResult.Error("Google ID token is null")
            signInWithGoogle(idToken)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success(null)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send reset email")
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(displayName: String?, photoUrl: String?): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error("Not authenticated")

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()

            user.updateProfile(profileUpdates).await()
            AuthResult.Success(mapFirebaseUser(user))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Profile update failed")
        }
    }

    /**
     * Update email
     */
    suspend fun updateEmail(newEmail: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error("Not authenticated")
            user.verifyBeforeUpdateEmail(newEmail).await()
            AuthResult.Success(mapFirebaseUser(user))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Email update failed")
        }
    }

    /**
     * Update password
     */
    suspend fun updatePassword(newPassword: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error("Not authenticated")
            user.updatePassword(newPassword).await()
            AuthResult.Success(mapFirebaseUser(user))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Password update failed")
        }
    }

    /**
     * Re-authenticate user (required for sensitive operations)
     */
    suspend fun reauthenticate(password: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error("Not authenticated")
            val email = user.email ?: return AuthResult.Error("Email not available")

            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            AuthResult.Success(mapFirebaseUser(user))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Re-authentication failed")
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
                ?: return AuthResult.Error("Not authenticated")
            user.delete().await()
            AuthResult.Success(null)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Account deletion failed")
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        firebaseAuth.signOut()
        // Also sign out from Google
        getGoogleSignInClient().signOut()
    }

    private fun mapFirebaseUser(user: FirebaseUser): AuthUser {
        return AuthUser(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
            isEmailVerified = user.isEmailVerified,
            providers = user.providerData.map { it.providerId },
            creationTimestamp = user.metadata?.creationTimestamp ?: 0L,
            lastSignInTimestamp = user.metadata?.lastSignInTimestamp ?: 0L
        )
    }
}

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val providers: List<String>,
    val creationTimestamp: Long,
    val lastSignInTimestamp: Long
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    object Loading : AuthState()
}

sealed class AuthResult {
    data class Success(val user: AuthUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}
