/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.common.OnRoomUpdated
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.user.ObjectSettingDialog
import com.storyteller_f.alphaheart.app.pages.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSettingPage(roomId: PrimaryKey) {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    val roomViewModel = createRoomViewModel(roomId)
    val roomInfo by roomViewModel.handler.data.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val closeDialog = {
        currentOption = null
    }
    Scaffold { padding ->
        roomInfo?.let { it1 ->
            RoomSettingInternal(padding, { option: SettingOption ->
                currentOption = option
            }, it1)
        }
        val scope = rememberCoroutineScope()
        val globalDialogController = LocalGlobalDialog.current
        ObjectSettingDialog(
            closeDialog,
            currentOption,
            sheetState,
            {
                scope.launch {
                    globalDialogController.useResult {
                        val body = UpdateRoomBody(icon = it.id)
                        request { updateRoomInfo(roomId, body) }
                    }.onSuccess { newInfo ->
                        globalDialogController.emitEvent(OnRoomUpdated(newInfo))
                    }
                }
            },
            {
                scope.launch {
                    globalDialogController.updateRoom(roomId, it, currentOption, closeDialog)
                }
            },
        )
    }
}

@Composable
private fun RoomSettingInternal(values: PaddingValues, showDialog: (SettingOption) -> Unit, roomInfo: RoomInfo) {
    val toasterState = LocalToaster.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    AdaptiveFormLayout(Modifier.padding(values)) {
        SettingOptionResettableView(
            "Icon",
            roomInfo.icon != null,
            { shouldReset ->
                if (shouldReset) {
                    scope.launch {
                        globalDialogController.useResult {
                            val body = UpdateRoomBody(icon = 0)
                            request { updateRoomInfo(roomInfo.id, body) }
                        }.onSuccess { newInfo ->
                            globalDialogController.emitEvent(OnRoomUpdated(newInfo))
                        }
                    }
                } else {
                    showDialog(
                        SettingOption.RoomIcon(
                            roomInfo.icon?.fullName,
                            roomInfo.id.takeIf { roomInfo.isPrivate },
                        ),
                    )
                }
            },
            {
                RoomIconWithDialog(roomInfo, showDialog = false, setClickEvent = false) {}
            },
        )
        SettingOptionView("Name", {
            showDialog(SettingOption.Name(roomInfo.name))
        }, {
            Text(roomInfo.name, textDecoration = TextDecoration.Underline)
        })
        val aid = roomInfo.aid
        SettingOptionView("Aid", {
            toasterState.showMessage("forbid")
        }, {
            Text(aid)
        })
    }
}

private suspend fun AppGlobalDialogController.updateRoom(
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
