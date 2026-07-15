package com.kurostream.core.platform

import javax.inject.Provider

actual class DefaultLoggerComponent(
    private val provider: Provider<PlatformLogger>
) : LoggerComponent {
    actual override val loggerProvider: Provider<PlatformLogger> = provider
}

actual class DefaultPlatformComponent(
    private val factoryProvider: Provider<PlatformFactory>
) : PlatformComponent {
    actual override val platformFactory: Provider<PlatformFactory> = factoryProvider
}