package com.kurostream.common.extension

import com.kurostream.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this.map<T, Result<T>> { Result.Success(it) }.onStart { emit(Result.Loading) }.catch { emit(Result.Error(it)) }
}

fun <T> Flow<Result<T>>.onSuccess(action: suspend (T) -> Unit): Flow<Result<T>> {
    return onEach { result ->
        if (result is Result.Success) action(result.data)
    }
}

fun <T> Flow<Result<T>>.onError(action: suspend (Throwable) -> Unit): Flow<Result<T>> {
    return onEach { result ->
        if (result is Result.Error) action(result.exception)
    }
}

fun <T> Flow<Result<T>>.onLoading(action: suspend () -> Unit): Flow<Result<T>> {
    return onEach { result ->
        if (result is Result.Loading) action()
    }
}
