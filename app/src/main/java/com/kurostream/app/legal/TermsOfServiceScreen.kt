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
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(48.dp).verticalScroll(rememberScrollState())) {
        Text("Terms of Service", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
        Text(
            "By using KuroStream, you agree to use the app for lawful purposes only. " +
            "The app is provided as-is under GPL-3.0. " +
            "Content availability depends on third-party sources.",
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
