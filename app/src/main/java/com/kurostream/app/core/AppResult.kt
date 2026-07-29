package com.kurostream.app.core

/**
 * Lightweight, allocation-minimized Result type.
 * Use [Success] for happy path, [Error] for failures with structured detail.
 * Avoids wrapping every value in a sealed class — prefer direct return types
 * when only one failure mode exists to keep allocations near zero.
 */
sealed class AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val code: ErrorCode = ErrorCode.UNKNOWN,
    ) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? {
        @Suppress("UNCHECKED_CAST")
        return (this as? Success<T?>)?.data
    }

    fun getOrThrow(): T = (this as Success).data

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun error(message: String, throwable: Throwable? = null, code: ErrorCode = ErrorCode.UNKNOWN): Error =
            Error(message, throwable, code)
    }
}

enum class ErrorCode {
    NETWORK,
    TIMEOUT,
    NOT_FOUND,
    UNAUTHORIZED,
    DECODE,
    UNSUPPORTED,
    DISK_FULL,
    STORAGE_ERROR,
    DATABASE,
    PLAYBACK,
    UNKNOWN,
}
