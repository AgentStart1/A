/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.RefCellStateView
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.PrimaryKey

@Composable
fun RoomRefCell(roomId: PrimaryKey, onClick: ((RoomInfo) -> Unit)? = null) {
    val handler = LocalRefCellHandlerProvider.current.roomHandler(roomId)
    RoomRefCellInternal(handler, onClick)
}

@Composable
fun RoomRefCell(roomAid: String, onClick: ((RoomInfo) -> Unit)? = null) {
    val handler = LocalRefCellHandlerProvider.current.roomHandler(roomAid)
    RoomRefCellInternal(handler, onClick)
}

@Composable
private fun RoomRefCellInternal(handler: LoadingHandler<RoomInfo>, onClick: ((RoomInfo) -> Unit)? = null) {
    val roomInfo by handler.data.collectAsState()
    val appNavFactory = LocalAppNavFactory.current
    val openRoom = {
        roomInfo?.let {
            onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoRoom(it.id, false)
        }
        Unit
    }
    val shape = RoundedCornerShape(10.dp)
    RefCellStateView(
        handler,
        modifier =
        Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .clip(shape)
            .clickable(onClick = openRoom)
            .appiumSemantics(
                description = roomInfo?.let { "${it.name}, room reference ${it.id}" },
                text = roomInfo?.name,
                onClick = openRoom,
            )
            .padding(10.dp),
    ) {
        UnboundedRoomCell(it)
    }
}
