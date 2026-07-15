package com.kurostream.core.platform

// Simple implementation for JS target
class DefaultLoggerComponent(
    private val provider: () -> PlatformLogger
) : LoggerComponent {
    actual override val loggerProvider: Provider<PlatformLogger> = object : Provider<PlatformLogger> {
        override fun get(): PlatformLogger = provider()
    }
}

actual class DefaultPlatformComponent(
    private val factoryProvider: () -> PlatformFactory
) : PlatformComponent {
    actual override val platformFactory: Provider<PlatformFactory> = object : Provider<PlatformFactory> {
        override fun get(): PlatformFactory = factoryProvider()
    }
}