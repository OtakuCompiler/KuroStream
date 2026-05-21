package com.kurostream.tv.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * PIN Lock Screen for profile or app access.
 * Supports D-pad navigation optimized for TV remotes.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinLockScreen(
    profileName: String? = null,
    isAppLock: Boolean = false,
    onPinVerified: () -> Unit,
    onCancel: (() -> Unit)? = null,
    viewModel: PinLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isShaking by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    
    // Handle PIN verification result
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onPinVerified()
        }
    }
    
    // Handle error animation
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            showError = true
            isShaking = true
            delay(500)
            isShaking = false
            enteredPin = ""
            delay(2000)
            showError = false
            viewModel.clearError()
        }
    }
    
    // Request focus on first button
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Shake animation
    val shakeOffset by animateFloatAsState(
        targetValue = if (isShaking) 1f else 0f,
        animationSpec = tween(100),
        label = "shake"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            // Title
            Text(
                text = when {
                    isAppLock -> "Enter PIN to Unlock"
                    profileName != null -> "Enter PIN for $profileName"
                    else -> "Enter PIN"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lockout message
            if (uiState.isLockedOut) {
                Text(
                    text = "Too many failed attempts.\nTry again in ${uiState.lockoutRemainingSeconds / 60}:${String.format("%02d", uiState.lockoutRemainingSeconds % 60)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                return@Column
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // PIN dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .scale(1f + shakeOffset * 0.05f)
                    .then(
                        if (isShaking) {
                            Modifier.padding(
                                start = (shakeOffset * 10).dp,
                                end = (shakeOffset * -10).dp
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                repeat(6) { index ->
                    PinDot(
                        filled = index < enteredPin.length,
                        isError = showError
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Text(
                    text = uiState.error ?: "Invalid PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Number pad
            NumberPad(
                focusRequester = focusRequester,
                onNumberClick = { number ->
                    if (enteredPin.length < 6) {
                        enteredPin += number
                        // Auto-submit when 4+ digits entered
                        if (enteredPin.length >= 4) {
                            viewModel.verifyPin(enteredPin, isAppLock)
                        }
                    }
                },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onClearClick = {
                    enteredPin = ""
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Cancel button
            if (onCancel != null) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
            
            // Remaining attempts
            if (uiState.remainingAttempts < 5) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${uiState.remainingAttempts} attempts remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PinDot(
    filled: Boolean,
    isError: Boolean
) {
    val backgroundColor = when {
        isError -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumberPad(
    focusRequester: FocusRequester,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        numbers.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { colIndex, key ->
                    val isFirstButton = rowIndex == 0 && colIndex == 0
                    
                    NumberPadButton(
                        key = key,
                        modifier = if (isFirstButton) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                        onClick = {
                            when (key) {
                                "⌫" -> onDeleteClick()
                                "C" -> onClearClick()
                                else -> onNumberClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumberPadButton(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(80.dp),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * PIN setup screen for creating or changing profile PIN.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinSetupScreen(
    isChangingPin: Boolean = false,
    onPinSet: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: PinLockViewModel = hiltViewModel()
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isChangingPin) 0 else 1) } // 0: current, 1: new, 2: confirm
    var error by remember { mutableStateOf<String?>(null) }
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            // Title based on step
            Text(
                text = when (step) {
                    0 -> "Enter Current PIN"
                    1 -> if (isChangingPin) "Enter New PIN" else "Create PIN"
                    2 -> "Confirm PIN"
                    else -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (step) {
                    0 -> "Enter your current PIN to continue"
                    1 -> "Choose a 4-6 digit PIN"
                    2 -> "Re-enter your PIN to confirm"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // PIN dots
            val currentEntry = when (step) {
                0 -> currentPin
                1 -> newPin
                2 -> confirmPin
                else -> ""
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(6) { index ->
                    PinDot(
                        filled = index < currentEntry.length,
                        isError = error != null
                    )
                }
            }
            
            // Error message
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Number pad
            NumberPad(
                focusRequester = focusRequester,
                onNumberClick = { number ->
                    error = null
                    when (step) {
                        0 -> {
                            if (currentPin.length < 6) {
                                currentPin += number
                                if (currentPin.length >= 4) {
                                    // Verify current PIN (would need to call ViewModel)
                                    step = 1
                                }
                            }
                        }
                        1 -> {
                            if (newPin.length < 6) {
                                newPin += number
                                if (newPin.length >= 4) {
                                    // Allow user to continue or wait for more digits
                                }
                            }
                        }
                        2 -> {
                            if (confirmPin.length < 6) {
                                confirmPin += number
                                if (confirmPin.length == newPin.length) {
                                    if (confirmPin == newPin) {
                                        onPinSet(newPin)
                                    } else {
                                        error = "PINs do not match"
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    error = null
                    when (step) {
                        0 -> currentPin = currentPin.dropLast(1)
                        1 -> newPin = newPin.dropLast(1)
                        2 -> confirmPin = confirmPin.dropLast(1)
                    }
                },
                onClearClick = {
                    error = null
                    when (step) {
                        0 -> currentPin = ""
                        1 -> newPin = ""
                        2 -> confirmPin = ""
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back button
                if (step > (if (isChangingPin) 0 else 1)) {
                    Button(
                        onClick = {
                            when (step) {
                                2 -> {
                                    confirmPin = ""
                                    step = 1
                                }
                                1 -> {
                                    newPin = ""
                                    step = 0
                                }
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Back")
                    }
                }
                
                // Continue button (for step 1)
                if (step == 1 && newPin.length >= 4) {
                    Button(
                        onClick = { step = 2 }
                    ) {
                        Text("Continue")
                    }
                }
                
                // Cancel button
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
