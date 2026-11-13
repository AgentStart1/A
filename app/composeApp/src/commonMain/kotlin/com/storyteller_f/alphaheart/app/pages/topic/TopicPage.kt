/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.OnTopicCreated
import com.storyteller_f.alphaheart.app.common.TopicViewModel
import com.storyteller_f.alphaheart.app.common.TopicsViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTopicViewModel
import com.storyteller_f.alphaheart.app.common.createTopicsInTopicViewModel
import com.storyteller_f.alphaheart.app.common.createUserViewModel
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.components.InteractionRow
import com.storyteller_f.alphaheart.app.pages.DetailReadingColumn
import com.storyteller_f.alphaheart.app.pages.room.CommonInputButton
import com.storyteller_f.alphaheart.app.pages.room.InputGroupInternal
import com.storyteller_f.alphaheart.app.pages.room.RoomInputGroup
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.search.detailSearchBar
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.components.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.horizontalSafeArea
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import com.storyteller_f.shared.utils.checkContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPage(topicId: PrimaryKey) {
    val viewModel = createTopicViewModel(topicId)
    val subTopicsViewModel = createTopicsInTopicViewModel(topicId)
    val snackBarHostState =
        remember {
            SnackbarHostState()
        }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = {
        SnackbarHost(snackBarHostState)
    }) { paddingValues ->
        TopicPageInternal(
            Modifier.horizontalSafeArea(paddingValues, LocalLayoutDirection.current),
            topicId,
            viewModel,
            subTopicsViewModel,
            snackBarHostState,
        ) {
            showBottomSheet = true
        }
        TopicEmojiPicker(viewModel, sheetState, showBottomSheet) {
            showBottomSheet = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicEmojiPicker(viewModel: TopicViewModel, sheetState: SheetState, showBottomSheet: Boolean, close: () -> Unit) {
    val topic by viewModel.handler.data.collectAsState()
    topic?.let {
        EmojiPicker(sheetState, showBottomSheet, it) {
            close()
        }
    }
}

@Composable
private fun TopicPageInternal(
    modifier: Modifier,
    topicId: PrimaryKey,
    viewModel: TopicViewModel,
    subTopicsViewModel: TopicsViewModel,
    snackBarHostState: SnackbarHostState,
    startAddReaction: () -> Unit,
) {
    val topicInfo by viewModel.handler.data.collectAsState()
    DetailReadingColumn(modifier) {
        var showDialog by remember {
            mutableStateOf(false)
        }
        detailSearchBar(SearchScope.TopicTopic(topicId)) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                topicInfo?.let {
                    val author = it.author
                    val authorViewModel = createUserViewModel(author)
                    val authorInfo by authorViewModel.handler.data.collectAsState()
                    UserIconWithDialog(authorInfo)
                }
                val showTopicDialog = {
                    showDialog = true
                }
                Icon(
                    Icons.Default.Topic,
                    "topic",
                    modifier =
                    Modifier.size(40.dp)
                        .appiumSemantics(
                            description = "topic",
                            onClick = showTopicDialog,
                        )
                        .clip(CircleShape)
                        .clickable(onClick = showTopicDialog)
                        .padding(8.dp),
                )
            }
            TopicDialog(topicInfo, showDialog) {
                showDialog = false
            }
        }
        topicInfo?.let {
            if (it.rootId != it.parentId) {
                Box(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                    TopicRefCell(it.parentId)
                }
            }
        }
        val lazyListState = rememberLazyListState()
        TopicPageContent(viewModel, subTopicsViewModel, startAddReaction, lazyListState)
        val scope = rememberCoroutineScope()
        topicInfo?.let {
            TopicPageInputGroup(it, snackBarHostState) {
                scope.launch {
                    delay(200.milliseconds)
                    lazyListState.animateScrollToItem(1)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TopicPageContent(
    viewModel: TopicViewModel,
    subTopicsViewModel: TopicsViewModel,
    startAddReaction: () -> Unit,
    lazyListState: LazyListState,
) {
    val topicState by viewModel.handler.state.collectAsState()
    val subTopics = subTopicsViewModel.flow.collectAsLazyPagingItems()
    LaunchedEffect(topicState) {
        if (topicState is LoadingState.Done) {
            subTopics.refresh()
        }
    }
    StateView(viewModel.handler, modifier = Modifier.weight(1f)) { topicInfo ->
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = LayoutDefaults.contentPadding,
            verticalArrangement = LayoutDefaults.pagingVerticalArrangement,
            state = lazyListState,
        ) {
            item {
                AppTopicContentView(topicInfo)
                Spacer(modifier = Modifier.height(12.dp))
                InteractionRow(topicInfo, {
                    startAddReaction()
                }) {
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
            }

            topPrepend(subTopics.loadState)
            pagingItems(subTopics, {
                it.id
            }) { subTopicIndex ->
                subTopics[subTopicIndex]?.let { it1 ->
                    TopicCell(it1)
                }
                if (subTopics.itemCount - 1 != subTopicIndex) {
                    HorizontalDivider()
                }
            }
            bottomAppending(subTopics.loadState)
        }
    }
}

@Composable
private fun TopicPageInputGroup(topic: TopicDetail, snackBarHostState: SnackbarHostState, scrollToNew: () -> Unit) {
    val alertDialogState =
        remember {
            CustomAlertDialogController()
        }
    if (topic.rootType == ObjectType.ROOM) {
        val roomId = topic.rootId
        val room = createRoomViewModel(roomId)
        val roomInfo by room.handler.data.collectAsState()
        RoomInputGroup(
            topic.rootId,
            roomInfo,
            ObjectTuple(topic.id, ObjectType.TOPIC),
            snackBarHostState,
            {},
            scrollToNew,
        )
    } else {
        TopicInputGroup(scrollToNew, topic, snackBarHostState)
    }

    val appNavFactory = LocalAppNavFactory.current
    CustomAlertDialog(alertDialogState, {
        alertDialogState.close()
    }) {
        appNavFactory.newAppNav().gotoTopic(topic.rootId)
    }
}

@Composable
private fun TopicInputGroup(scrollTo: () -> Unit, topic: TopicDetail, snackBarHostState: SnackbarHostState) {
    var input by remember {
        mutableStateOf("")
    }
    val appNavFactory = LocalAppNavFactory.current
    val myInfo = LocalUserInfo.current
    val canWrite = myInfo?.status == UserStatus.NORMAL && !topic.readOnly
    InputGroupInternal(
        if (topic.isEncrypted) {
            ObjectTuple(topic.rootId, topic.rootType)
        } else {
            ObjectTuple(myInfo?.id ?: 0, ObjectType.USER)
        },
        MaterialTheme.colorScheme.secondaryContainer,
        input,
        {
            input = it
        },
        {
            val parentTuple = topic.tuple()
            val data =
                when (topic.rootType) {
                    ObjectType.ROOM -> {
                        val roomInfo = topic.roomInfo ?: return@InputGroupInternal
                        roomInfo.communityId?.let {
                            TopicComposeData.PublicRoom(topic.rootId, it, parentTuple)
                        } ?: TopicComposeData.PrivateRoom(topic.rootId, parentTuple)
                    }

                    ObjectType.COMMUNITY -> {
                        TopicComposeData.Community(topic.rootId, parentTuple)
                    }

                    else -> {
                        TopicComposeData.User(topic.rootId, parentTuple)
                    }
                }
            appNavFactory.newAppNav().gotoTopicCompose(data)
        },
        enabled = canWrite,
        sendButton = {
            TopicSendButton(topic, input, {
                input = it
            }, scrollTo, snackBarHostState)
        },
    )
}

@Composable
fun TopicSendButton(
    topic: TopicDetail,
    input: String,
    updateInput: (String) -> Unit,
    scrollToNew: () -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    if (LocalUserInfo.current?.status != UserStatus.NORMAL || topic.readOnly) return
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val globalTask = LocalGlobalTask.current
    val key = "topic ${topic.id}"
    val isSending = globalTask.stateMap[key] is LoadingState.Loading
    CommonInputButton(
        LoadingState.Done,
        input,
        isSending,
    ) {
        sendTopicInTopicPage(
            input,
            topic,
            SendTopicContext(scope, snackBarHostState, globalTask, key, updateInput, focusManager, scrollToNew),
        )
    }
}

private fun sendTopicInTopicPage(input: String, topic: TopicDetail, context: SendTopicContext) {
    checkContent(input).exceptionOrNull()?.let {
        context.scope.launch {
            context.snackBarHostState.showSnackbar(it.message.orEmpty())
        }
        return
    }
    context.globalTask.launch(context.key) {
        use {
            request {
                createTopic(ObjectType.TOPIC, topic.id, input).onSuccess {
                    emitEvent(OnTopicCreated(it))
                    context.updateInput("")
                    context.focusManager.clearFocus()
                    context.scrollToNew()
                }
            }
        }.onFailure {
            context.snackBarHostState
                .showSnackbar(it.cause.message.orEmpty())
        }
    }
}

private data class SendTopicContext(
    val scope: CoroutineScope,
    val snackBarHostState: SnackbarHostState,
    val globalTask: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>,
    val key: String,
    val updateInput: (String) -> Unit,
    val focusManager: FocusManager,
    val scrollToNew: () -> Unit,
)
