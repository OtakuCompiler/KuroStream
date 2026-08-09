// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.theme.Af3Theme

private data class OnboardingPage(val icon: String, val title: String, val subtitle: String)

private val pages = listOf(
    OnboardingPage(
        icon = "🌌",
        title = "Welcome to KuroStream",
        subtitle = "A privacy-first streaming hub for movies, series and anime.",
    ),
    OnboardingPage(
        icon = "🎬",
        title = "Your library, everywhere",
        subtitle = "Sync your watch history and favorites across all your devices.",
    ),
    OnboardingPage(
        icon = "🛡️",
        title = "Privacy first",
        subtitle = "No telemetry, no tracking, no analytics. Ever.",
    ),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    val palette = Af3Theme.palette
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(current.icon, fontSize = 96.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = current.title,
            color = palette.text,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = current.subtitle,
            color = palette.textSec,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Af3PillButton(
                label = if (page < pages.lastIndex) "Skip" else "Get Started",
                primary = false,
                onClick = onSkip,
            )
            Af3PillButton(
                label = if (page < pages.lastIndex) "Next" else "Done",
                primary = true,
                onClick = {
                    if (page < pages.lastIndex) page++ else onComplete()
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        // Page dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.forEachIndexed { idx, _ ->
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .height(6.dp)
                        .width(if (idx == page) 24.dp else 8.dp)
                        .background(
                            if (idx == page) palette.accent else palette.border,
                            androidx.compose.foundation.shape.RoundedCornerShape(50),
                        ),
                )
            }
        }
    }
}
