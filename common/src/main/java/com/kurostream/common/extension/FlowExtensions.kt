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

package com.kurostream.common.extension

import com.kurostream.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this.map<T, Result<T>> { Result.Success(it) }.onStart { emit(Result.Loading) }.catch { emit(Result.Error(it)) }
}

@Suppress("EmptyElseBlock")
fun <T> Flow<Result<T>>.onSuccess(action: suspend (T) -> Unit): Flow<Result<T>> {
    return this.map { result ->
        if (result is Result.Success) action(result.data)
        result
    }
}

@Suppress("EmptyElseBlock")
fun <T> Flow<Result<T>>.onError(action: suspend (Throwable) -> Unit): Flow<Result<T>> {
    return this.map { result ->
        if (result is Result.Error) action(result.exception)
        result
    }
}

@Suppress("EmptyElseBlock")
fun <T> Flow<Result<T>>.onLoading(action: suspend () -> Unit): Flow<Result<T>> {
    return this.map { result ->
        if (result is Result.Loading) action()
        result
    }
}
