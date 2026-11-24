/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.mikepenz.markdown.model.State
import com.storyteller_f.alphaheart.app.common.OnTopicCreated
import com.storyteller_f.alphaheart.app.common.readingMarkdownSource
import com.storyteller_f.alphaheart.app.pages.topic.RoomTopicCell
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.room.sameRoomMessageGroup
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.checkContent

internal fun LazyListScope.topicDetailReplies(topicInfo: TopicDetail, subTopics: LazyPagingItems<TopicDetail>) {
    val roomTopic = topicInfo.rootType == ObjectType.ROOM
    topPrepend(subTopics.loadState, subTopics::retry)
    pagingItems(subTopics, {
        it.id
    }) { subTopicIndex ->
        subTopics[subTopicIndex]?.let { it1 ->
            if (roomTopic) {
                val previous = if (subTopicIndex > 0) subTopics.peek(subTopicIndex - 1) else topicInfo
                RoomTopicCell(
                    it1,
                    showAvatar = !sameRoomMessageGroup(previous, it1),
                    presentation = TopicActionsPresentation.DETAIL_REPLY,
                )
            } else {
                Box(Modifier.padding(vertical = 10.dp)) {
                    TopicCell(it1, presentation = TopicActionsPresentation.DETAIL_REPLY)
                }
            }
        }
        if (!roomTopic && subTopics.itemCount - 1 != subTopicIndex) {
            HorizontalDivider()
        }
    }
    bottomAppending(subTopics.loadState, subTopics::retry)
}

internal const val CURRENT_TOPIC_ACTIONS_KEY = "current-topic-actions"

internal fun currentTopicMarkdown(topic: TopicDetail, cached: Pair<String, State>?): State? {
    val source = topic.readingMarkdownSource() ?: return null
    return cached?.takeIf { it.first == source }?.second ?: State.Loading()
}

internal fun currentTopicBlockCount(parsed: State?): Int = (parsed as? State.Success)?.node?.children?.size ?: 1

internal fun sendTopicInTopicPage(input: String, topic: TopicDetail, context: SendTopicContext) {
    checkContent(input).exceptionOrNull()?.let {
        context.failureDialog.showErrorMessage(it)
        return
    }
    context.globalTask.launch(context.key) {
        use {
            request {
                createTopic(ObjectType.TOPIC, topic.id, input).onSuccess {
                    emitEvent(OnTopicCreated(it))
                    if (context.currentInput() == input) {
                        context.updateInput("")
                        context.focusManager.clearFocus()
                    }
                    context.scrollToNew()
                }
            }
        }.onFailure {
            context.failureDialog.showErrorMessage(it.cause)
        }
    }
}

internal data class SendTopicContext(
    val globalTask: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>,
    val key: String,
    val updateInput: (String) -> Unit,
    val currentInput: () -> String,
    val focusManager: FocusManager,
    val scrollToNew: () -> Unit,
    val failureDialog: CustomAlertDialogController,
)
