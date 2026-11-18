/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.storyteller_f.alphaheart.app.pages.topic.TopicReplyPreviews
import com.storyteller_f.alphaheart.app.ui.theme.AppTheme
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.compose_core.components.PillAppearance
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.model.TopicReplyPreview

@PreviewTest
@Preview(name = "Narrow", showBackground = true, widthDp = 390, heightDp = 520)
@Preview(name = "Wide", showBackground = true, widthDp = 1000, heightDp = 520)
@Composable
fun FeedTopicReplyPreviewsScreenshot() = ReplyPreviewSnapshot(chat = false)

@PreviewTest
@Preview(name = "Narrow", showBackground = true, widthDp = 390, heightDp = 520)
@Preview(name = "Wide", showBackground = true, widthDp = 1000, heightDp = 520)
@Composable
fun ChatTopicReplyPreviewsScreenshot() = ReplyPreviewSnapshot(chat = true)

@Composable
private fun ReplyPreviewSnapshot(chat: Boolean) {
    AppTheme(dynamicColor = false) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 740.dp).fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("User1", style = MaterialTheme.typography.titleMedium)
                    if (!chat) Text("让复杂的表达，变得清楚", style = MaterialTheme.typography.headlineSmall)
                    if (chat) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        ) {
                            Text("一起讨论数学表达式的排版，以及更容易阅读的内容组织方式。", Modifier.padding(16.dp))
                        }
                    } else {
                        Text("一起讨论数学表达式的排版，以及更容易阅读的内容组织方式。")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Pill(
                            text = "2",
                            icon = actionIcon(ActionGlyph.MESSAGE_SQUARE),
                            appearance = PillAppearance.FEED_ACTION,
                        ) {}
                        Pill(
                            icon = actionIcon(ActionGlyph.SMILE),
                            description = "添加回应",
                            appearance = PillAppearance.FEED_ACTION,
                        ) {}
                    }
                    TopicReplyPreviews(
                        listOf(
                            snapshotReply(1, "把公式和解释分开排版，会更容易理解。"),
                            snapshotReply(
                                2,
                                "较长的回复保留两行摘要，避免窄屏只露出几个字。完整内容仍然可以进入话题后阅读。".repeat(3),
                            ),
                        ),
                        chat = chat,
                    ) {
                        UserIcon(setClickEvent = false, avatarUrl = null, size = 24.dp, onClick = {})
                    }
                }
            }
        }
    }
}

private fun snapshotReply(id: Long, text: String): TopicReplyPreview =
    TopicReplyPreview(TopicInfo.EMPTY.copy(id = id, content = TopicContent.Plain(text)), null)
