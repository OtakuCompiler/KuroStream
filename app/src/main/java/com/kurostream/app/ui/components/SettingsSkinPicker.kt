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

package com.kurostream.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.arctic.AFText
import com.kurostream.app.ui.arctic.AFTextDim
import com.kurostream.app.ui.theme.Skin

/**
 * Reusable picker for selecting the [Skin] background.
 * Shows the current skin name and a button to cycle through options.
 */
@Composable
fun SettingsSkinPicker(
    selected: Skin,
    onSkinSelected: (Skin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { contentDescription = "Background Skin selector" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Background Skin",
            style = MaterialTheme.typography.bodyLarge,
            color = AFText,
        )
        TextButton(
            onClick = {
                val skins = Skin.entries
                val currentIndex = skins.indexOf(selected)
                val nextIndex = (currentIndex + 1) % skins.size
                onSkinSelected(skins[nextIndex])
            },
            modifier = Modifier.semantics {
                contentDescription = "Current skin ${selected.label}. Tap to change."
            },
        ) {
            Text(text = selected.label, color = AFCyan)
            Text(
                text = " ▸",
                color = AFTextDim,
            )
        }
    }
}
