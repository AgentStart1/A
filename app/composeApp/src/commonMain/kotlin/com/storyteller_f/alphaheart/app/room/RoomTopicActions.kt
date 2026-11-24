/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.room

import androidx.compose.runtime.MutableState
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.OnRoomExited
import com.storyteller_f.alphaheart.app.common.OnRoomJoined
import com.storyteller_f.alphaheart.app.common.RoomKeysViewModel
import com.storyteller_f.alphaheart.app.common.RoomScreen
import com.storyteller_f.alphaheart.app.common.hasRoute
import com.storyteller_f.alphaheart.app.error
import com.storyteller_f.alphaheart.app.error.RoomActionError
import com.storyteller_f.alphaheart.app.message_result_unconfirmed
import com.storyteller_f.alphaheart.app.private_room_pub_key_loading
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.WebSocketClient
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.checkContent
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

internal fun latestVisibleRoomTopicId(keys: List<Any>): PrimaryKey? = keys.filterIsInstance<PrimaryKey>().maxOrNull()

internal fun sendRoomTopic(roomInfo: RoomInfo, input: String, context: SendRoomTopicContext) {
    if (!roomInfo.isJoined || roomInfo.readOnly) return
    val handler = context.keysViewModel.handler
    val keyState = handler.state.value
    val keyData = handler.data.value
    checkContent(input).exceptionOrNull()?.let {
        context.failureDialog.showErrorMessage(it)
        return
    }
    if ((keyState !is LoadingState.Done || keyData == null) && roomInfo.isPrivate) {
        context.scope.launch {
            context.failureDialog.showTitle(getString(Res.string.private_room_pub_key_loading))
        }
        return
    }
    val connection = context.wsClient.connectionHandler.data.value ?: return
    if (!context.wsClient.isCurrentConnection(connection)) return
    val message = PendingRoomMessage(input, context.parentTarget, context.author)
    context.pending.value = message
    context.scope.launch {
        captureFailure({ it }) {
            val currentKeys =
                if (roomInfo.isPrivate) context.keysViewModel.loadCurrentKeys().getOrThrow() else emptyList()
            val submitted =
                prepareRoomMessage(message, { context.pending.value }, {
                    context.wsClient.isCurrentConnection(connection)
                }) {
                    if (roomInfo.isPrivate) {
                        input
                    } else {
                        withContext(Dispatchers.Default) { trimMarkdownUnusedContent(input) }
                    }
                }
            submitted to currentKeys
        }.onSuccess { (submitted, currentKeys) ->
            if (submitted != null) {
                context.wsClient.useWebSocket {
                    if (
                        this === connection && context.wsClient.isCurrentConnection(connection) &&
                        message.owns(context.pending.value)
                    ) {
                        context.pending.value = submitted
                        sendMessage(context.parentTarget, roomInfo.isPrivate, input, currentKeys)
                    } else {
                        context.failPendingRoomMessage(message)
                    }
                }.onFailure { context.failPendingRoomMessage(message, it.cause) }
            } else {
                context.failPendingRoomMessage(message)
            }
        }.onFailure { context.failPendingRoomMessage(message, it) }
    }
}

internal data class SendRoomTopicContext(
    val pending: MutableState<PendingRoomMessage?>,
    val author: PrimaryKey,
    val scope: CoroutineScope,
    val failureDialog: CustomAlertDialogController,
    val keysViewModel: RoomKeysViewModel,
    val wsClient: WebSocketClient,
    val parentTarget: ObjectTuple,
)

private fun WebSocketClient.isCurrentConnection(connection: DefaultClientWebSocketSession): Boolean =
    connectionHandler.state.value is LoadingState.Done &&
        connectionHandler.data.value === connection && connection.isActive

/** A late preparation/send failure must never clear a newer operation's draft ownership. */
private suspend fun SendRoomTopicContext.failPendingRoomMessage(message: PendingRoomMessage, error: Throwable? = null) {
    if (!message.owns(pending.value)) return
    pending.value = null
    if (error != null) {
        failureDialog.showErrorMessage(error)
    } else {
        failureDialog.showTitle(getString(Res.string.message_result_unconfirmed))
    }
}

internal fun checkRoomRouteAndAlert(appNavFactory: AppNavFactory, roomId: PrimaryKey, startJoinRoom: () -> Unit) {
    val appNav = appNavFactory.newAppNav()
    if (appNav.hasRoute<RoomScreen>()) {
        val navKey = appNav.backStack.last()
        if (navKey is RoomScreen && navKey.roomId == roomId) {
            startJoinRoom()
            return
        }
    }
    appNav.gotoRoom(roomId, true)
}

internal suspend fun joinRoom(
    roomInfo: RoomInfo,
    globalDialogController: AppGlobalDialogController,
    onSuccess: suspend () -> Unit,
) {
    globalDialogController.useResult {
        request {
            captureFailure(RoomActionError::from) {
                val communityId = roomInfo.communityId
                if (communityId != null) {
                    check(getCommunityInfo(communityId).getOrThrow().isJoined) {
                        "you should join community first."
                    }
                }
                joinRoom(roomInfo.id).getOrThrow()
            }
        }
    }.onSuccess { info ->
        globalDialogController.emitEvent(OnRoomJoined(info))
        onSuccess()
    }
}

internal suspend fun exitRoom(
    roomInfo: RoomInfo,
    globalDialogController: AppGlobalDialogController,
    onSuccess: suspend () -> Unit,
) {
    globalDialogController.useResult {
        request {
            exitRoom(roomInfo.id)
        }
    }.onSuccess { info ->
        globalDialogController.emitEvent(OnRoomExited(info))
        onSuccess()
    }
}
