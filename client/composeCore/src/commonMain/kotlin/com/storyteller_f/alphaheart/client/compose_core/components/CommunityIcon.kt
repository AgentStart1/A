/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.utils.safeFirstUnicode

@Composable
fun CommunityIcon(communityInfo: CommunityInfo?, iconSize: Dp, setClickEvent: Boolean, onClickIcon: (Boolean) -> Unit) {
    val model = communityInfo?.icon?.url
    val firstCharacter = communityInfo?.name?.let(::safeFirstUnicode)
    val openDialog =
        if (setClickEvent) {
            { onClickIcon(true) }
        } else {
            null
        }
    val shape = RoundedCornerShape(8.dp)
    if (model != null) {
        CommonImage(
            model,
            "icon",
            Modifier.size(iconSize)
                .appiumSemantics(text = firstCharacter, onClick = openDialog)
                .clip(shape)
                .clickable(setClickEvent) { onClickIcon(true) }
                .border(1.dp, Color.Gray, shape),
        )
    } else {
        Box(
            modifier =
            Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, shape)
                .clip(shape)
                .size(iconSize)
                .appiumSemantics(text = firstCharacter, onClick = openDialog)
                .clickable(setClickEvent) { onClickIcon(true) }
                .border(1.dp, Color.Gray, shape),
            contentAlignment = Alignment.Center,
        ) {
            CharSequenceText(firstCharacter.orEmpty())
        }
    }
}

@Composable
fun CommunityPoster(communityInfo: CommunityInfo?) {
    val url = communityInfo?.poster?.url
    val shape = RoundedCornerShape(14.dp)
    if (url != null) {
        CommonImage(url, contentDescription = "community poster", modifier = Modifier.fillMaxSize().clip(shape))
    } else {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape).fillMaxSize())
    }
}
