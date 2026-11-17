/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.LocalPlatformContext
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.aid_display
import com.storyteller_f.alphaheart.app.all_members
import com.storyteller_f.alphaheart.app.belongs_to_community
import com.storyteller_f.alphaheart.app.bubble
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.IdRoomViewModel
import com.storyteller_f.alphaheart.app.common.OnRoomExited
import com.storyteller_f.alphaheart.app.common.OnRoomJoined
import com.storyteller_f.alphaheart.app.common.OnRoomRead
import com.storyteller_f.alphaheart.app.common.RoomKeysViewModel
import com.storyteller_f.alphaheart.app.common.RoomScreen
import com.storyteller_f.alphaheart.app.common.createRoomKeysViewModel
import com.storyteller_f.alphaheart.app.common.createRoomTopicsViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.hasRoute
import com.storyteller_f.alphaheart.app.common.hasRouteFlow
import com.storyteller_f.alphaheart.app.error
import com.storyteller_f.alphaheart.app.error.RoomActionError
import com.storyteller_f.alphaheart.app.exit_room
import com.storyteller_f.alphaheart.app.files_title
import com.storyteller_f.alphaheart.app.input_is_empty
import com.storyteller_f.alphaheart.app.join_room
import com.storyteller_f.alphaheart.app.join_room_prompt
import com.storyteller_f.alphaheart.app.message_result_unconfirmed
import com.storyteller_f.alphaheart.app.pages.DetailReadingColumn
import com.storyteller_f.alphaheart.app.pages.community.CommunityMenuRefCell
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.search.detailSearchBar
import com.storyteller_f.alphaheart.app.pages.topic.RoomTopicList
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.pages.user.ButtonBadgeSuffix
import com.storyteller_f.alphaheart.app.permission_denied
import com.storyteller_f.alphaheart.app.private_room_pub_key_loading
import com.storyteller_f.alphaheart.app.refresh
import com.storyteller_f.alphaheart.app.send
import com.storyteller_f.alphaheart.app.settings_title
import com.storyteller_f.alphaheart.app.start_call
import com.storyteller_f.alphaheart.app.success
import com.storyteller_f.alphaheart.app.utils.appPlatformImpl
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.horizontalSafeArea
import com.storyteller_f.alphaheart.client.compose_core.components.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.getRemoteImageBitmap
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.WebSocketClient
import com.storyteller_f.alphaheart.client.core.addReadLog
import com.storyteller_f.alphaheart.client.core.decryptCreatedTopic
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getPlatform
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import com.storyteller_f.shared.utils.checkContent
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun RoomPage(roomId: PrimaryKey, needShowDialog: Boolean) {
    val room = createRoomViewModel(roomId)
    RoomPageInternal(room, needShowDialog, roomId)
}

@Composable
private fun RoomPageInternal(room: IdRoomViewModel, needShowDialog: Boolean, roomId: PrimaryKey) {
    Scaffold { paddingValues ->
        DetailReadingColumn(Modifier.horizontalSafeArea(paddingValues, LocalLayoutDirection.current)) {
            var showDialog by remember {
                mutableStateOf(false)
            }
            val dialogShown by room.dialog.dialogShown.collectAsState()
            LaunchedEffect(needShowDialog, dialogShown) {
                if (needShowDialog && !dialogShown) {
                    room.dialog.markDialogShown()
                    showDialog = true
                }
            }
            val roomInfo by room.handler.data.collectAsState()
            detailSearchBar(SearchScope.RoomTopic(roomId)) {
                RoomIconWithDialog(
                    roomInfo,
                    showDialog = showDialog,
                    width = 40.dp,
                    setClickEvent = true,
                ) {
                    showDialog = it
                }
            }
            val lazyListState = rememberLazyListState()
            StateView(room.handler, Modifier.weight(1f)) {
                val roomId = it.id
                val viewModel = createRoomTopicsViewModel(roomId)
                val items = viewModel.flow.collectAsLazyPagingItems()
                Box(Modifier.weight(1f)) {
                    RoomTopicList(items, viewModel, lazyListState)
                    NewTopicView(lazyListState, it)
                }
            }
            val scope = rememberCoroutineScope()
            roomInfo?.let {
                RoomInputGroup(roomId, it, roomId ob ObjectType.ROOM, {
                    showDialog = true
                }) {
                    scope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }
            }
        }
    }
}

