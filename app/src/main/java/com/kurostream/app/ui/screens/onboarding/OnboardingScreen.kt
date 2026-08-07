package com.kurostream.app.ui.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.kurostream.app.ui.arctic.AFAccentPrimary
import com.kurostream.app.ui.arctic.AFBg
import com.kurostream.app.ui.arctic.AFBgDeep
import com.kurostream.app.ui.arctic.AFText
import com.kurostream.app.ui.arctic.AFTextSec

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val options: List<String>,
    val multiSelect: Boolean = false,
    val onComplete: (List<String>) -> Unit,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit = onComplete,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(0) }
    var selectedOptions by remember { mutableStateOf(mutableSetOf<String>()) }
    var startAnimation by remember { mutableStateOf(false) }

    val steps = remember {
        listOf(
            OnboardingStep(
                title = "Welcome to KuroStream",
                subtitle = "Your unified cinema hub",
                options = listOf("Movies", "Series", "Anime"),
                multiSelect = true,
                onComplete = { selected ->
                    selectedOptions.addAll(selected)
                },
            ),
            OnboardingStep(
                title = "Select your region",
                subtitle = "For localized content",
                options = listOf("US", "IN", "UK", "JP", "BR", "DE", "FR", "CA", "AU", "OTHER"),
                multiSelect = false,
                onComplete = { selected ->
                    selectedOptions.addAll(selected)
                },
            ),
        )
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "onboardingAlpha",
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AFBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 64.dp)
                .alpha(alpha),
        ) {
            val step = steps[currentStep]
            androidx.compose.material3.Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium,
                color = AFText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = step.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = AFTextSec,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                step.options.forEach { option ->
                    OnboardingOption(
                        text = option,
                        selected = selectedOptions.contains(option),
                        multiSelect = step.multiSelect,
                        onSelect = {
                            if (step.multiSelect) {
                                if (selectedOptions.contains(option)) {
                                    selectedOptions.remove(option)
                                } else {
                                    selectedOptions.add(option)
                                }
                            } else {
                                selectedOptions.clear()
                                selectedOptions.add(option)
                            }
                        },
                        onConfirm = {
                            step.onComplete(selectedOptions.toList())
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                                selectedOptions.clear()
                            } else {
                                onComplete()
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.TextButton(onClick = onSkip) {
                androidx.compose.material3.Text("Skip", color = AFTextSec)
            }
        }
    }
}

@Composable
private fun OnboardingOption(
    text: String,
    selected: Boolean,
    multiSelect: Boolean,
    onSelect: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(150),
        label = "optionScale",
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 2f else if (selected) 1f else 0f,
        animationSpec = tween(150),
        label = "optionBorder",
    )

    Box(
        modifier = modifier
            .size(160.dp, 120.dp)
            .scale(scale)
            .border(
                width = borderWidth.dp,
                color = if (selected) AFAccentPrimary else Color.White,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                if (selected) AFAccentPrimary.copy(alpha = 0.2f) else AFBgDeep,
                RoundedCornerShape(12.dp),
            )
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    if (multiSelect) {
                        onSelect()
                    } else {
                        onConfirm()
                    }
                    true
                } else if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionRight) {
                    true
                } else {
                    false
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(
                text = text,
                color = if (selected) AFAccentPrimary else AFText,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(AFAccentPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text("✓", color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    }
}
