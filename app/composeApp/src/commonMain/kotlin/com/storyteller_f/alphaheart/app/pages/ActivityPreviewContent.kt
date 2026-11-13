/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.activity_attachments
import com.storyteller_f.alphaheart.app.activity_empty
import com.storyteller_f.alphaheart.app.activity_empty_message
import com.storyteller_f.alphaheart.app.activity_load_failed
import com.storyteller_f.alphaheart.app.activity_no_messages
import com.storyteller_f.alphaheart.app.activity_preview_count
import com.storyteller_f.alphaheart.app.activity_unavailable
import com.storyteller_f.alphaheart.app.common.MessagePreview
import com.storyteller_f.alphaheart.app.common.MessageSummary
import com.storyteller_f.alphaheart.app.common.PreviewState
import com.storyteller_f.alphaheart.app.common.RoomActivityPreview
import com.storyteller_f.alphaheart.app.refresh
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

/** Runtime-free preview state and refresh action, shared by both Home context cards. */
@Composable
fun <T> ActivityPreviewSection(
    title: String,
    state: PreviewState<T>,
    refresh: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
    item: @Composable (T) -> Unit,
) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    Modifier.weight(1f).appiumSemantics(text = title),
                    style = MaterialTheme.typography.titleMedium,
                )
                action()
            }
            TextButton(
                refresh,
                modifier = Modifier.appiumSemantics(text = stringResource(Res.string.refresh), onClick = refresh),
                enabled = state !is PreviewState.Loading,
            ) { Text(stringResource(Res.string.refresh)) }
            when (state) {
                PreviewState.Idle, PreviewState.Loading -> CircularProgressIndicator(Modifier.size(20.dp))

                PreviewState.Failed -> Text(stringResource(Res.string.activity_load_failed))

                is PreviewState.Ready -> {
                    if (state.items.isEmpty()) Text(stringResource(Res.string.activity_empty))
                    state.items.forEach { item(it) }
                    if (state.total > state.items.size) {
                        Text(
                            stringResource(Res.string.activity_preview_count, state.items.size, state.total),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomActivityPreviewContent(preview: RoomActivityPreview, modifier: Modifier = Modifier, open: () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().clickable(onClick = open)
            .appiumSemantics(text = roomActivityDescription(preview.room.name, preview), onClick = open).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                preview.room.name,
                Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            if (preview.unreadCount > 0) {
                Badge {
                    Text(
                        preview.unreadCount.toString(),
                        Modifier.appiumSemantics(text = preview.unreadCount.toString()),
                    )
                }
            }
        }
        ActivityMessageContent(preview.message)
    }
}

@Composable
fun ActivityMessageContent(message: MessagePreview?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val summaryText = activityMessageSummary(message)
        Text(
            summaryText,
            Modifier.appiumSemantics(text = summaryText),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
        if (message != null) {
            listOfNotNull(message.author, message.time).forEach { metadata ->
                Text(
                    metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A merged clickable row must retain the message and badge in its accessible name. */
@Composable
internal fun roomActivityDescription(roomName: String, activity: RoomActivityPreview?): String =
    if (activity == null) {
        roomName
    } else {
        listOfNotNull(
            roomName,
            activityMessageSummary(activity.message),
            activity.message?.author,
            activity.message?.time,
            activity.unreadCount.takeIf { it > 0 }?.toString(),
        ).joinToString(", ")
    }

@Composable
private fun activityMessageSummary(message: MessagePreview?): String =
    when (val summary = message?.summary) {
        is MessageSummary.Text -> summary.value
        MessageSummary.Unavailable -> stringResource(Res.string.activity_unavailable)
        MessageSummary.Attachment -> stringResource(Res.string.activity_attachments)
        MessageSummary.Empty -> stringResource(Res.string.activity_empty_message)
        null -> stringResource(Res.string.activity_no_messages)
    }
