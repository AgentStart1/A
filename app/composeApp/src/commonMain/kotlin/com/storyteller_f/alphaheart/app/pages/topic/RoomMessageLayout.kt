/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime

/** Presentation only: message renderers and identity/actions retain their own owners. */
@Composable
internal fun RoomMessageLayout(
    ownMessage: Boolean,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    showIdentity: Boolean = true,
    sentAt: LocalDateTime? = null,
    avatar: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    actionsTopPadding: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        val messageWidth = maxWidth * if (compact) 0.95f else 0.88f
        val side = if (ownMessage) Alignment.End else Alignment.Start
        Row(
            Modifier.widthIn(max = messageWidth).align(if (ownMessage) Alignment.TopEnd else Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (!ownMessage && (showIdentity || senderName != null)) {
                Box(Modifier.size(34.dp)) { if (showIdentity) avatar() }
            }
            Column(Modifier.weight(1f, fill = false), horizontalAlignment = side) {
                val visibleSender = senderName?.takeIf { showIdentity && !ownMessage }
                if (sentAt != null || visibleSender != null) {
                    Row(
                        Modifier.padding(horizontal = 3.dp).padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (visibleSender != null) {
                            Text(
                                visibleSender,
                                Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (sentAt != null) TopicRelativeTime(sentAt)
                    }
                }
                RoomMessageBubble(ownMessage, compact, content)
                Box(
                    Modifier.fillMaxWidth().padding(top = actionsTopPadding),
                    contentAlignment = if (ownMessage) Alignment.TopEnd else Alignment.TopStart,
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
private fun RoomMessageBubble(ownMessage: Boolean, compact: Boolean, content: @Composable () -> Unit) {
    Surface(
        shape =
        RoundedCornerShape(
            topStart = if (ownMessage) 15.dp else 3.dp,
            topEnd = if (ownMessage) 3.dp else 15.dp,
            bottomEnd = 15.dp,
            bottomStart = 15.dp,
        ),
        color =
        if (ownMessage) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (ownMessage) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 12.dp else 15.dp)) {
            content()
        }
    }
}
