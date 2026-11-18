/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.invalid_content
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicReplyPreview
import org.jetbrains.compose.resources.stringResource

/** Excerpts inherit the enclosing topic's navigation; avatars retain their own actions. */
@Composable
fun TopicReplyPreviews(
    replies: List<TopicReplyPreview>,
    chat: Boolean = false,
    avatar: @Composable (TopicReplyPreview) -> Unit,
) {
    if (replies.isEmpty()) return
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().testTag("topic-reply-previews")) {
        replies.forEachIndexed { index, reply ->
            key(reply.id) {
                Box(
                    Modifier.fillMaxWidth().testTag("topic-reply-${reply.id}").drawBehind {
                        if (index < replies.lastIndex) {
                            val x =
                                if (layoutDirection == LayoutDirection.Rtl) size.width - 12.dp.toPx() else 12.dp.toPx()
                            drawLine(
                                edgeColor,
                                Offset(x, 34.dp.toPx()),
                                Offset(x, size.height + 4.dp.toPx()),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    },
                ) {
                    ReplyPreviewRow(reply, chat, avatar)
                }
            }
        }
    }
}

@Composable
private fun ReplyPreviewRow(reply: TopicReplyPreview, chat: Boolean, avatar: @Composable (TopicReplyPreview) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(contentAlignment = Alignment.Center) { avatar(reply) }
        val summary =
            when (val content = reply.content) {
                is TopicContent.Plain -> content.plain
                is TopicContent.Extracted -> content.plain
                else -> stringResource(Res.string.invalid_content)
            }
        val bubble =
            if (chat) {
                Modifier.clip(RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp)
            } else {
                Modifier
            }
        Text(
            summary,
            Modifier.weight(1f, fill = !chat).then(bubble).testTag("topic-reply-summary-${reply.id}"),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
