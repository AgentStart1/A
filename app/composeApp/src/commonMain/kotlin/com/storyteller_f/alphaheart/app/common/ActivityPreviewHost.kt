/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.api.RoomActivityListResponse
import com.storyteller_f.alphaheart.api.TopicActivityListResponse
import com.storyteller_f.alphaheart.app.error.ActivityPreviewError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrElse
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.formatTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

private const val CONTEXT_PREVIEW_SIZE = 3

sealed interface PreviewState<out T> {
    data object Idle : PreviewState<Nothing>
    data object Loading : PreviewState<Nothing>
    data class Ready<T>(val items: ImmutableList<T>, val total: Long) : PreviewState<T>
    data object Failed : PreviewState<Nothing>
}

sealed interface MessageSummary {
    data class Text(val value: String) : MessageSummary
    data object Unavailable : MessageSummary
    data object Attachment : MessageSummary
    data object Empty : MessageSummary
}

data class MessagePreview(val summary: MessageSummary, val author: String?, val time: String?)
data class RoomActivityPreview(val room: RoomInfo, val unreadCount: Long, val message: MessagePreview?)
data class PinnedTopicPreview(val id: PrimaryKey, val message: MessagePreview)

/** Owns ephemeral previews; no decrypted private body is written to local storage. */
internal class ActivityPreviewHost(
    parentScope: CoroutineScope,
    coordination: CoroutineDispatcher,
    private val worker: CoroutineDispatcher,
    private val fetchUnread: suspend (PaginationQuery) -> CustomResult<RoomActivityListResponse, FailureDetails>,
    private val fetchPinned: suspend (PaginationQuery) -> CustomResult<TopicActivityListResponse, FailureDetails>,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutableUnread = MutableStateFlow<PreviewState<RoomActivityPreview>>(PreviewState.Idle)
    private val mutablePinned = MutableStateFlow<PreviewState<PinnedTopicPreview>>(PreviewState.Idle)
    val unread = mutableUnread.asStateFlow()
    val pinned = mutablePinned.asStateFlow()
    private var unreadJob: Job? = null
    private var pinnedJob: Job? = null

    fun refreshUnread() =
        scope.launch {
            unreadJob?.cancel()
            unreadJob =
                load(
                    mutableUnread,
                ) { fetchUnread(PaginationQuery(size = CONTEXT_PREVIEW_SIZE)).map { it.previewState() } }
        }

    fun refreshPinned() =
        scope.launch {
            pinnedJob?.cancel()
            pinnedJob =
                load(mutablePinned) {
                    fetchPinned(PaginationQuery(size = CONTEXT_PREVIEW_SIZE)).map { response ->
                        PreviewState.Ready(
                            response.data.map { PinnedTopicPreview(it.id, it.messagePreview()) }.toImmutableList(),
                            response.pagination?.total ?: response.data.size.toLong(),
                        )
                    }
                }
        }

    fun onEvent(event: Any, context: Boolean) {
        if (context && event.changesRoomActivity()) refreshUnread()
        if (context) {
            val pinsChanged =
                when (event) {
                    is OnTopicCreated -> event.topicInfo.rootType == ObjectType.COMMUNITY && event.topicInfo.isPin
                    is OnTopicPinChanged -> event.rootType == ObjectType.COMMUNITY
                    is OnCommunityJoined, is OnCommunityExited -> true
                    else -> false
                }
            if (pinsChanged) refreshPinned()
        }
    }

    fun pause() =
        scope.launch {
            unreadJob?.cancel()
            pinnedJob?.cancel()
            mutableUnread.value = PreviewState.Idle
            mutablePinned.value = PreviewState.Idle
        }

    fun close() = scope.cancel()

    private fun <T> load(
        state: MutableStateFlow<PreviewState<T>>,
        source: suspend () -> CustomResult<PreviewState.Ready<T>, FailureDetails>,
    ) = scope.launch {
        state.value = PreviewState.Loading
        val result = withContext(worker) { captureFailure(ActivityPreviewError::from) { source().getOrThrow() } }
        ensureActive()
        state.value = result.getOrElse { PreviewState.Failed }
    }
}

private fun RoomActivityListResponse.previewState() =
    PreviewState.Ready(
        data.map { it.toPreview() }.toImmutableList(),
        pagination?.total ?: data.size.toLong(),
    )

internal fun RoomActivityInfo.toPreview(): RoomActivityPreview {
    val message =
        latestMessage?.messagePreview() ?: if (latestMessageId != null || unreadMessageCount > 0) {
            MessagePreview(MessageSummary.Unavailable, null, null)
        } else {
            null
        }
    return RoomActivityPreview(room, unreadMessageCount, message)
}

@OptIn(ExperimentalTime::class)
private fun TopicActivity.messagePreview() =
    MessagePreview(
        when (val body = content) {
            is TopicContent.Plain -> summarizeMessage(body.plain, body.fileInfos.isNotEmpty())
            is TopicContent.Extracted -> summarizeMessage(body.plain, body.fileInfos.isNotEmpty())
            else -> MessageSummary.Unavailable
        },
        authorInfo?.nickname?.takeIf { it.isNotBlank() },
        createdTime.formatTime(),
    )

private fun summarizeMessage(text: String, hasFiles: Boolean): MessageSummary {
    val preview = text.take(180).replace('\n', ' ').replace('\r', ' ').trim()
    return when {
        preview.isNotEmpty() -> MessageSummary.Text(preview)
        hasFiles -> MessageSummary.Attachment
        else -> MessageSummary.Empty
    }
}

internal fun Any.changesRoomActivity(): Boolean =
    when (this) {
        is OnTopicCreated -> topicInfo.rootType == ObjectType.ROOM

        is OnTopicPinChanged -> rootType == ObjectType.ROOM

        is OnRoomRead, is OnRoomCreated, is OnRoomJoined, is OnRoomExited, is OnRoomUpdated,
        is OnCommunityJoined, is OnCommunityExited,
        -> true

        else -> false
    }
