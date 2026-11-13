/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.MarkdownMediasViewModel
import com.storyteller_f.alphaheart.app.common.OnTopicCreated
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.getMarkdownMediasViewModel
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.input_is_empty
import com.storyteller_f.alphaheart.app.pages.AdaptiveEditorLayout
import com.storyteller_f.alphaheart.app.pages.community.CommunityTopicContent
import com.storyteller_f.alphaheart.app.pages.room.RoomSendButton
import com.storyteller_f.alphaheart.app.preview
import com.storyteller_f.alphaheart.app.raw
import com.storyteller_f.alphaheart.app.submit
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.components.block.BlockEditor
import com.storyteller_f.alphaheart.client.compose_core.components.block.BlockToolbar
import com.storyteller_f.alphaheart.client.compose_core.components.block.rememberBlockEditorState
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface TopicComposeData {
    fun getMediaTarget(): ObjectTuple?
    fun getParent(): ObjectTuple

    data class PublicRoom(val roomId: PrimaryKey, val communityId: PrimaryKey, val parentTuple: ObjectTuple) :
        TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent() = parentTuple
    }

    data class PrivateRoom(val roomId: PrimaryKey, val parentTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple = roomId ob ObjectType.ROOM

        override fun getParent() = parentTuple
    }

    data class User(val uid: PrimaryKey, val objectTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent() = objectTuple
    }

    data class Community(val communityId: PrimaryKey, val objectTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent(): ObjectTuple = objectTuple
    }
}

@Composable
fun TopicComposePage(data: TopicComposeData, backPrePage: () -> Unit) {
    val myInfo = LocalUserInfo.current ?: return
    TopicComposeScaffold(data, data.getMediaTarget() ?: myInfo.id ob ObjectType.USER, backPrePage)
}

