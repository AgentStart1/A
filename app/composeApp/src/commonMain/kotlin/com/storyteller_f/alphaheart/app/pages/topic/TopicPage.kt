/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.mikepenz.markdown.model.State
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.TopicViewModel
import com.storyteller_f.alphaheart.app.common.TopicsViewModel
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
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
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.topic.CURRENT_TOPIC_ACTIONS_KEY
import com.storyteller_f.alphaheart.app.topic.SendTopicContext
import com.storyteller_f.alphaheart.app.topic.TopicActionsPresentation
import com.storyteller_f.alphaheart.app.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.topic.currentTopicBlockCount
import com.storyteller_f.alphaheart.app.topic.currentTopicMarkdown
import com.storyteller_f.alphaheart.app.topic.sendTopicInTopicPage
import com.storyteller_f.alphaheart.app.topic.topicDetailReplies
import com.storyteller_f.alphaheart.app.topic_detail_community
import com.storyteller_f.alphaheart.app.topic_detail_community_reply
import com.storyteller_f.alphaheart.app.topic_detail_room
import com.storyteller_f.alphaheart.app.topic_detail_room_reply
import com.storyteller_f.alphaheart.app.topic_detail_user
import com.storyteller_f.alphaheart.app.topic_detail_user_reply
import com.storyteller_f.alphaheart.app.topics
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.theme.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.theme.horizontalSafeArea
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPage(topicId: PrimaryKey) {
    val viewModel = createTopicViewModel(topicId)
    val subTopicsViewModel = createTopicsInTopicViewModel(topicId)
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        TopicPageInternal(
            Modifier.horizontalSafeArea(paddingValues, LocalLayoutDirection.current),
            topicId,
            viewModel,
            subTopicsViewModel,
        ) {
            showBottomSheet = true
        }
        TopicEmojiPicker(viewModel, showBottomSheet) {
            showBottomSheet = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicEmojiPicker(viewModel: TopicViewModel, showBottomSheet: Boolean, close: () -> Unit) {
    val topic by viewModel.handler.data.collectAsState()
    topic?.let {
        EmojiPicker(showBottomSheet, it) {
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
    startAddReaction: () -> Unit,
) {
    val topicInfo by viewModel.handler.data.collectAsState()
    DetailReadingColumn(modifier) {
        TopicDetailToolbar(topicId, topicInfo)
        val lazyListState = rememberLazyListState()
        val cachedMarkdown by viewModel.markdown.collectAsState()
        val parsed = topicInfo?.let { currentTopicMarkdown(it, cachedMarkdown) }
        val subTopics = subTopicsViewModel.flow.collectAsLazyPagingItems()
        TopicPageContent(viewModel, subTopics, parsed, startAddReaction, lazyListState)

        var replySent by remember { mutableIntStateOf(0) }
        val firstReplyIndex by rememberUpdatedState(
            if (topicInfo == null || parsed is State.Loading || subTopics.itemCount == 0) {
                null
            } else {
                val bodyItems = if (topicInfo?.rootType == ObjectType.ROOM) 1 else currentTopicBlockCount(parsed) + 1
                val parentItems = if (topicInfo?.topicInfo?.parentType == ObjectType.TOPIC) 1 else 0
                val prependItems = if (subTopics.loadState.prepend is LoadState.NotLoading) 0 else 1
                bodyItems + parentItems + prependItems + 1 // pagingItems adds a spacer.
            },
        )
        LaunchedEffect(replySent) {
            if (replySent > 0) {
                val index =
                    snapshotFlow {
                        firstReplyIndex?.takeIf { it < lazyListState.layoutInfo.totalItemsCount }
                    }.filterNotNull().first()
                lazyListState.animateScrollToItem(index)
            }
        }
        topicInfo?.let { TopicPageInputGroup(it) { replySent++ } }
    }
}

@Composable
private fun TopicDetailToolbar(topicId: PrimaryKey, topic: TopicDetail?) {
    val reply = topic?.topicInfo?.parentType == ObjectType.TOPIC
    val title =
        when (topic?.rootType) {
            ObjectType.COMMUNITY ->
                if (reply) Res.string.topic_detail_community_reply else Res.string.topic_detail_community

            ObjectType.ROOM ->
                if (reply) Res.string.topic_detail_room_reply else Res.string.topic_detail_room

            ObjectType.USER ->
                if (reply) Res.string.topic_detail_user_reply else Res.string.topic_detail_user

            else -> Res.string.topics
        }
    CustomSearchBar(
        scope = SearchScope.TopicTopic(topicId),
        title = stringResource(title),
        subtitle = topic?.let { TopicDetailScopeName(it) },
    ) {
        topic?.takeIf { it.author != LocalUserInfo.current?.id }?.let {
            val authorInfo by createUserViewModel(it.author).handler.data.collectAsState()
            UserIconWithDialog(authorInfo)
        }
    }
}

@Composable
private fun TopicDetailScopeName(topic: TopicDetail): String? =
    when (topic.rootType) {
        ObjectType.COMMUNITY ->
            createCommunityViewModel(topic.rootId).handler.data
                .collectAsState().value?.name

        ObjectType.ROOM ->
            topic.roomInfo?.name ?: createRoomViewModel(topic.rootId).handler.data
                .collectAsState().value?.name

        ObjectType.USER ->
            createUserViewModel(topic.rootId).handler.data
                .collectAsState().value?.nickname

        else -> null
    }

@Composable
private fun ColumnScope.TopicPageContent(
    viewModel: TopicViewModel,
    subTopics: LazyPagingItems<TopicDetail>,
    parsed: State?,
    startAddReaction: () -> Unit,
    lazyListState: LazyListState,
) {
    val topicState by viewModel.handler.state.collectAsState()
    LaunchedEffect(topicState) {
        if (topicState is LoadingState.Done) {
            subTopics.refresh()
        }
    }
    StateView(viewModel.handler, modifier = Modifier.weight(1f).fillMaxWidth()) { topicInfo ->
        val roomTopic = topicInfo.rootType == ObjectType.ROOM
        LazyColumn(
            modifier = Modifier.weight(1f).widthIn(max = 740.dp).fillMaxWidth(),
            contentPadding = LayoutDefaults.contentPadding,
            verticalArrangement = if (roomTopic) LayoutDefaults.pagingVerticalArrangement else Arrangement.Top,
            state = lazyListState,
        ) {
            if (topicInfo.topicInfo.parentType == ObjectType.TOPIC) {
                item(key = "parent-topic-reference") {
                    ParentTopicReference(topicInfo.parentId)
                }
            }
            if (roomTopic) {
                item {
                    CurrentRoomTopicContent(topicInfo, parsed, startAddReaction)
                }
            } else {
                items(currentTopicBlockCount(parsed), key = { "current-topic-block-$it" }) { index ->
                    AppTopicContentView(
                        topicInfo,
                        parsed = parsed,
                        blockIndex = index.takeIf { parsed is State.Success },
                    )
                }
                item(key = CURRENT_TOPIC_ACTIONS_KEY) {
                    CurrentTopicActions(topicInfo, startAddReaction)
                }
            }

            topicDetailReplies(topicInfo, subTopics)
        }
    }
}

@Composable
private fun CurrentRoomTopicContent(topicInfo: TopicDetail, parsed: State?, startAddReaction: () -> Unit) {
    val ownMessage = topicInfo.author == LocalUserInfo.current?.id
    RoomMessageLayout(
        ownMessage = ownMessage,
        showIdentity = false,
        sentAt = topicInfo.createdTime,
        actionsTopPadding = 0.dp,
        actions = {
            InteractionRow(
                topicInfo,
                alignEnd = ownMessage,
                startAddReaction = startAddReaction,
                presentation = TopicActionsPresentation.DETAIL_MAIN,
                startAddComment = null,
            )
        },
    ) {
        AppTopicContentView(topicInfo, parsed = parsed, fillWidth = false)
    }
}

@Composable
private fun CurrentTopicActions(topicInfo: TopicDetail, startAddReaction: () -> Unit) {
    InteractionRow(
        topicInfo,
        startAddReaction = startAddReaction,
        presentation = TopicActionsPresentation.DETAIL_MAIN,
        startAddComment = null,
    )
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
}

@Composable
private fun TopicPageInputGroup(topic: TopicDetail, scrollToNew: () -> Unit) {
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
            startJoinRoom = {},
            currentTopicReadOnly = topic.readOnly,
            scrollToNew = scrollToNew,
        )
    } else {
        TopicInputGroup(scrollToNew, topic)
    }

    val appNavFactory = LocalAppNavFactory.current
    CustomAlertDialog(alertDialogState, {
        alertDialogState.close()
    }) {
        appNavFactory.newAppNav().gotoTopic(topic.rootId)
    }
}

@Composable
private fun TopicInputGroup(scrollTo: () -> Unit, topic: TopicDetail) {
    var input by rememberSaveable(topic.id) {
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
        replyingToTopic = true,
        sendButton = {
            TopicSendButton(topic, input, {
                input = it
            }, scrollTo)
        },
    )
}

@Composable
fun TopicSendButton(topic: TopicDetail, input: String, updateInput: (String) -> Unit, scrollToNew: () -> Unit) {
    if (LocalUserInfo.current?.status != UserStatus.NORMAL || topic.readOnly) return
    val focusManager = LocalFocusManager.current
    val globalTask = LocalGlobalTask.current
    val failureDialog = rememberAlertDialogController()
    CustomAlertDialog(failureDialog, failureDialog::close, failureDialog::close)
    val key = "topic ${topic.id}"
    val isSending = globalTask.stateMap[key] is LoadingState.Loading
    val currentInput by rememberUpdatedState(input)
    CommonInputButton(
        LoadingState.Done,
        input,
        isSending,
    ) {
        sendTopicInTopicPage(
            input,
            topic,
            SendTopicContext(
                globalTask,
                key,
                updateInput,
                { currentInput },
                focusManager,
                scrollToNew,
                failureDialog,
            ),
        )
    }
}