internal fun latestVisibleRoomTopicId(keys: List<Any>): PrimaryKey? = keys.filterIsInstance<PrimaryKey>().maxOrNull()

@Composable
private fun BoxScope.NewTopicView(lazyListState: LazyListState, roomInfo: RoomInfo) {
    val firstVisibleItemScrollOffset by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemScrollOffset
        }
    }
    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val sessionManager = LocalSessionManager.current
    val events = LocalGlobalDialog.current.context.events
    val visibleTopicId by remember(lazyListState) {
        derivedStateOf {
            latestVisibleRoomTopicId(lazyListState.layoutInfo.visibleItemsInfo.map { it.key })
        }
    }
    LaunchedEffect(visibleTopicId, roomInfo.id) {
        val topicId = visibleTopicId ?: return@LaunchedEffect
        delay(1000)
        try {
            sessionManager.addReadLog(UpdateUserRead(roomInfo.tuple(), topicId))
                .getOrThrow()
            events.emit(OnRoomRead(roomInfo.id))
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Napier.e(e) {
                "add read log failed"
            }
        }
    }
    if (firstVisibleItemScrollOffset != 0 || firstVisibleItemIndex != 0) {
        val scope = rememberCoroutineScope()
        IconButton({
            scope.launch {
                lazyListState.animateScrollToItem(0, 0)
            }
        }, modifier = Modifier.align(Alignment.BottomStart)) {
            Icon(Icons.Default.ArrowCircleDown, "move to newer topic")
        }
    }
}

@Composable
fun RoomInputGroup(
    roomId: PrimaryKey,
    roomInfo: RoomInfo?,
    parentTarget: ObjectTuple,
    startJoinRoom: () -> Unit,
    currentTopicReadOnly: Boolean = false,
    scrollToNew: () -> Unit,
) {
    val appNavFactory = LocalAppNavFactory.current
    var input by rememberSaveable(roomId, parentTarget) {
        mutableStateOf("")
    }
    val controller =
        remember {
            CustomAlertDialogController()
        }
    if (roomInfo != null) {
        RoomInputGroupInternal(roomId, roomInfo, parentTarget, controller, input, scrollToNew, currentTopicReadOnly) {
            input = it
        }
    }

    CustomAlertDialog(controller, {
        controller.close()
    }) {
        checkRoomRouteAndAlert(appNavFactory, roomId, startJoinRoom)
    }
}

@Composable
private fun RoomInputGroupInternal(
    roomId: PrimaryKey,
    roomInfo: RoomInfo,
    parentTarget: ObjectTuple,
    controller: CustomAlertDialogController,
    input: String,
    scrollToNew: () -> Unit,
    currentTopicReadOnly: Boolean,
    updateInput: (String) -> Unit,
) {
    val myInfo = LocalUserInfo.current
    val canWrite = canWriteRoomMessage(myInfo?.status, roomInfo.isJoined, roomInfo.readOnly, currentTopicReadOnly)
    val mediaTarget =
        if (roomInfo.isPrivate) {
            ObjectTuple(roomInfo.id, ObjectType.ROOM)
        } else {
            ObjectTuple(myInfo?.id ?: 0, ObjectType.USER)
        }

    val appNavFactory = LocalAppNavFactory.current
    InputGroupInternal(
        mediaTarget,
        MaterialTheme.colorScheme.tertiaryContainer,
        input,
        updateInput,
        {
            appNavFactory.newAppNav().gotoTopicCompose(
                roomInfo.communityId?.let {
                    TopicComposeData.PublicRoom(roomId, it, parentTarget)
                } ?: TopicComposeData.PrivateRoom(roomId, parentTarget),
            )
        },
        {
            RoomInputTopContent(roomInfo)
        },
        {
            if (canWrite) {
                RoomSendButton(
                    input,
                    roomInfo,
                    parentTarget,
                    controller,
                    onSent = { updateInput("") },
                    scrollToNew = scrollToNew,
                )
            }
        },
        enabled = canWrite,
        replyingToTopic = parentTarget.objectType == ObjectType.TOPIC,
    )
}

