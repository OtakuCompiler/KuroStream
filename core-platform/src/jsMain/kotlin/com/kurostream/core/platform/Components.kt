package com.kurostream.core.platform

import kotlinx.coroutines.Dispatchers

// Simple implementation for JS target
class DefaultLoggerComponent(
    private val provider: () -> PlatformLogger
) : LoggerComponent {
    override val loggerProvider: Provider<PlatformLogger> = object : Provider<PlatformLogger> {
        override fun get(): PlatformLogger = provider()
    }
}

class DefaultPlatformComponent(
    private val factoryProvider: () -> PlatformFactory
) : PlatformComponent {
    override val platformFactory: Provider<PlatformFactory> = object : Provider<PlatformFactory> {
        override fun get(): PlatformFactory = factoryProvider()
    }
}