/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_attachment
import com.storyteller_f.alphaheart.app.common.MarkdownMediasViewModel
import com.storyteller_f.alphaheart.app.common.OnTopicCreated
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTopicViewModel
import com.storyteller_f.alphaheart.app.common.getMarkdownMediasViewModel
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.components.block.BlockEditor
import com.storyteller_f.alphaheart.app.components.block.BlockToolbar
import com.storyteller_f.alphaheart.app.components.block.rememberBlockEditorState
import com.storyteller_f.alphaheart.app.compose_topic
import com.storyteller_f.alphaheart.app.composer_readonly
import com.storyteller_f.alphaheart.app.content_blocks
import com.storyteller_f.alphaheart.app.editor_source
import com.storyteller_f.alphaheart.app.input_is_empty
import com.storyteller_f.alphaheart.app.media.insertContent
import com.storyteller_f.alphaheart.app.pages.AdaptiveEditorLayout
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.community.CommunityTopicContent
import com.storyteller_f.alphaheart.app.pages.room.RoomSendButton
import com.storyteller_f.alphaheart.app.pages.search.SearchScopeIdentity
import com.storyteller_f.alphaheart.app.pages.search.searchScopeName
import com.storyteller_f.alphaheart.app.parent_topic_load_failed_retry
import com.storyteller_f.alphaheart.app.preview
import com.storyteller_f.alphaheart.app.submit
import com.storyteller_f.alphaheart.app.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.topic.TopicEditorAccess
import com.storyteller_f.alphaheart.app.topic.composeCommunityId
import com.storyteller_f.alphaheart.app.topic.topicComposeScope
import com.storyteller_f.alphaheart.app.topic.topicEditorAccess
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.theme.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

@Composable
fun TopicComposePage(data: TopicComposeData, backPrePage: () -> Unit) {
    val myInfo = LocalUserInfo.current ?: return
    TopicComposeScaffold(data, data.getMediaTarget() ?: myInfo.id ob ObjectType.USER, backPrePage)
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
        val scope = topicComposeScope(data)
        ReadingPageToolbar(
            stringResource(Res.string.compose_topic),
            subtitle = searchScopeName(scope),
            leading = { SearchScopeIdentity(scope, interactive = true) },
            showAccount = false,
        )
    }) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Column(modifier = Modifier.safeArea(paddingValues, direction)) {
            TopicComposeInternal(input, mediaTarget, data, { showSheet = true }, { input = it }) {
                TopicComposeAction(input, data) {
                    input = ""
                    backPrePage()
                }
            }
        }
    }
    FilePicker(showSheet, mediaTarget, onClickItems = { info ->
        showSheet = false
        insertContent(info.first(), input) {
            input = it
        }
    }) {
        showSheet = false
    }
}

@Composable
private fun TopicComposeAction(input: String, data: TopicComposeData, onSent: () -> Unit) {
    val parent = data.getParent()
    val readOnly =
        if (parent.objectType == ObjectType.TOPIC) {
            val handler = createTopicViewModel(parent.objectId).handler
            val parentTopic by handler.data.collectAsState()
            val parentState by handler.state.collectAsState()
            if (parentTopic == null && parentState is LoadingState.Error) {
                TextButton(handler::refresh) { Text(stringResource(Res.string.parent_topic_load_failed_retry)) }
                return
            }
            parentTopic?.readOnly
        } else {
            null
        }
    when (topicEditorAccess(LocalUserInfo.current?.status, parent.objectType, readOnly)) {
        TopicEditorAccess.WRITABLE -> TopicComposeSubmitButton(input, data, onSent)

        TopicEditorAccess.LOADING -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)

        TopicEditorAccess.READ_ONLY ->
            Text(
                stringResource(Res.string.composer_readonly),
                modifier = Modifier.widthIn(max = 200.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicComposeInternal(
    input: String,
    mediaTarget: ObjectTuple,
    data: TopicComposeData,
    showAttachments: () -> Unit,
    updateInput: (String) -> Unit,
    submit: @Composable () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(0) }
    val tabs =
        listOf(
            stringResource(Res.string.editor_source),
            stringResource(Res.string.content_blocks),
            stringResource(Res.string.preview),
        )
    val communityId = composeCommunityId(data)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, label ->
                    val selectTab = { selected = index }
                    Tab(
                        selected = selected == index,
                        onClick = selectTab,
                        modifier = Modifier.appiumSemantics(text = label, onClick = selectTab),
                    ) { Text(label) }
                }
            }
            if (selected == 0) {
                ReadingIconButton(showAttachments) {
                    Icon(actionIcon(ActionGlyph.PAPERCLIP), stringResource(Res.string.add_attachment))
                }
            }
            submit()
        }
        HorizontalDivider(Modifier.padding(top = 12.dp))
        Box(Modifier.weight(1f).padding(top = 20.dp)) {
            AdaptiveEditorLayout(
                preview = { PreviewTopicPage(input, mediaTarget, communityId) },
                previewOnly = selected == 2,
            ) {
                CommunityTopicContent(communityId) {
                    if (selected == 1) {
                        BlockEditTopicPage(input, updateInput)
                    } else {
                        EditTopicPage(input, updateInput)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicComposeSubmitButton(input: String, data: TopicComposeData, backPrePage: () -> Unit) {
    if (LocalUserInfo.current?.status != UserStatus.NORMAL) return
    val alertDialogController = rememberAlertDialogController()
    CustomAlertDialog(alertDialogController, alertDialogController::close) {}
    val roomId =
        when (data) {
            is TopicComposeData.PublicRoom -> data.roomId
            is TopicComposeData.PrivateRoom -> data.roomId
            else -> null
        }
    if (roomId != null) {
        TopicComposeRoomSubmitButton(input, roomId, data.getParent(), alertDialogController, backPrePage)
        return
    }
    val (objectId, objectType) = data.getParent()
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    val emptyInputMessage = stringResource(Res.string.input_is_empty)
    val submit: () -> Unit = {
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
        } else {
            alertDialogController.showTitle(emptyInputMessage)
        }
    }
    ReadingIconButton(
        onClick = submit,
        modifier =
        Modifier.appiumSemantics(
            description = stringResource(Res.string.submit),
            onClick = submit,
        ),
    ) {
        Icon(imageVector = actionIcon(ActionGlyph.SEND), stringResource(Res.string.submit))
    }
}

@Composable
private fun TopicComposeRoomSubmitButton(
    input: String,
    roomId: PrimaryKey,
    parent: ObjectTuple,
    controller: CustomAlertDialogController,
    onSent: () -> Unit,
) {
    val roomInfo by createRoomViewModel(roomId).handler.data.collectAsState()
    roomInfo?.let {
        RoomSendButton(input, it, parent, controller, onSent = onSent, scrollToNew = {}, quiet = true)
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
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
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
            updateInput(newMarkdown)
        }
    }
}
