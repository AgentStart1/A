/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Pill(
    text: String? = null,
    icon: ImageVector? = null,
    emoji: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val background =
        when {
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val textColor =
        when {
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
        Modifier.background(background, shape)
            .height(28.dp)
            .clip(shape)
            .clickable {
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = text, modifier = Modifier.size(15.dp), tint = textColor)

            emoji != null -> {
                Text(emoji)
            }
        }
        if (text != null) {
            Text(text, color = textColor, fontSize = 12.sp)
        }
    }
}
