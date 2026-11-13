/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun UserIcon(setClickEvent: Boolean, avatarUrl: String?, size: Dp = 40.dp, onClick: () -> Unit) {
    val shape = CircleShape
    if (avatarUrl != null) {
        CommonImage(
            avatarUrl,
            contentDescription = "avatar",
            modifier =
            Modifier.size(size).clip(shape).let {
                if (setClickEvent) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            }.border(1.dp, MaterialTheme.colorScheme.primary, shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            Icons.Default.AccountCircle,
            contentDescription = "avatar",
            modifier =
            Modifier.size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                .let {
                    if (setClickEvent) {
                        it.clickable(onClick = onClick)
                    } else {
                        it
                    }
                }
                .border(1.dp, MaterialTheme.colorScheme.primary, shape)
                .padding(size / 5),
        )
    }
}
