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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(48.dp).verticalScroll(rememberScrollState())) {
        Text("Privacy Policy", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
        Text(
            "KuroStream collects minimal data necessary for app functionality. " +
            "Watch history is stored locally. Anonymous crash reports may be sent. " +
            "No personal data is sold to third parties. " +
            "You can request data deletion at any time.",
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
