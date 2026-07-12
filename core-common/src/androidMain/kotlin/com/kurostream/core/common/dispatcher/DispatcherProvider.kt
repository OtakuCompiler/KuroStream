package com.kurostream.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

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