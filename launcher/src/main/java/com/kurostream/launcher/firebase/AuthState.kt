package com.kurostream.launcher.firebase

sealed class AuthState {
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
}
