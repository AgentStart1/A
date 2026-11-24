/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnRoomCreated
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.create_room_action
import com.storyteller_f.alphaheart.app.creation_aid
import com.storyteller_f.alphaheart.app.creation_room_name
import com.storyteller_f.alphaheart.app.pages.CreateFormLayout
import com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithSettingsMenu
import com.storyteller_f.alphaheart.app.pages.title.CommonComposePage
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomComposePage(communityId: PrimaryKey? = null) {
    val community = communityId?.let { createCommunityViewModel(it).handler.data.collectAsState().value }
    var name by remember {
        mutableStateOf("")
    }
    var aid by remember {
        mutableStateOf("")
    }
    val appNavFactory = LocalAppNavFactory.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    CommonComposePage({
        scope.launch {
            globalDialogController.useResult {
                request {
                    createRoom(NewRoom(name, aid, communityId = communityId))
                }
            }.onSuccess { roomInfo ->
                globalDialogController.emitEvent(OnRoomCreated(roomInfo))
                appNavFactory.newAppNav().back()
            }
        }
    }, title = stringResource(Res.string.create_room_action), subtitle = community?.name, leading = {
        CommunityIconWithSettingsMenu(community)
    }) { actions ->
        CreateFormLayout {
            OutlinedTextField(
                name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                label = { Text(stringResource(Res.string.creation_room_name)) },
            )
            OutlinedTextField(
                aid,
                onValueChange = { aid = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                label = { Text(stringResource(Res.string.creation_aid)) },
            )
            actions()
        }
    }
}
