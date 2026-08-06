// This file is part of KuroStream.
//
// ArcticFuseBadges — AF3-aligned tag/badge row and individual badge composables.
// Two styles:
//   BOX   — rounded-corner filled box (AF3 default for resolution/audio/codec)
//   TEXT  — plain uppercase text (legacy AF2 style, kept for accessibility)
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MediaTag(
    val label: String,
    val color: Color,
    val icon: @Composable (() -> Unit)? = null,
)

enum class TagStyle { BOX, TEXT }

@Composable
fun AdditionalTagsRow(
    tags: List<MediaTag>,
    style: TagStyle = TagStyle.BOX,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        tags.forEach { tag ->
            when (style) {
                TagStyle.BOX -> TagBox(tag)
                TagStyle.TEXT -> TagText(tag)
            }
        }
    }
}

@Composable
private fun TagBox(tag: MediaTag) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AFRadius.sm))
            .background(tag.color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tag.icon?.invoke()
            Text(
                text       = tag.label,
                color      = Color.Black,
                fontSize   = AFTypo.meta,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
            )
        }
    }
}

@Composable
private fun TagText(tag: MediaTag) {
    Text(
        text       = tag.label.uppercase(),
        color      = tag.color,
        fontSize   = AFTypo.meta,
        fontWeight = FontWeight.SemiBold,
        maxLines   = 1,
        modifier   = Modifier.padding(end = 8.dp),
    )
}