internal fun composeCommunityId(data: TopicComposeData): PrimaryKey? =
    when (data) {
        is TopicComposeData.Community -> data.communityId
        is TopicComposeData.PublicRoom -> data.communityId
        is TopicComposeData.PrivateRoom, is TopicComposeData.User -> null
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopicComposeScaffold(data: TopicComposeData, mediaTarget: ObjectTuple, backPrePage: () -> Unit) {
    var input by rememberSaveable {
        mutableStateOf("")
    }
    var showSheet by remember {
        mutableStateOf(false)
    }
    Scaffold(topBar = {
        TopAppBar({
        }, navigationIcon = {
            IconButton(onClick = {
                showSheet = true
            }) {
                Icon(Icons.Filled.PermMedia, contentDescription = null)
            }
        }, actions = {
            TopicComposeSubmitButton(input, data) {
                input = ""
                backPrePage()
            }
        })
    }) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Column(modifier = Modifier.safeArea(paddingValues, direction)) {
            TopicComposeInternal(input, mediaTarget, data) {
                input = it
            }
        }
    }
    val sheetState = rememberModalBottomSheetState()
    FilePicker(showSheet, sheetState, mediaTarget, onClickItems = { info ->
        insertContent(info.first(), input) {
            input = it
        }
    }) {
        showSheet = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicComposeInternal(
    input: String,
    mediaTarget: ObjectTuple,
    data: TopicComposeData,
    updateInput: (String) -> Unit,
) {
    val pagerState =
        rememberPagerState {
            3
        }
    val tabs =
        listOf(
            stringResource(Res.string.preview),
            stringResource(Res.string.raw),
            "Block",
        )
    val selected = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()

    val communityId = composeCommunityId(data)
    AdaptiveEditorLayout(preview = { PreviewTopicPage(input, mediaTarget, communityId) }) { wide ->
        LaunchedEffect(wide) {
            if (wide && pagerState.currentPage == 0) pagerState.scrollToPage(1)
        }
        val visibleTabs = if (wide) tabs.drop(1) else tabs
        val visibleSelected = if (wide) selected.coerceAtLeast(1) - 1 else selected
        PrimaryTabRow(visibleSelected) {
            visibleTabs.forEachIndexed { i, label ->
                val page = if (wide) i + 1 else i
                val selectTab: () -> Unit = {
                    coroutineScope.launch { pagerState.scrollToPage(page) }
                }
                Tab(
                    selected = visibleSelected == i,
                    onClick = selectTab,
                    modifier = Modifier.appiumSemantics(text = label, onClick = selectTab),
                ) {
                    Text(text = label, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
        HorizontalPager(pagerState, key = tabs::get, userScrollEnabled = !wide) { index ->
            when {
                index == 0 && !wide -> PreviewTopicPage(input, mediaTarget, communityId)
                index == 2 -> CommunityTopicContent(communityId) { BlockEditTopicPage(input, updateInput) }
                else -> CommunityTopicContent(communityId) { EditTopicPage(input, updateInput) }
            }
        }
    }
}

@Composable
private fun TopicComposeSubmitButton(input: String, data: TopicComposeData, backPrePage: () -> Unit) {
    if (LocalUserInfo.current?.status != UserStatus.NORMAL) return
    val alertDialogController = rememberAlertDialogController()
    CustomAlertDialog(alertDialogController, alertDialogController::close) {}
    if (data is TopicComposeData.PublicRoom) {
        val roomViewModel = createRoomViewModel(data.roomId)
        val roomData by roomViewModel.handler.data.collectAsState()
        roomData?.let {
            RoomSendButton(
                input,
                it,
                data.parentTuple,
                alertDialogController,
            ) {
            }
        }
        return
    }
    if (data is TopicComposeData.PrivateRoom) {
        val roomViewModel = createRoomViewModel(data.roomId)
        val roomData by roomViewModel.handler.data.collectAsState()
        roomData?.let {
            RoomSendButton(
                input,
                it,
                data.parentTuple,
                alertDialogController,
            ) {}
        }
        return
    }
    val (objectId, objectType) = data.getParent()
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    val submit = {
        val finalInput = input.trim()
        if (finalInput.isNotEmpty()) {
            scope.launch {
                globalDialogController.useResult {
                    request {
                        createTopic(objectType, objectId, finalInput)
                    }
                }.onSuccess { info ->
                    globalDialogController.emitEvent(OnTopicCreated(info))
                    backPrePage()
                }
            }
        }
    }
    IconButton(
        onClick = submit,
        modifier =
        Modifier.appiumSemantics(
            description = stringResource(Res.string.submit),
            onClick = submit,
        ),
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Send, stringResource(Res.string.submit))
    }
}

@Composable
fun PreviewTopicPage(input: String, objectTuple: ObjectTuple, communityId: PrimaryKey? = null) {
    val markdownMediasViewModel = getMarkdownMediasViewModel(input, objectTuple)
    PreviewTopicInternal(markdownMediasViewModel, input, communityId)
}

@Composable
private fun PreviewTopicInternal(
    markdownMediasViewModel: MarkdownMediasViewModel,
    input: String,
    communityId: PrimaryKey?,
) {
    val list by markdownMediasViewModel.handler.data.collectAsState()
    val topicInfo =
        TopicDetail.EMPTY.copy(
            topicInfo =
            TopicDetail.EMPTY.topicInfo.copy(
                content = TopicContent.Plain(input, list.orEmpty().toImmutableList()),
            ),
        )
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp),
    ) {
        item {
            AppTopicContentView(topicInfo, communityId = communityId)
        }
    }
}

@Composable
fun EditTopicPage(input: String, updateInput: (String) -> Unit) {
    Box(modifier = Modifier.navigationBarsPadding()) {
        val fontFamily = LocalTopicContentFonts.current.editor
        val textStyle = LocalTextStyle.current
        BasicTextField(
            input,
            updateInput,
            modifier =
            Modifier.fillMaxSize()
                .appiumSemantics(
                    input = true,
                    inputValue = input,
                    onInputValueChange = updateInput,
                )
                .padding(20.dp),
            textStyle = textStyle.merge(color = LocalContentColor.current, fontFamily = fontFamily),
            decorationBox = { innerTextField ->
                if (input.isEmpty()) {
                    Text(
                        stringResource(Res.string.input_is_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            },
        )
    }
}

/**
 * Block 编辑模式页面.
 */
@Composable
fun BlockEditTopicPage(input: String, updateInput: (String) -> Unit) {
    // 提升状态，使 BlockToolbar 和 BlockEditor 共享 blocks 列表
    val editorState = rememberBlockEditorState(initialMarkdown = input)

    Column(modifier = Modifier.navigationBarsPadding()) {
        // Block 工具栏 - 现在可以正常工作了
        BlockToolbar(
            onInsertBlock = { block ->
                editorState.appendBlock(block)
            },
        )

        // Block 编辑器
        BlockEditor(
            modifier = Modifier.weight(1f),
            blocks = editorState.blocks,
        ) { newMarkdown ->
            Napier.i {
                "block editor markdown $newMarkdown"
            }
            updateInput(newMarkdown)
        }
    }
}
