package com.kurostream.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.js.JsName

sealed class Result<out T> {
    companion object {
        fun <T> loading(): Result<T> = Loading
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> error(exception: Throwable): Result<T> = Error(exception)

        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e)
        }

        fun <T> flowAsResult(flow: Flow<T>): Flow<Result<T>> = flow
            .map<Result<T>> { data -> @JsName("mapResult") Success(data) }
            .catch { emit(Error(it)) }
    }

    @JsName("Success")
    data class Success<out T>(val data: T) : Result<T>()

    @JsName("Error")
    data class Error(val exception: Throwable) : Result<Nothing>()

    @JsName("Loading")
    object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception

    inline fun <R> fold(onSuccess: (T) -> R, onError: (Throwable) -> R, onLoading: () -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
        is Loading -> onLoading()
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    @JsName("exceptionOrNull")
    fun exception(): Throwable? = (this as? Error)?.exception

    fun toThrowable(): T? = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> null
    }
}

sealed class Resource<out T> {
    @JsName("ResourceLoading")
    data class Loading<out T>(val data: T? = null) : Resource<T>()
    
    @JsName("ResourceSuccess")
    data class Success<out T>(val data: T) : Resource<T>()
    
    @JsName("ResourceError")
    data class Error<out T>(val exception: Throwable, val data: T? = null) : Resource<T>()

    companion object {
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)
        fun <T> success(data: T): Resource<T> = Success(data)
        fun <T> error(exception: Throwable, data: T? = null): Resource<T> = Error(exception, data)

        inline fun <T> runCatching(block: () -> T): Resource<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e)
        }
    }

    open val data: T? get() = when (this) {
        is Success -> data
        is Loading -> data
        is Error -> data
    }

    open val exception: Throwable? get() = when (this) {
        is Error -> exception
        else -> null
    }

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    inline fun <R> fold(onSuccess: (T) -> R, onError: (Throwable) -> R, onLoading: () -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
        is Loading -> onLoading()
    }

    inline fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Resource.success(transform(data))
        is Error -> Resource.error(exception, null)
        is Loading -> Resource.loading(null)
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable) -> Unit): Resource<T> {
        if (this is Error) action(exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) action()
        return this
    }
}