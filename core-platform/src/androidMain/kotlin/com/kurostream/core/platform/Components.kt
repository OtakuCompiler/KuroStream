package com.kurostream.core.platform

import javax.inject.Provider as JavaxProvider

class AndroidProvider<T>(private val delegate: JavaxProvider<T>) : Provider<T> {
    override fun get(): T = delegate.get()
}

actual interface LoggerComponent {
    actual val loggerProvider: Provider<PlatformLogger>
}

actual interface PlatformComponent {
    actual val platformFactory: Provider<PlatformFactory>
}