package com.kurostream.core.platform

// Cross-platform Provider interface
interface Provider<T> {
    fun get(): T
}

expect interface LoggerComponent {
    val loggerProvider: Provider<PlatformLogger>
}

expect interface PlatformComponent {
    val platformFactory: Provider<PlatformFactory>
}