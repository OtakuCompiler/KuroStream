package com.kurostream.tizen

import com.kurostream.core.platform.PlatformFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("KuroStream Tizen App starting...")

    val platformFactory = PlatformFactory.getInstance()
    val appScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Default)

    println("KuroStream Tizen App initialized!")

    appScope.launch {
        println("Running on Tizen...")
    }

    while (true) {
        delay(60_000L)
    }
}
