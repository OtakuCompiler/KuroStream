package com.kurostream.backup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kurostream.backup.domain.*
import com.kurostream.backup.ui.viewmodel.BackupViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.backupConfig.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(48.dp)
    ) {
        BackupHeader(onBackClick = onBackClick)
        Spacer(modifier = Modifier.height(24.dp))

        AuthSection(authState = authState, onLogin = { viewModel.authenticate() }, onLogout = { viewModel.logout() })

        if (authState.isAuthenticated) {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            BackupConfigSection(config = config, onConfigChange = { viewModel.updateBackupConfig(it) })
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            AutoBackupSection(config = config, onConfigChange = { viewModel.updateBackupConfig(it) })
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            BackupActionsSection(
                uiState = uiState,
                onCreateBackup = { viewModel.createBackup() },
                onRestore = { viewModel.showRestoreDialog() },
                onManage = { viewModel.showManageBackups() },
            )
            if (config.lastBackupTimestamp > 0) {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                LastBackupSection(config = config)
            }
        }
    }

    uiState.restoreDialogOpen?.let { metadata ->
        RestoreConfirmDialog(
            metadata = metadata,
            onDismiss = { viewModel.dismissRestoreDialog() },
            onConfirm = { password -> viewModel.restoreBackup(metadata, password) }
        )
    }

    uiState.manageBackupsOpen?.let {
        ManageBackupsDialog(
            backups = uiState.backups,
            onDismiss = { viewModel.dismissManageBackups() },
            onRestore = { viewModel.prepareRestore(it) },
            onDelete = { viewModel.deleteBackup(it) },
        )
    }
}

@Composable
private fun BackupHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("GitHub Backup & Sync", style = MaterialTheme.typography.displaySmall)
        androidx.tv.material3.IconButton(onClick = onBackClick) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}

@Composable
private fun AuthSection(authState: GitHubAuthState, onLogin: () -> Unit, onLogout: () -> Unit) {
    SettingsSection(title = "GitHub Account", icon = Icons.Default.AccountCircle) {
        if (authState.isAuthenticated) {
            AuthenticatedView(
                username = authState.username ?: "Unknown",
                avatarUrl = authState.avatarUrl,
                onLogout = onLogout
            )
        } else {
            UnauthenticatedView(onLogin = onLogin)
        }
    }
}

@Composable
private fun BackupConfigSection(config: BackupConfig, onConfigChange: (BackupConfig) -> Unit) {
    SettingsSection(title = "Backup Configuration", icon = Icons.Default.CloudUpload) {
        BackupTypeSelector(
            selectedType = config.backupType,
            onTypeChange = { onConfigChange(config.copy(backupType = it)) }
        )

        if (config.backupType == BackupType.GIST) {
            GistSettings(
                gistId = config.gistId,
                onGistIdChange = { onConfigChange(config.copy(gistId = it)) },
                onCreateGist = { },
            )
        } else {
            RepoSettings(
                repoOwner = config.repoOwner,
                repoName = config.repoName,
                repoBranch = config.repoBranch,
                repoPath = config.repoPath,
                onOwnerChange = { onConfigChange(config.copy(repoOwner = it)) },
                onNameChange = { onConfigChange(config.copy(repoName = it)) },
                onBranchChange = { onConfigChange(config.copy(repoBranch = it)) },
                onPathChange = { onConfigChange(config.copy(repoPath = it)) },
                onCreateRepo = { },
            )
        }

        SettingsToggle(
            title = "Encrypt Backups",
            subtitle = "Encrypt backup data with AES-256-GCM before uploading",
            checked = config.encryptBackups,
            onCheckedChange = { onConfigChange(config.copy(encryptBackups = it)) }
        )

        SettingsToggle(
            title = "Compress Backups",
            subtitle = "Compress backup data to reduce size",
            checked = config.compressionEnabled,
            onCheckedChange = { onConfigChange(config.copy(compressionEnabled = it)) }
        )
    }
}

