/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.utils.safeFirstUnicode

@Composable
fun RoomIcon(roomInfo: RoomInfo?, width: Dp, setClickEvent: Boolean, updateDialog: (Boolean) -> Unit) {
    val height = width * 3 / 4
    val iconUrl = roomInfo?.icon?.url
    val radius = 8.dp
    val shape = RoundedCornerShape(radius)
    if (iconUrl != null) {
        CommonImage(
            iconUrl,
            contentDescription = "${roomInfo.name}'s icon",
            modifier =
            Modifier.size(width, height).clip(shape).let {
                if (setClickEvent) {
                    it.clickable { updateDialog(true) }.appiumSemantics(
                        description = "room details",
                        onClick = { updateDialog(true) },
                    )
                } else {
                    it
                }
            }.border(1.dp, Color.Gray, shape),
        )
    } else {
        Box(
            modifier =
            Modifier.size(width, height)
                .background(MaterialTheme.colorScheme.tertiaryContainer, shape)
                .clip(shape)
                .let {
                    if (setClickEvent) {
                        it.clickable { updateDialog(true) }.appiumSemantics(
                            description = "room details",
                            onClick = { updateDialog(true) },
                        )
                    } else {
                        it
                    }
                }.border(1.dp, Color.Gray, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(roomInfo?.name?.let { safeFirstUnicode(it) }.orEmpty())
        }
    }
}
