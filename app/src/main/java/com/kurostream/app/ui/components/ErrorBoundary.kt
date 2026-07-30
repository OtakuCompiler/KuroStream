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
    var error by remember { mutableStateOf<Throwable?>(null) }

    if (error != null) {
        fallback(error!!)
    } else {
        try {
            content()
        } catch (e: Throwable) {
            Timber.e(e, "Compose error boundary caught")
            error = e
        }
    }
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
