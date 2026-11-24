/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTitleComposeViewModel
import com.storyteller_f.alphaheart.app.pages.room.RoomIconWithDialog
import com.storyteller_f.alphaheart.app.title.roomTitleComposePageCreateTitle
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch

@Composable
fun RoomTitleComposePage(roomId: PrimaryKey) {
    LocalUserInfo.current ?: return
    RoomTitleComposeInternal(roomId)
}

@Composable
private fun RoomTitleComposeInternal(roomId: PrimaryKey) {
    val scope = roomId ob ObjectType.ROOM
    val vm =
        createTitleComposeViewModel(
            initialScope = scope,
            initialType = com.storyteller_f.shared.model.TitleType.JOIN,
            lockScope = true,
            lockType = true,
        )
    val appNavFactory = LocalAppNavFactory.current
    val coroutineScope = rememberCoroutineScope()
    val globalDialog = LocalGlobalDialog.current

    val room by createRoomViewModel(roomId).handler.data.collectAsState()
    CommonComposePage({
        coroutineScope.launch {
            globalDialog.useResult {
                roomTitleComposePageCreateTitle(vm.buildNewTitle().getOrThrow())
            }.onSuccess {
                appNavFactory.newAppNav().back()
            }
        }
    }, subtitle = room?.name, leading = {
        val menu = rememberCommonDialogController()
        RoomIconWithDialog(room, menu.shown.value, 48.dp, updateDialog = menu::update)
    }) { actions ->
        TitleComposeInternalEdit(vm, actions)
    }
    TitleComposeSheet(vm)
}
