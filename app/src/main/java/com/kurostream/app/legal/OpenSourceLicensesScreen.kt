package com.kurostream.app.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    val licenses = listOf(
        "AndroidX - Apache 2.0",
        "Jetpack Compose - Apache 2.0",
        "Hilt - Apache 2.0",
        "Retrofit - Apache 2.0",
        "OkHttp - Apache 2.0",
        "Coil - Apache 2.0",
        "Media3 - Apache 2.0",
        "Timber - Apache 2.0",
        "Kotlinx Coroutines - Apache 2.0",
        "jlibtorrent - GPL-3.0",
        "libVLC - LGPL-2.1+",
        "libmpv - LGPL-2.1+",
    )
    Column(modifier = Modifier.padding(48.dp).verticalScroll(rememberScrollState())) {
        Text("Open Source Licenses", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
        licenses.forEach {
            Text(it, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
