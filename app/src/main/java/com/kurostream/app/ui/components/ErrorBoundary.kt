package com.kurostream.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import timber.log.Timber

class ComposeException(val error: Throwable) : Exception(error)

@Composable
fun ErrorBoundary(
    fallback: @Composable (Throwable) -> Unit = { DefaultErrorFallback(it) },
    content: @Composable () -> Unit
) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }

    val error = errorState.value
    if (error != null) {
        fallback(error)
        return
    }

    // Composable content is allowed here because we don't wrap it in try/catch
    // Error handling must be done at call sites or with LaunchedEffect/side-effect
    content()
}

// Caller-facing helper to use inside a ViewModel or side-effect to propagate errors
inline fun <T> safeComposeCall(
    fallback: (Throwable) -> T,
    block: () -> T
): T = try {
    block()
} catch (e: Throwable) {
    Timber.e(e, "Compose error boundary caught")
    fallback(e)
}

@Composable
private fun DefaultErrorFallback(error: Throwable) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.headlineMedium)
        Text(error.message ?: "Unknown error", style = MaterialTheme.typography.bodyMedium)
    }
}
