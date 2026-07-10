// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.players.advanced.captions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Caption overlay composable with low-latency text updates.
 * Supports language selection and real-time caption display.
 */
@Composable
fun CaptionOverlay(
    captionManager: LiveCaptionManager,
    modifier: Modifier = Modifier
) {
    val captions by captionManager.captionFlow.collectAsStateWithLifecycle(
        initialValue = null
    )

    val scope = rememberCoroutineScope()
    var displayText by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    // Auto-hide after inactivity
    LaunchedEffect(captions) {
        captions?.let { caption ->
            displayText = caption.text
            isVisible = true

            if (caption.isFinal) {
                delay(5000) // Hide after 5 seconds for final captions
                isVisible = false
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible && displayText.isNotBlank(),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        CaptionBubble(
            text = displayText,
            language = captions?.language ?: "en",
            confidence = captions?.confidence ?: 0f
        )
    }
}

@Composable
private fun CaptionBubble(
    text: String,
    language: String,
    confidence: Float
) {
    val confidenceColor = when {
        confidence > 0.9f -> Color(0xFF4CAF50)
        confidence > 0.7f -> Color(0xFFFFA000)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(confidenceColor, RoundedCornerShape(50))
                )
            }
        }
    }
}

/**
 * Language selector for caption settings.
 */
@Composable
fun CaptionLanguageSelector(
    captionManager: LiveCaptionManager,
    onLanguageSelected: (String) -> Unit
) {
    val languages = captionManager.getSupportedLanguages()
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage = captionManager.getCurrentLanguage()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = languages[currentLanguage] ?: currentLanguage,
            onValueChange = {},
            readOnly = true,
            label = { Text("Caption Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLanguageSelected(code)
                        captionManager.setLanguage(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
