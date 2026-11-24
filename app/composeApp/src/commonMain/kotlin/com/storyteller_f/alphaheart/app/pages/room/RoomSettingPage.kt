/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnRoomUpdated
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSettingsLayout
import com.storyteller_f.alphaheart.app.components.ReadingSettingsSection
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingValueRow
import com.storyteller_f.alphaheart.app.field_name
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.user.ObjectSettingDialog
import com.storyteller_f.alphaheart.app.room.updateRoom
import com.storyteller_f.alphaheart.app.settings_aid
import com.storyteller_f.alphaheart.app.settings_basic
import com.storyteller_f.alphaheart.app.settings_room
import com.storyteller_f.alphaheart.app.settings_room_icon
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSettingPage(roomId: PrimaryKey) {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    val roomViewModel = createRoomViewModel(roomId)
    val roomInfo by roomViewModel.handler.data.collectAsState()
    val closeDialog = {
        currentOption = null
    }
    Scaffold(
        topBar = {
            ReadingPageToolbar(
                stringResource(Res.string.settings_room),
                subtitle = roomInfo?.name,
                leading = {
                    if (roomInfo != null) {
                        val menu = rememberCommonDialogController()
                        RoomIconWithDialog(roomInfo, menu.shown.value, width = 48.dp, updateDialog = menu::update)
                    }
                },
            )
        },
    ) { padding ->
        StateView(roomViewModel.handler, Modifier.padding(padding)) { it1 ->
            RoomSettingInternal(PaddingValues(0.dp), { option: SettingOption ->
                currentOption = option
            }, it1)
        }
        val scope = rememberCoroutineScope()
        val globalDialogController = LocalGlobalDialog.current
        ObjectSettingDialog(
            closeDialog,
            currentOption,
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
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    ReadingSettingsLayout(Modifier.padding(values)) {
        ReadingSettingsSection(stringResource(Res.string.settings_basic)) {
            SettingValueRow(stringResource(Res.string.field_name), roomInfo.name, onEdit = {
                showDialog(SettingOption.Name(roomInfo.name))
            })
            SettingValueRow(stringResource(Res.string.settings_aid), roomInfo.aid)
        }
        SettingOptionResettableView(
            stringResource(Res.string.settings_room_icon),
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
    }
}
