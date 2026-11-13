/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.PanelNav
import com.storyteller_f.alphaheart.panel.encrypted
import com.storyteller_f.alphaheart.panel.interaction
import com.storyteller_f.alphaheart.panel.pinned
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopicCell(info: TopicDetail, panelNav: PanelNav) {
    val text =
        when (val content = info.content) {
            is TopicContent.Plain -> content.plain
            is TopicContent.Extracted -> content.plain
            else -> ""
        }
    val author = info.authorInfo?.nickname ?: info.author.toString()
    val room = info.roomInfo?.name.orEmpty()
    val overline = listOf(author, room).filter { it.isNotEmpty() }.joinToString(" @ ")
    val counts =
        if (info.commentCount > 0 || info.reactionCount > 0) {
            stringResource(Res.string.interaction, info.commentCount, info.reactionCount)
        } else {
            ""
        }
    val flags =
        listOfNotNull(
            if (info.isEncrypted) stringResource(Res.string.encrypted) else null,
            if (info.isPin) stringResource(Res.string.pinned) else null,
        ).joinToString(" • ")
    val supporting =
        listOf(
            info.createdTime.toString(),
            counts,
            flags,
        ).filter { it.isNotEmpty() }.joinToString(" • ")

    ListItem(
        modifier = Modifier.clickable { panelNav.gotoTopicDetail(info.id) },
        headlineContent = { Text(text) },
        overlineContent = { Text(overline) },
        supportingContent = {
            Text(supporting)
        },
    )
}
