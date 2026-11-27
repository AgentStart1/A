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
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val compact = iconSize <= 22.dp
    val label = if (compact) communityInfo?.name else firstCharacter
    val shape = RoundedCornerShape(if (compact) 5.dp else 10.dp)
    if (model != null) {
        CommonImage(
            model,
            communityInfo.name,
            Modifier.size(iconSize)
                .appiumSemantics(description = label, text = label, onClick = openDialog)
                .clip(shape)
                .clickable(setClickEvent) { onClickIcon(true) }
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        )
    } else {
        val outline = if (compact) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
        Box(
            modifier =
            Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, shape)
                .clip(shape)
                .size(iconSize)
                .semantics { communityInfo?.name?.let { contentDescription = it } }
                .appiumSemantics(description = label, text = label, onClick = openDialog)
                .clickable(setClickEvent) { onClickIcon(true) }
                .then(outline),
            contentAlignment = Alignment.Center,
        ) {
            val labelStyle =
                if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (iconSize.value / 2).sp,
                    )
                }
            ProvideTextStyle(labelStyle) {
                CharSequenceText(firstCharacter.orEmpty())
            }
        }
    }
}
