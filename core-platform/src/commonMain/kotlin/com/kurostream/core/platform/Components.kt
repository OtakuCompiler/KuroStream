package com.kurostream.core.platform

import javax.inject.Provider

interface LoggerComponent {
    val loggerProvider: Provider<PlatformLogger>
}

interface PlatformComponent {
    val platformFactory: Provider<PlatformFactory>
}