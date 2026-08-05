package com.kurostream.app.ui.screens.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import com.kurostream.domain.extension.UnifiedExtension

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    onNavigateToMarketplace: () -> Unit = {},
    onNavigateToInstalled: () -> Unit = {},
    viewModel: ExtensionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Text(
            text = "Extensions",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )
        Text(
            text = "Manage your streaming sources and addons",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                QuickActionCard(
                    icon = Icons.Default.Add,
                    title = "Browse Marketplace",
                    subtitle = "Discover new sources",
                    onClick = onNavigateToMarketplace,
                )
            }
            item {
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    title = "Installed Extensions",
                    subtitle = "${uiState.installedCount} active",
                    onClick = onNavigateToInstalled,
                )
            }
            item {
                QuickActionCard(
                    icon = Icons.Default.Refresh,
                    title = "Check for Updates",
                    subtitle = "Keep sources fresh",
                    onClick = { viewModel.checkForUpdates() },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Extension Health",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.extensions, key = { it.id }) { ext ->
                ExtensionHealthCard(
                    extension = ext,
                    onToggle = { viewModel.toggleExtension(ext.id) },
                    onUninstall = { viewModel.uninstallExtension(ext.id) },
                    onConfigure = { viewModel.configureExtension(ext.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(120.dp)
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExtensionHealthCard(
    extension: UnifiedExtension,
    onToggle: () -> Unit,
    onUninstall: () -> Unit,
    onConfigure: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val healthColor = when {
        extension.healthScore > 0.8f -> Color(0xFF4CAF50)
        extension.healthScore > 0.5f -> Color(0xFFFFA000)
        else -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f))
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = extension.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = extension.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (extension.isOfficial) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Official",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = "${extension.type.name} • ${extension.version} • Health: ${(extension.healthScore * 100).toInt()}%",
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(healthColor),
        )
        Spacer(modifier = Modifier.width(16.dp))
        if (extension.configSchema.isNotEmpty()) {
            Button(onClick = onConfigure) {
                Icon(Icons.Default.Settings, contentDescription = "Configure")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Button(onClick = onToggle) {
            Text(if (extension.isEnabled) "Disable" else "Enable")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onUninstall) {
            Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = Color.Red)
        }
    }
}
