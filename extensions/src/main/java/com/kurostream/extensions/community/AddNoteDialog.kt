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

package com.kurostream.extensions.community

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddNoteDialog(currentTimestamp: Long, onDismiss: () -> Unit, onSubmit: (String, NoteType) -> Unit) {
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteType.COMMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note at ${formatTimestamp(currentTimestamp)}") },
        text = {
            Column {
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Your note") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteType.values().forEach { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(type.name.replace("_", " ")) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(content, selectedType) }, enabled = content.isNotBlank()) { Text("Post") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTimestamp(ms: Long): String {
    val seconds = ms / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
