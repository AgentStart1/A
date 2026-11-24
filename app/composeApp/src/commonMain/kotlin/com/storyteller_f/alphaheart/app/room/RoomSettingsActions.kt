/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.room

import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.common.OnRoomUpdated
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.PrimaryKey

internal suspend fun AppGlobalDialogController.updateRoom(
    roomId: PrimaryKey,
    string: String,
    showInputDialog: SettingOption?,
    closeDialog: () -> Unit,
) {
    val body =
        when (showInputDialog) {
            is SettingOption.Name -> {
                UpdateRoomBody(name = string)
            }

            else -> {
                null
            }
        } ?: return
    useResult {
        request { updateRoomInfo(roomId, body) }
    }.onSuccess { newInfo ->
        emitEvent(OnRoomUpdated(newInfo))
        closeDialog()
    }
}
