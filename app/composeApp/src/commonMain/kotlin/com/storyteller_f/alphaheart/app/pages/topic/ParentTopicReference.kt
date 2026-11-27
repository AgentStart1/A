/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.components.RefCellStateView
import com.storyteller_f.alphaheart.app.parent_topic_author
import com.storyteller_f.alphaheart.app.parent_topic_view
import com.storyteller_f.alphaheart.app.topic.asFeedPreview
import com.storyteller_f.alphaheart.app.topic.parentReferenceArrow
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ParentTopicReference(topicId: PrimaryKey) {
    val handler = LocalRefCellHandlerProvider.current.topicHandler(topicId)
    val nav = LocalAppNavFactory.current
    RefCellStateView(handler, Modifier.fillMaxWidth()) { parent ->
        val preview = remember(parent) { parent.asFeedPreview() }
        ParentTopicReferenceContent(
            author = parent.authorInfo?.nickname ?: parent.author.toString(),
            onClick = { nav.newAppNav().gotoTopic(parent.id) },
        ) {
            AppTopicContentView(preview, isEmbed = true)
        }
    }
}

@Composable
internal fun ParentTopicReferenceContent(author: String, onClick: () -> Unit, preview: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val soft = if (dark) Color(0xFF2D4633) else Color(0xFFE4EEE3)
    val accent = if (dark) Color(0xFFAED4B5) else Color(0xFF32634D)
    val muted = if (dark) Color(0xFFADBCAF) else Color(0xFF63766A)
    val action = stringResource(Res.string.parent_topic_view)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        val horizontal = if (maxWidth <= 600.dp) 12.dp else 18.dp
        val vertical = if (maxWidth <= 600.dp) 12.dp else 14.dp
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(soft).drawBehind {
                val stroke = 3.dp.toPx()
                drawLine(accent, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), strokeWidth = stroke)
            }.semantics { contentDescription = action }
                .appiumSemantics(description = action, onClick = onClick)
                .clickable(onClick = onClick)
                .padding(start = horizontal + 3.dp, end = horizontal, top = vertical, bottom = vertical),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.parent_topic_author, author),
                color = muted,
                fontSize = 12.sp,
                lineHeight = 19.2.sp,
            )
            preview()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(action, color = muted, fontSize = 12.sp, lineHeight = 19.2.sp)
                ReadingIcon(parentReferenceArrow, null, Modifier.size(17.dp), tint = muted)
            }
        }
    }
}
