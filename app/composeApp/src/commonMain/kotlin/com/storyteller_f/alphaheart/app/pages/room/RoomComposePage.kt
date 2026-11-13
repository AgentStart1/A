/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.aid
import com.storyteller_f.alphaheart.app.common.OnRoomCreated
import com.storyteller_f.alphaheart.app.name
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.title.CommonComposePage
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.result.onSuccess
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomComposePage() {
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
                    createRoom(NewRoom(name, aid))
                }
            }.onSuccess { roomInfo ->
                globalDialogController.emitEvent(OnRoomCreated(roomInfo))
                appNavFactory.newAppNav().back()
            }
        }
    }) {
        AdaptiveFormLayout {
            OutlinedTextField(name, onValueChange = {
                name = it
            }, label = {
                Text(stringResource(Res.string.name))
            })
            OutlinedTextField(aid, onValueChange = {
                aid = it
            }, label = {
                Text(stringResource(Res.string.aid))
            })
        }
    }
}
