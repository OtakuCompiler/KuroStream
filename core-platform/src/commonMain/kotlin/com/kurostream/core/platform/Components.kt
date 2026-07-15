package com.kurostream.core.platform

// Cross-platform Provider interface
interface Provider<T> {
    fun get(): T
}

interface LoggerComponent {
    val loggerProvider: Provider<PlatformLogger>
}

interface PlatformComponent {
    val platformFactory: Provider<PlatformFactory>
}