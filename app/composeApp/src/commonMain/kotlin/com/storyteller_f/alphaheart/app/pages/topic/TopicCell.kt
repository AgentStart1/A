/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.components.InteractionRow
import com.storyteller_f.alphaheart.app.invalid_content
import com.storyteller_f.alphaheart.app.is_pinned
import com.storyteller_f.alphaheart.app.pages.user.UserCell
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.app.pin
import com.storyteller_f.alphaheart.app.topic_menu
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.app.unpin
import com.storyteller_f.alphaheart.client.compose_core.components.CustomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.PrimaryKey
import dev.tclement.fonticons.FontIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCell(info: TopicDetail?) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    TopicCellInternal(info, supportPin = false) {
        showBottomSheet = true
    }
    info?.let {
        EmojiPicker(sheetState, showBottomSheet, it) {
            showBottomSheet = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTopicCell(info: TopicDetail?) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    UserTopicCellInternal(info, supportPin = true) {
        showBottomSheet = true
    }
    info?.let {
        EmojiPicker(sheetState, showBottomSheet, it) {
            showBottomSheet = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomTopicCell(info: TopicDetail?, showAvatar: Boolean = true) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    RoomTopicCellInternal(info, showAvatar) {
        showBottomSheet = true
    }
    info?.let {
        EmojiPicker(sheetState, showBottomSheet, it) {
            showBottomSheet = false
        }
    }
}

class TopicPreviewProvider : PreviewParameterProvider<TopicDetail> {
    override val values: Sequence<TopicDetail>
        get() =
            sequenceOf(
                TopicDetail.EMPTY.copy(
                    topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = TopicContent.Plain("hello")),
                ),
            )
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun TopicCellInternal(
    @PreviewParameter(TopicPreviewProvider::class) topicInfo: TopicDetail?,
    supportPin: Boolean = false,
    startAddReaction: () -> Unit = {},
) {
    topicInfo ?: return
    val topicId = topicInfo.id
    val authorInfo = topicInfo.authorInfo
    CommonTopicCellInternal(topicInfo, supportPin) {
        UserCell(authorInfo)
        TopicContentAndInteraction(topicInfo, startAddReaction, topicId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun UserTopicCellInternal(
    @PreviewParameter(TopicPreviewProvider::class) topicInfo: TopicDetail?,
    supportPin: Boolean = false,
    startAddReaction: () -> Unit = {},
) {
    topicInfo ?: return
    val topicId = topicInfo.id
    CommonTopicCellInternal(topicInfo, supportPin) {
        TopicContentAndInteraction(topicInfo, startAddReaction, topicId)
    }
}

@Composable
private fun TopicContentAndInteraction(topicInfo: TopicDetail, startAddReaction: () -> Unit, topicId: PrimaryKey) {
    val appNavFactory = LocalAppNavFactory.current
    Column(
        Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppTopicContentView(topicInfo, true)
        InteractionRow(topicInfo, startAddReaction) {
            appNavFactory.newAppNav().gotoTopic(topicId)
        }
        SubTopics(topicInfo)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun CommonTopicCellInternal(
    @PreviewParameter(TopicPreviewProvider::class) topicInfo: TopicDetail?,
    supportPin: Boolean = false,
    block: @Composable () -> Unit = {},
) {
    topicInfo ?: return
    val topicId = topicInfo.id
    val appNavFactory = LocalAppNavFactory.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier =
            Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(onLongClick = {
                if (supportPin) {
                    expanded = true
                }
            }, onLongClickLabel = stringResource(Res.string.topic_menu)) {
                appNavFactory.newAppNav().gotoTopic(topicId)
            },
        ) {
            block()
        }

        if (topicInfo.isPin) {
            FontIcon(
                MaterialSymbolsOutlined.Keep,
                stringResource(Res.string.is_pinned),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
        TopicDropdownMenu(expanded, topicInfo) {
            expanded = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomTopicCellInternal(topicInfo: TopicDetail?, showAvatar: Boolean, startAddReaction: () -> Unit) {
    topicInfo ?: return
    val authorInfo = topicInfo.authorInfo
    val topicId = topicInfo.id
    val appNavFactory = LocalAppNavFactory.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier =
            Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(onLongClick = {
                expanded = true
            }, onLongClickLabel = stringResource(Res.string.topic_menu)) {
                appNavFactory.newAppNav().gotoTopic(topicId)
            }.padding(8.dp),
        ) {
            if (showAvatar) {
                UserCell(authorInfo)
            }
            Column(
                Modifier.fillMaxWidth().padding(start = 48.dp, end = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTopicContentView(topicInfo, true)
                if (topicInfo.reactionCount > 0) {
                    InteractionRow(topicInfo, startAddReaction) {
                        appNavFactory.newAppNav().gotoTopic(topicId)
                    }
                }
                if (topicInfo.commentCount > 0) {
                    SubTopics(topicInfo)
                }
            }
        }
        RoomTopicDropdownMenu(expanded, startAddReaction) {
            expanded = false
        }
    }
}

@Composable
private fun SubTopics(topicInfo: TopicDetail) {
    val topics = topicInfo.subTopics
    if (topics.isNotEmpty()) {
        Column(
            modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp),
        ) {
            repeat(topics.size) { index ->
                val info = topics[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val userInfo = info.authorInfo
                    UserIconWithDialog(userInfo, size = 20.dp)
                    when (val content = info.content) {
                        is TopicContent.Extracted -> {
                            Text(content.plain, maxLines = 1)
                        }

                        is TopicContent.Plain -> {
                            Text(content.plain, maxLines = 1)
                        }

                        else -> {
                            Text(stringResource(Res.string.invalid_content))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicDropdownMenu(expanded: Boolean, topicInfo: TopicDetail, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val title = if (topicInfo.isPin) stringResource(Res.string.unpin) else stringResource(Res.string.pin)
        DropdownMenuItem(
            leadingIcon = {
                val char =
                    when {
                        topicInfo.isPin -> MaterialSymbolsOutlined.KeepOff
                        else -> MaterialSymbolsOutlined.Keep
                    }
                Box(modifier = Modifier.size(20.dp)) {
                    CustomIcon(IconRes.Font(char))
                }
            },
            text = { Text(title) },
            onClick = {
                scope.launch {
                    globalDialogController.pinOrUnpinTopic(topicInfo).onSuccess {
                        onDismissRequest()
                    }
                }
            },
        )
    }
}

@Composable
fun RoomTopicDropdownMenu(expanded: Boolean, startAddReaction: () -> Unit, onDismissRequest: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Box(modifier = Modifier.size(20.dp)) {
                    CustomIcon(IconRes.Font(MaterialSymbolsOutlined.AddReaction))
                }
            },
            text = { Text(stringResource(Res.string.add_reaction)) },
            onClick = {
                startAddReaction()
            },
        )
    }
}
