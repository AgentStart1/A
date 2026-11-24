/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.storyteller_f.alphaheart.app.common.OnAddReaction
import com.storyteller_f.alphaheart.app.common.OnRemoveReaction
import com.storyteller_f.alphaheart.app.error.ReactionActionError
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.tasks.NestedGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.deleteReaction
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.TopicDetail

internal suspend fun NestedGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>.addReaction(
    topic: TopicDetail,
    emojiText: String,
): CustomResult<ReactionInfo, ReactionActionError> {
    val existing =
        topic.reactions.firstOrNull {
            it.emoji == emojiText
        }
    val fakeInfo =
        existing?.copy(count = existing.count + 1) ?: ReactionInfo(
            emojiText,
            topic.id,
            1,
            true,
            Long.MAX_VALUE,
        )
    emitEvent(OnAddReaction(fakeInfo, topic))
    return request {
        addReaction(topic.id, emojiText)
    }.onSuccess {
        emitEvent(OnAddReaction(it, topic))
    }.onFailure {
        emitEvent(OnRemoveReaction(fakeInfo.copy(count = fakeInfo.count - 1), topic))
    }.mapError(ReactionActionError::fromError)
}

internal suspend fun NestedGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>.deleteReaction(
    topic: TopicDetail,
    emojiText: String,
    existing: ReactionInfo,
): CustomResult<ReactionInfo, ReactionActionError> {
    val fakeInfo = existing.copy(count = existing.count - 1)
    emitEvent(OnRemoveReaction(fakeInfo, topic))
    return request { deleteReaction(emojiText, topic.id) }.onSuccess {
        emitEvent(OnRemoveReaction(it, topic))
    }.onFailure {
        emitEvent(OnRemoveReaction(existing, topic))
    }.mapError(ReactionActionError::fromError)
}