@Composable
private fun AutoBackupSection(config: BackupConfig, onConfigChange: (BackupConfig) -> Unit) {
    SettingsSection(title = "Automatic Backup", icon = Icons.Default.Schedule) {
        SettingsToggle(
            title = "Enable Auto Backup",
            subtitle = "Automatically create backups on a schedule",
            checked = config.autoBackupEnabled,
            onCheckedChange = { onConfigChange(config.copy(autoBackupEnabled = it)) }
        )

        if (config.autoBackupEnabled) {
            SettingsItem("Backup Interval") {
                DropdownMenuButton(
                    text = "${config.autoBackupIntervalDays} days",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun BackupActionsSection(
    uiState: BackupUiState,
    onCreateBackup: () -> Unit,
    onRestore: () -> Unit,
    onManage: () -> Unit,
) {
    SettingsSection(title = "Actions", icon = Icons.Default.PlayArrow) {
        ListItem(
            headlineContent = { Text("Create Backup Now", style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { Text("Create a new backup with current settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Button(onClick = onCreateBackup, enabled = !uiState.isCreatingBackup) {
                    if (uiState.isCreatingBackup) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Create")
                }
            },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
        )

        ListItem(
            headlineContent = { Text("Restore from Backup", style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { Text("Browse and restore from existing backups", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Button(onClick = onRestore) { Text("Restore") }
            },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
        )

        if (uiState.backups.isNotEmpty()) {
            ListItem(
                headlineContent = { Text("Manage Backups", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("${uiState.backups.size} backup(s) available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = {
                    Button(onClick = onManage) { Text("Manage") }
                },
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
            )
        }
    }
}

@Composable
private fun LastBackupSection(config: BackupConfig) {
    SettingsSection(title = "Last Backup", icon = Icons.Default.History) {
        ListItem(
            headlineContent = { Text(formatTimestamp(config.lastBackupTimestamp), style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { Text("Size: ${formatSize(config.lastBackupSize)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
        )
    }
}

@Composable
fun AuthenticatedView(username: String, avatarUrl: String?, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl ?: "",
            contentDescription = "Avatar",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            placeholder = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp)) }
        )
        Column {
            Text("Signed in as $username", style = MaterialTheme.typography.bodyLarge)
            Text("GitHub", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onLogout) { Text("Sign Out") }
    }
}

@Composable
fun UnauthenticatedView(onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Connect your GitHub account to enable backup & sync", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onLogin) {
            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Sign in with GitHub")
        }
    }
}

@Composable
fun BackupTypeSelector(
    selectedType: BackupType,
    onTypeChange: (BackupType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackupTypeButton("Gist", BackupType.GIST, selectedType == BackupType.GIST) { onTypeChange(BackupType.GIST) }
        BackupTypeButton("Repository", BackupType.REPOSITORY, selectedType == BackupType.REPOSITORY) { onTypeChange(BackupType.REPOSITORY) }
    }
}

@Composable
fun BackupTypeButton(text: String, type: BackupType, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .wrapContentSize()
    )
}

@Composable
fun GistSettings(
    gistId: String?,
    onGistIdChange: (String?) -> Unit,
    onCreateGist: () -> Unit
) {
    SettingsItem("Gist ID") {
        OutlinedTextField(
            value = gistId ?: "",
            onValueChange = { onGistIdChange(if (it.isBlank()) null else it) },
            label = { Text("Enter existing Gist ID (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
    TextButton(onClick = onCreateGist) { Text("Create New Gist") }
}

@Composable
fun RepoSettings(
    repoOwner: String?,
    repoName: String?,
    repoBranch: String,
    repoPath: String,
    onOwnerChange: (String?) -> Unit,
    onNameChange: (String?) -> Unit,
    onBranchChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onCreateRepo: () -> Unit
) {
    SettingsItem("Repository Owner") {
        OutlinedTextField(value = repoOwner ?: "", onValueChange = { onOwnerChange(if (it.isBlank()) null else it) }, label = { Text("Owner") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    SettingsItem("Repository Name") {
        OutlinedTextField(value = repoName ?: "", onValueChange = { onNameChange(if (it.isBlank()) null else it) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    SettingsItem("Branch") {
        OutlinedTextField(value = repoBranch, onValueChange = onBranchChange, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    SettingsItem("Backup File Path") {
        OutlinedTextField(value = repoPath, onValueChange = onPathChange, label = { Text("Path") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    TextButton(onClick = onCreateRepo) { Text("Create Repository") }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

@Composable
fun SettingsItem(title: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
    )
}

@Composable
fun DropdownMenuButton(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.End) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RestoreConfirmDialog(
    metadata: BackupMetadata,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.8f).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Restore Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Restore from ${metadata.description} (${formatSize(metadata.size)})?", style = MaterialTheme.typography.bodyMedium)
                Text("This will overwrite your current data. Continue?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

                if (metadata.isEncrypted) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Decryption Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(Icons.Default.Visibility, contentDescription = if (showPassword) "Hide password" else "Show password")
                            }
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(if (password.isBlank()) null else password); onDismiss() }) { Text("Restore") }
                }
            }
        }
    }
}

@Composable
fun ManageBackupsDialog(
    backups: List<BackupMetadata>,
    onDismiss: () -> Unit,
    onRestore: (BackupMetadata) -> Unit,
    onDelete: (BackupMetadata) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f).padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Manage Backups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    backups.forEach { backup ->
                        BackupListItem(
                            backup = backup,
                            onRestore = { onRestore(backup) },
                            onDelete = { onDelete(backup) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupListItem(backup: BackupMetadata, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(backup.description, style = MaterialTheme.typography.bodyLarge)
                    Text(formatTimestamp(backup.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatSize(backup.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRestore) { Text("Restore") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
