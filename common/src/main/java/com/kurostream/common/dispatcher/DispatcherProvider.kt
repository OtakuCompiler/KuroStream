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

package com.kurostream.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    // Adaptive dispatchers based on CPU cores
    val adaptiveIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(
            (Runtime.getRuntime().availableProcessors() * 2).coerceAtMost(8)
        )
    }

    val cpuIntensive: CoroutineDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(
            Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
        )
    }

    val networkIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(
            Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        )
    }

    val databaseIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(2)
    }

    val fileIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(
            Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        )
    }

    val imageProcessing: CoroutineDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(2)
    }

    val backgroundSync: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(1)
    }
}
