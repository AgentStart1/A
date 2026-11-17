/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_topic
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.create_community_action
import com.storyteller_f.alphaheart.app.create_room_action
import com.storyteller_f.alphaheart.app.create_title_action
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog

@Composable
fun ComposeMenu(show: Boolean, onDismiss: () -> Unit, onCheck: (ObjectType) -> Unit) {
    if (show) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                ComposeMenuContent(onDismiss, onCheck)
            }
        }
    }
}

/** Pure menu chrome; caller retains its original creation target and permission handling. */
@Composable
internal fun ComposeMenuContent(onDismiss: () -> Unit, onCheck: (ObjectType) -> Unit, compact: Boolean? = null) {
    var completed by remember { mutableStateOf(false) }
    val dismiss = {
        if (!completed) {
            completed = true
            onDismiss()
        }
    }
    val select: (ObjectType) -> Unit = { type ->
        if (!completed) {
            completed = true
            onDismiss()
            onCheck(type)
        }
    }
    ReadingActionMenuSurface(
        onDismiss = dismiss,
        heading = {
            Text(
                stringResource(Res.string.create),
                fontSize = 17.sp,
                lineHeight = 27.2.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        compact = compact,
    ) {
        // Final third-dialog-body margin22 plus its menu grid margin12.
        IconActionMenu(topSpacing = 34.dp) {
            CreateMenuAction(ActionGlyph.FILE_PLUS, stringResource(Res.string.add_topic), ObjectType.TOPIC, select)
            CreateMenuAction(
                ActionGlyph.SQUARE_PLUS,
                stringResource(Res.string.create_community_action),
                ObjectType.COMMUNITY,
                select,
            )
            CreateMenuAction(
                ActionGlyph.MESSAGE_SQUARE_PLUS,
                stringResource(Res.string.create_room_action),
                ObjectType.ROOM,
                select,
            )
            CreateMenuAction(
                ActionGlyph.BADGE_PLUS,
                stringResource(Res.string.create_title_action),
                ObjectType.TITLE,
                select,
            )
        }
    }
}

@Composable
private fun CreateMenuAction(glyph: ActionGlyph, label: String, type: ObjectType, select: (ObjectType) -> Unit) {
    ButtonNav(IconRes.Vector(actionIcon(glyph), label), label) { select(type) }
}
