package com.kurostream.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.Default
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined

    val adaptiveIO: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val cpuIntensive: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val networkIO: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val databaseIO: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val fileIO: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val imageProcessing: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }

    val backgroundSync: CoroutineDispatcher by lazy {
        Dispatchers.Default
    }
}