@Composable
private fun RoomInputTopContent(roomInfo: RoomInfo) {
    val keysViewModel = createRoomKeysViewModel(roomInfo.id, roomInfo)
    val keysState by keysViewModel.handler.state.collectAsState()
    if (roomInfo.isPrivate) {
        com.storyteller_f.alphaheart.client.compose_core.components.CachedRefreshIndicator(
            keysState,
            keysViewModel.handler::refresh,
        )
    }
}

private fun sendRoomTopic(roomInfo: RoomInfo, input: String, context: SendRoomTopicContext) {
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

private data class SendRoomTopicContext(
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

private fun checkRoomRouteAndAlert(appNavFactory: AppNavFactory, roomId: PrimaryKey, startJoinRoom: () -> Unit) {
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

@Composable
fun RoomSendButton(
    input: String,
    roomInfo: RoomInfo,
    parentTarget: ObjectTuple,
    controller: CustomAlertDialogController,
    onSent: (() -> Unit)? = null,
    scrollToNew: () -> Unit,
) {
    if (LocalUserInfo.current?.status != UserStatus.NORMAL || roomInfo.readOnly) return
    val failureDialog = rememberAlertDialogController()
    CustomAlertDialog(failureDialog, failureDialog::close, failureDialog::close)
    val scope = rememberCoroutineScope()
    val uiViewModel = LocalUiViewModel.current
    val instance by uiViewModel.instance.collectAsState()
    val wsClient = instance.sessionManager.webSocketClient
    val state by wsClient.connectionHandler.state.collectAsState()
    val sendState by wsClient.localState.collectAsState()
    val pending =
        rememberRoomMessageReceipt(
            wsClient,
            roomInfo.id,
            parentTarget,
            input,
            onSent,
            scrollToNew,
            failureDialog,
        )
    val myInfo = LocalUserInfo.current
    val isSending = sendState is LoadingState.Loading || pending.value != null
    val keysViewModel = createRoomKeysViewModel(roomInfo.id, roomInfo)
    CommonInputButton(state, input, isSending, retry = { wsClient.retryConnection() }) {
        if (pending.value != null) return@CommonInputButton
        if (roomInfo.isJoined) {
            sendRoomTopic(
                roomInfo,
                input,
                SendRoomTopicContext(
                    pending,
                    myInfo?.id ?: 0,
                    scope,
                    failureDialog,
                    keysViewModel,
                    wsClient,
                    parentTarget,
                ),
            )
        } else {
            scope.launch {
                val title = getString(Res.string.permission_denied)
                val message = getString(Res.string.join_room_prompt)
                controller.showMessage(title, message)
            }
        }
    }
}

@Composable
private fun rememberRoomMessageReceipt(
    wsClient: WebSocketClient,
    roomId: PrimaryKey,
    parentTarget: ObjectTuple,
    input: String,
    onSent: (() -> Unit)?,
    scrollToNew: () -> Unit,
    failureDialog: CustomAlertDialogController,
): MutableState<PendingRoomMessage?> {
    val state by wsClient.connectionHandler.state.collectAsState()
    val sendState by wsClient.localState.collectAsState()
    val pending = remember(roomId, parentTarget) { mutableStateOf<PendingRoomMessage?>(null) }
    var pendingMessage by pending
    val currentInput by rememberUpdatedState(input)
    val completeDraft by rememberUpdatedState(onSent)
    val showLatest by rememberUpdatedState(scrollToNew)
    val sessionManager = LocalSessionManager.current
    LaunchedEffect(sendState) {
        (sendState as? LoadingState.Error)?.let {
            pendingMessage = null
            failureDialog.showErrorMessage(it.e)
        }
    }
    LaunchedEffect(state) {
        if (state !is LoadingState.Done && pendingMessage != null) {
            pendingMessage = null
            failureDialog.showTitle(getString(Res.string.message_result_unconfirmed))
        }
    }
    LaunchedEffect(wsClient, parentTarget) {
        wsClient.frameFlow.collect { frame ->
            if (frame is RoomFrame.Error && pendingMessage != null) {
                pendingMessage = null
                failureDialog.showTitle(frame.error)
            } else if (frame is RoomFrame.NewTopicInfo) {
                val pending = pendingMessage ?: return@collect
                val topic =
                    if (frame.topicInfo.content is TopicContent.Encrypted) {
                        decryptCreatedTopic(frame.topicInfo, sessionManager)
                    } else {
                        frame.topicInfo
                    }
                if (pending.owns(pendingMessage) && pending.matches(topic)) {
                    pendingMessage = null
                    showLatest()
                    if (currentInput == pending.content) completeDraft?.invoke()
                }
            }
        }
    }
    return pending
}

@Composable
fun CommonInputButton(
    state: LoadingState?,
    input: String,
    isSending: Boolean,
    retry: (() -> Unit)? = null,
    send: () -> Unit,
) {
    val alertDialogController = rememberAlertDialogController()
    val scope = rememberCoroutineScope()
    CustomAlertDialog(alertDialogController, alertDialogController::close, alertDialogController::close)
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is LoadingState.Done -> {
                val submit: () -> Unit = {
                    if (input.isBlank()) {
                        scope.launch {
                            alertDialogController.showTitle(getString(Res.string.input_is_empty))
                        }
                    } else {
                        send()
                    }
                }
                androidx.compose.material3.Button(
                    onClick = submit,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(0.dp),
                    modifier =
                    Modifier.appiumSemantics(
                        description = stringResource(Res.string.send),
                        onClick = submit,
                    ),
                    enabled = !isSending,
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(actionIcon(ActionGlyph.SEND), stringResource(Res.string.send))
                    }
                }
            }

            is LoadingState.Error -> {
                IconButton({
                    if (retry != null) retry() else alertDialogController.showErrorMessage(state.e)
                }) {
                    if (retry != null) {
                        Icon(Icons.Default.Refresh, stringResource(Res.string.refresh))
                    } else {
                        Icon(Icons.Default.Error, stringResource(Res.string.error))
                    }
                }
            }

            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun RoomDialogInternal(roomInfo: RoomInfo, dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ReadingActionMenuSurface(onDismiss = dismiss, heading = {
        Row(
            modifier =
            Modifier.fillMaxWidth(),
            Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val commonDialogController = rememberCommonDialogController()
            RoomIcon(roomInfo, 48.dp, false, 4.dp, commonDialogController::update)
            Column(Modifier.weight(1f)) {
                Text(roomInfo.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(Res.string.aid_display, roomInfo.aid), fontSize = 12.sp)
            }
        }
    }) {
        roomInfo.communityId?.let { communityId ->
            Text(
                stringResource(Res.string.belongs_to_community),
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                fontSize = 12.sp,
            )
            CommunityMenuRefCell(communityId) {
                dismiss()
                appNavFactory.newAppNav().gotoCommunity(communityId, false)
            }
        }

        RoomDialogButtons(roomInfo, dismiss)
    }
}

@Composable
private fun RoomDialogButtons(roomInfo: RoomInfo, dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    val me = LocalUserInfo.current
    val globalDialogController = LocalGlobalDialog.current
    IconActionMenu {
        val isRoomPage = appNavFactory.hasRouteFlow<RoomScreen>()
        if (isRoomPage) {
            RoomAllMembers(roomInfo, dismiss, appNavFactory)
            RoomMemberStatus(roomInfo, globalDialogController)
            FavoriteButton(roomInfo.favoriteId, roomInfo.tuple())
            SubscriptionButton(roomInfo.subscriptionId, roomInfo.tuple())
            StartCallButton(roomInfo, dismiss)
            if (roomInfo.isPrivate || roomInfo.creator == me?.id) {
                RoomFileExplorerButton(roomInfo, dismiss, appNavFactory)
            }
            RoomSettings(roomInfo, me, dismiss, appNavFactory)
        }
    }
}

@Composable
private fun RoomSettings(roomInfo: RoomInfo, me: UserInfo?, dismiss: () -> Unit, appNavFactory: AppNavFactory) {
    if (roomInfo.creator == me?.id) {
        ButtonNav(actionIcon(ActionGlyph.SETTINGS), stringResource(Res.string.settings_title)) {
            dismiss()
            appNavFactory.newAppNav().gotoSettingPage(roomInfo.id, ObjectType.ROOM)
        }
    }
}

@Composable
private fun RoomFileExplorerButton(roomInfo: RoomInfo, dismiss: () -> Unit, appNavFactory: AppNavFactory) {
    ButtonNav(navigationIcon(NavigationGlyph.FOLDER), stringResource(Res.string.files_title)) {
        dismiss()
        appNavFactory.newAppNav().gotoFileExplorer(roomInfo.id ob ObjectType.ROOM)
    }
}

@Composable
private fun RoomAllMembers(roomInfo: RoomInfo, dismiss: () -> Unit, appNavFactory: AppNavFactory) {
    ButtonNav(
        IconRes.Vector(navigationIcon(NavigationGlyph.USERS)),
        stringResource(Res.string.all_members),
        {
            ButtonBadgeSuffix(roomInfo.memberCount)
        },
        semanticDescription = "all-members-action",
    ) {
        dismiss()
        appNavFactory.newAppNav().gotoMemberPage(roomInfo.id, ObjectType.ROOM)
    }
}

@Composable
private fun RoomMemberStatus(roomInfo: RoomInfo, globalDialogController: AppGlobalDialogController) {
    val scope = rememberCoroutineScope()

    val toasterState = LocalToaster.current
    if (roomInfo.isJoined) {
        ButtonNav(navigationIcon(NavigationGlyph.LOG_OUT), stringResource(Res.string.exit_room)) {
            scope.launch {
                exitRoom(roomInfo, globalDialogController) {
                    toasterState.showMessage(getString(Res.string.success))
                }
            }
        }
    } else {
        ButtonNav(actionIcon(ActionGlyph.LOG_IN), stringResource(Res.string.join_room)) {
            scope.launch {
                joinRoom(roomInfo, globalDialogController) {
                    val message = getString(Res.string.success)
                    toasterState.showMessage(message)
                }
            }
        }
    }
}

@Composable
private fun StartCallButton(roomInfo: RoomInfo, dismiss: () -> Unit) {
    val nav = LocalAppNavFactory.current.newAppNav()
    val sessionManager = LocalSessionManager.current

    val scope = rememberCoroutineScope()

    ButtonNav(actionIcon(ActionGlyph.PHONE), stringResource(Res.string.start_call)) {
        dismiss()
        nav.gotoCall(roomInfo.id)
    }
    if (getPlatform().name.contains("android", ignoreCase = true)) {
        val context = LocalPlatformContext.current
        ButtonNav(actionIcon(ActionGlyph.MESSAGE_CIRCLE), stringResource(Res.string.bubble)) {
            scope.launch {
                val bitmap =
                    roomInfo.icon?.let { getRemoteImageBitmap(sessionManager, context, it) }
                        ?.getOrNull()
                appPlatformImpl.notifyNotification(roomInfo, bitmap)
            }
        }
    }
}

private suspend fun joinRoom(
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

private suspend fun exitRoom(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDialog(showDialog: Boolean, roomInfo: RoomInfo?, dismiss: () -> Unit) {
    if (roomInfo != null && showDialog) {
        BasicAlertDialog({
            dismiss()
        }) {
            RoomDialogInternal(roomInfo, dismiss)
        }
    }
}
