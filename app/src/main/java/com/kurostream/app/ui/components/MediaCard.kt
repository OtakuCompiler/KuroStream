package com.kurostream.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceHighlight
import com.kurostream.app.ui.theme.focusedCardBorder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    GradientFocusBorder(
        isFocused = isFocused,
        cornerRadius = 12.dp,
        modifier = modifier.width(180.dp),
    ) {
        FocusScaleAnimation(
            isFocused = isFocused,
        ) {
            Card(
                onClick = onClick,
                modifier = Modifier.width(180.dp),
                colors = CardDefaults.colors(
                    containerColor = TvSurfaceHighlight,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = CardDefaults.border(
                    focused = CardDefaults.border(
                        focused = RoundedCornerShape(12.dp),
                        focusedBorder = null,
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1.0f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                ) {
                    Column {
                        AsyncImage(
                            model = item.posterImage,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.year} · ${item.format}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvOnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
