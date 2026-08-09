// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kurostream.app.security.InputSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Native Material 3 install-from-URL dialog. Fetches the manifest, then
 * hands it off to the Stremio importer (or any other compatible importer).
 *
 * @param onDismiss called when the user cancels or the dialog is dismissed
 * @param onInstall callback receiving a (validated URL, trusted) pair.
 *                  The actual install is handled by the caller via the
 *                  importer so this composable stays UI-only.
 */
@Composable
fun InstallAddonFromUrlDialog(
    onDismiss: () -> Unit,
    onInstall: suspend (url: String) -> Result<Unit>,
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        containerColor = cs.surface,
        title = {
            Text(
                text = "Install add-on from URL",
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste a Stremio, Kodi, or CloudStream manifest URL.",
                    color = cs.onSurfaceVariant,
                    fontSize = 13.sp(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Manifest URL") },
                    placeholder = { Text("https://example.com/manifest.json") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceVariant,
                        unfocusedContainerColor = cs.surfaceVariant,
                    ),
                    enabled = !isWorking,
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error.orEmpty(),
                        color = cs.error,
                        fontSize = 12.sp(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tip: most addons live at /manifest.json or /configure.",
                    color = cs.onSurfaceVariant,
                    fontSize = 11.sp(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sanitized = InputSanitizer.sanitizeUrl(url.trim())
                    when {
                        sanitized == null -> {
                            error = "Invalid URL — must be http(s) and shorter than 2048 chars."
                        }
                        !sanitized.endsWith("/manifest.json") && !sanitized.contains("/configure") -> {
                            error = "URL must end with /manifest.json or contain /configure"
                        }
                        else -> {
                            isWorking = true
                            error = null
                            scope.launch {
                                try {
                                    val r = onInstall(sanitized)
                                    if (r.isSuccess) onDismiss()
                                    else error = r.exceptionOrNull()?.localizedMessage ?: "Install failed"
                                } catch (t: Throwable) {
                                    Timber.e(t, "install from URL failed")
                                    error = t.localizedMessage ?: "Install failed"
                                } finally {
                                    isWorking = false
                                }
                            }
                        }
                    }
                },
                enabled = !isWorking && url.isNotBlank(),
            ) {
                Text(if (isWorking) "Installing…" else "Install", color = cs.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        },
    )
}

private fun Int.sp(): androidx.compose.ui.unit.TextUnit =
    androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
