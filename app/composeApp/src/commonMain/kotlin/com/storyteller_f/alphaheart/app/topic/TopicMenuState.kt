/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.error.TopicActionError
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.pinTopic
import com.storyteller_f.alphaheart.client.core.unpinTopic
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicDetail

internal sealed interface TopicMenuTarget {
    data object Actions : TopicMenuTarget
    data object Translation : TopicMenuTarget
    data class Community(val info: CommunityInfo) : TopicMenuTarget
    data class Room(val info: RoomInfo) : TopicMenuTarget
}

suspend fun AppGlobalDialogController.pinOrUnpinTopic(topicInfo: TopicDetail): CustomResult<Unit, TopicActionError> =
    useResult {
        if (topicInfo.isPin) {
            request { unpinTopic(topicInfo.id) }
        } else {
            request { pinTopic(topicInfo.id) }
        }
    }.onSuccess {
        context.emitEvent(
            com.storyteller_f.alphaheart.app.common.OnTopicPinChanged(it.id, it.parentId, it.rootType, it.isPin),
        )
    }.map { Unit }.mapError(TopicActionError::fromError)

internal sealed interface TranslationState {
    data object Loading : TranslationState
    data object Unavailable : TranslationState
    data object PlatformUnavailable : TranslationState
    data object Empty : TranslationState
    data class Streaming(val text: String) : TranslationState
    data class Complete(val text: String) : TranslationState
    data class Failed(val text: String) : TranslationState
}
