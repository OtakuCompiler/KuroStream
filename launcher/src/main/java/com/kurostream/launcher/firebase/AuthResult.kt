package com.kurostream.launcher.firebase

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Failure(val exception: Throwable) : AuthResult<Nothing>()
}
