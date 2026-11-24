/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToastWidthPolicy
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterState
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.close
import com.storyteller_f.alphaheart.app.common.IncomingMessageToast
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadingToasts(state: ToasterState, incoming: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Toaster(
        state = state,
        maxVisibleToasts = 1,
        alignment = Alignment.TopCenter,
        containerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        widthPolicy = { ToastWidthPolicy(max = 408.dp, fillMaxWidth = incoming) },
        background = { SolidColor(colors.surfaceContainer) },
        contentColor = { colors.onSurface },
        border = { BorderStroke(1.dp, colors.outlineVariant) },
        shape = { RoundedCornerShape(if (incoming) 14.dp else 50.dp) },
        elevation = 4.dp,
        contentPadding = {
            if (incoming) {
                PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            } else {
                PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            }
        },
        iconSlot = {},
        messageSlot = { toast ->
            val message = toast.message
            if (message is IncomingMessageToast) {
                IncomingMessageToastContent(message) { state.dismiss(toast) }
            } else {
                FeedbackToastContent(message.toString(), toast.type)
            }
        },
    )
}

@Composable
private fun FeedbackToastContent(message: String, type: ToastType) {
    val color = if (type == ToastType.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val icon =
        when (type) {
            ToastType.Error -> actionIcon(ActionGlyph.CIRCLE_ALERT)
            ToastType.Success -> actionIcon(ActionGlyph.CHECK)
            else -> actionIcon(ActionGlyph.INFO)
        }
    Row(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadingIcon(icon, null, Modifier.size(16.dp), tint = color)
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun IncomingMessageToastContent(message: IncomingMessageToast, dismiss: () -> Unit) {
    Row(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UserIcon(false, message.avatarUrl, size = 32.dp) {}
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.nickname?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                message.roomName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                message.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ReadingIconButton(dismiss, Modifier.size(32.dp)) {
            ReadingIcon(actionIcon(ActionGlyph.X), stringResource(Res.string.close))
        }
    }
}
