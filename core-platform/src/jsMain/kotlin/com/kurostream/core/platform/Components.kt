package com.kurostream.core.platform

class JsProvider<T>(private val delegate: () -> T) : Provider<T> {
    override fun get(): T = delegate()
}

actual interface LoggerComponent {
    actual val loggerProvider: Provider<PlatformLogger>
}

actual interface PlatformComponent {
    actual val platformFactory: Provider<PlatformFactory>
}