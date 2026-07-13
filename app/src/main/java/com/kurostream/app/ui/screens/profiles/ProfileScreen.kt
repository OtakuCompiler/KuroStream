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

package com.kurostream.app.ui.screens.profiles

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.TextField
import androidx.tv.material3.TextFieldDefaults
import coil.compose.AsyncImage
import com.kurostream.domain.entity.Profile
import com.kurostream.domain.entity.ContentRating
import java.util.Calendar

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    profiles: List<Profile>,
    onProfileSelected: (Profile) -> Unit,
    onManageProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Who's watching?",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { onProfileSelected(profile) }
                )
            }
            
            item {
                AddProfileCard(onClick = onManageProfiles)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onManageProfiles,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Manage Profiles")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.focusRequester(FocusRequester()).onKeyEvent { event ->
            if (event.key == Key.Enter || event.key == Key.DirectionCenter) {
                onClick()
                true
            } else false
        }
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent
            ),
            border = CardDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    shape = CircleShape
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .onFocusChanged { isFocused = it.isFocused }
            ) {
                if (profile.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = profile.displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(
                                    red = (profile.displayName.hashCode() and 0xFF) / 255f,
                                    green = ((profile.displayName.hashCode() shr 8) and 0xFF) / 255f,
                                    blue = ((profile.displayName.hashCode() shr 16) and 0xFF) / 255f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                    }
                }
                
                if (profile.isKids) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👶",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.onKeyEvent { event ->
            if (event.key == Key.Enter || event.key == Key.DirectionCenter) {
                onClick()
                true
            } else false
        }
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = CardDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    shape = CircleShape
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .onFocusChanged { isFocused = it.isFocused },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Add Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PINEntryScreen(
    profile: Profile,
    onPINVerified: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter PIN for ${profile.displayName}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (index < pin.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < pin.length) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        NumericKeyboard(
            onNumberClick = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    if (pin.length == 4) {
                        if (pin == profile.pin) {
                            onPINVerified()
                        } else {
                            error = "Incorrect PIN"
                            pin = ""
                        }
                    }
                }
            },
            onBackspaceClick = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    error = null
                }
            },
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onBack,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Back")
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        ).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspaceClick()
                                "" -> {}
                                else -> onNumberClick(key)
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = if (key == "⌫") {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KidsModeScreen(
    profile: Profile,
    timeRemaining: Long,
    contentRating: ContentRating,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = timeRemaining / 3600
    val minutes = (timeRemaining % 3600) / 60
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFD700))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kids Mode",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF1A1A2E)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${hours}h ${minutes}m remaining",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1A1A2E)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Content Rating: ${contentRating.name}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFFFD700)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Only age-appropriate content is shown",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onExit,
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color(0xFF1A1A2E)
            )
        ) {
            Text("Exit Kids Mode")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

val Profile.isKids: Boolean
    get() = this.contentRatingLimit != null && this.contentRatingLimit.ordinal <= ContentRating.PG.ordinal

val Profile.contentRatingLimit: ContentRating?
    get() = null

val Profile.pin: String
    get() = ""

val Profile.dailyTimeLimitMinutes: Int
    get() = 60