package com.kurostream.app.ui.screens.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.kurostream.domain.extension.ConfigField
import com.kurostream.domain.extension.ConfigFieldType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExtensionConfigScreen(
    extensionId: String,
    viewModel: ExtensionConfigViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.getConfigState(extensionId).collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Text(
            text = "Configure ${uiState.extensionName}",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(24.dp))

        val configValues = remember { mutableStateOf(uiState.currentValues.toMutableMap()) }

        uiState.fields.forEach { field ->
            when (field.type) {
                ConfigFieldType.STRING -> {
                    var value by remember { mutableStateOf(configValues.value[field.key] ?: field.defaultValue ?: "") }
                    TextField(
                        value = value,
                        onValueChange = { value = it; configValues.value[field.key] = it },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ConfigFieldType.PASSWORD -> {
                    var value by remember { mutableStateOf(configValues.value[field.key] ?: field.defaultValue ?: "") }
                    TextField(
                        value = value,
                        onValueChange = { value = it; configValues.value[field.key] = it },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ConfigFieldType.NUMBER -> {
                    var value by remember { mutableStateOf(configValues.value[field.key] ?: field.defaultValue ?: "") }
                    TextField(
                        value = value,
                        onValueChange = { value = it; configValues.value[field.key] = it },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ConfigFieldType.BOOLEAN -> {
                    var checked by remember { mutableStateOf((configValues.value[field.key] ?: field.defaultValue ?: "false").toBoolean()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(field.label, color = Color.White, fontSize = 16.sp)
                        Switch(
                            checked = checked,
                            onCheckedChange = { checked = it; configValues.value[field.key] = it.toString() },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ConfigFieldType.SELECT -> {
                    Text(field.label, color = Color.White, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        field.options?.forEach { option ->
                            Button(onClick = { configValues.value[field.key] = option }) {
                                Text(option)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ConfigFieldType.URL -> {
                    var value by remember { mutableStateOf(configValues.value[field.key] ?: field.defaultValue ?: "") }
                    TextField(
                        value = value,
                        onValueChange = { value = it; configValues.value[field.key] = it },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            val helpText = field.helpText
            if (helpText != null) {
                Text(
                    text = helpText,
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                viewModel.saveConfig(extensionId, configValues.value)
                onBack()
            }) {
                Text("Save")
            }
            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

@Immutable
data class ExtensionConfigUiState(
    val extensionName: String = "",
    val fields: List<ConfigField> = emptyList(),
    val currentValues: Map<String, String> = emptyMap(),
)
