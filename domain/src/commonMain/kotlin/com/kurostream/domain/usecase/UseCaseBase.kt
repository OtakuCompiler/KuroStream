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

package com.kurostream.domain.usecase

import com.kurostream.core.common.result.Result
import com.kurostream.core.common.result.Resource
import kotlinx.coroutines.flow.Flow

interface BaseUseCase<in Params, out Output> {
    suspend operator fun invoke(params: Params): Output
}

interface BaseUseCaseNoParams<out Output> {
    suspend operator fun invoke(): Output
}

interface BaseFlowUseCase<in Params, out Output> {
    operator fun invoke(params: Params): Flow<Output>
}

interface BaseFlowUseCaseNoParams<out Output> {
    operator fun invoke(): Flow<Output>
}

interface BaseResultUseCase<in Params, out Output> {
    suspend operator fun invoke(params: Params): Result<Output>
}

interface BaseResultUseCaseNoParams<out Output> {
    suspend operator fun invoke(): Result<Output>
}

interface BaseResourceUseCase<in Params, out Output> {
    suspend operator fun invoke(params: Params): Resource<Output>
}

interface BaseResourceUseCaseNoParams<out Output> {
    suspend operator fun invoke(): Resource<Output>
}

class NoParams private constructor() {
    companion object {
        val INSTANCE = NoParams()
    }
}