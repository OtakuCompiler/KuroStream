package com.kurostream.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    val adaptiveIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(4)
    }

    val cpuIntensive: CoroutineDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(2)
    }

    val networkIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(2)
    }

    val databaseIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(2)
    }

    val fileIO: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(2)
    }

    val imageProcessing: CoroutineDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(2)
    }

    val backgroundSync: CoroutineDispatcher by lazy {
        Dispatchers.IO.limitedParallelism(1)
    }
}